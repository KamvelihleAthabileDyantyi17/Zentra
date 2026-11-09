// Live backend: the same Firebase project as the Android app.
// Collections and field names match FirestoreRepository.kt exactly, so an order placed on the
// website appears in the app's staff dashboard and vice versa.
//
// References:
//  - Firebase, "Get started with Cloud Firestore" (web modular API), https://firebase.google.com/docs/firestore/quickstart
//  - Firebase, "Get realtime updates", https://firebase.google.com/docs/firestore/query-data/listen
//  - Firebase, "Authenticate Using Google with JavaScript", https://firebase.google.com/docs/auth/web/google-signin
//  - Firebase, "Link multiple auth providers", https://firebase.google.com/docs/auth/web/account-linking

import { firebaseConfig, FIREBASE_SDK_VERSION } from "./config.js";
import { log } from "./util.js";
import { STARTER_MENU, STARTER_TRUCKS, STARTER_STOCK } from "./starter-data.js";

const cdn = (name) => `https://www.gstatic.com/firebasejs/${FIREBASE_SDK_VERSION}/firebase-${name}.js`;

export async function createFirebaseBackend() {
  const [{ initializeApp }, fs, fa] = await Promise.all([import(cdn("app")), import(cdn("firestore")), import(cdn("auth"))]);
  const app = initializeApp(firebaseConfig);
  const db = fs.getFirestore(app);
  const firebaseAuth = fa.getAuth(app);
  log.d("Firebase", "Connected to project " + firebaseConfig.projectId);

  /** Runs a live query and passes plain objects to cb. Returns an unsubscribe function. */
  const live = (query, cb, map = (list) => list) =>
    fs.onSnapshot(
      query,
      (snap) => {
        log.d("Firebase", `Live update: ${snap.size} document(s)${snap.metadata.fromCache ? " (offline cache)" : ""}`);
        cb(map(snap.docs.map((d) => ({ id: d.id, ...d.data() }))));
      },
      (err) => log.w("Firebase", "Live query stopped", err),
    );

  const data = {
    onMenu: (cb) => live(fs.query(fs.collection(db, "menu"), fs.orderBy("sort")), cb),
    onTrucks: (cb) => live(fs.query(fs.collection(db, "trucks"), fs.orderBy("name")), cb),

    async placeOrder(order) {
      log.d("Firebase", `Placing order ${order.number}`);
      const ref = await fs.addDoc(fs.collection(db, "orders"), order);
      return ref.id;
    },

    onOrder: (id, cb) =>
      fs.onSnapshot(
        fs.doc(db, "orders", id),
        (snap) => cb(snap.exists() ? { id: snap.id, ...snap.data() } : null),
        (err) => log.w("Firebase", "Order listener stopped", err),
      ),

    onCustomerOrders: (uid, cb) =>
      live(fs.query(fs.collection(db, "orders"), fs.where("customerUid", "==", uid), fs.limit(30)), cb,
        (list) => list.sort((a, b) => b.createdAt - a.createdAt)),

    onOpenOrders: (truckId, cb) =>
      live(fs.query(fs.collection(db, "orders"), fs.where("truckId", "==", truckId), fs.where("stage", "in", ["RECEIVED", "COOKING", "READY"])), cb,
        (list) => list.sort((a, b) => a.createdAt - b.createdAt)),

    setStage: (id, stage) => fs.updateDoc(fs.doc(db, "orders", id), { stage }),
    setAvailable: (itemId, available) => fs.updateDoc(fs.doc(db, "menu", itemId), { available }),

    onStock: (truckId, cb) => live(fs.query(fs.collection(db, "trucks", truckId, "stock"), fs.orderBy("name")), cb),

    /** Fills an empty database. Never overwrites anything that exists. */
    async loadStarterData() {
      const batch = fs.writeBatch(db);
      const menuEmpty = (await fs.getDocs(fs.query(fs.collection(db, "menu"), fs.limit(1)))).empty;
      const trucksEmpty = (await fs.getDocs(fs.query(fs.collection(db, "trucks"), fs.limit(1)))).empty;
      if (menuEmpty) STARTER_MENU.forEach(({ id, ...item }) => batch.set(fs.doc(db, "menu", id), item));
      if (trucksEmpty) {
        STARTER_TRUCKS.forEach(({ id, ...truck }) => {
          batch.set(fs.doc(db, "trucks", id), truck);
          STARTER_STOCK.forEach(({ id: sid, ...s }) => batch.set(fs.doc(db, "trucks", id, "stock", sid), s));
        });
      }
      await batch.commit();
      log.i("Firebase", "Starter data loaded (only empty collections were filled)");
    },

    submitFeedback: (feedback) =>
      fs.addDoc(fs.collection(db, "feedback"), { ...feedback, customerUid: firebaseAuth.currentUser?.uid ?? null, createdAt: Date.now() }),
  };

  const toUser = (u) => u && {
    uid: u.uid,
    name: u.displayName || u.providerData.find((p) => p.displayName)?.displayName || null,
    email: u.email || u.providerData.find((p) => p.email)?.email || null,
    isGuest: u.isAnonymous,
  };

  const isStaff = async () => {
    const u = firebaseAuth.currentUser;
    if (!u || u.isAnonymous) return false;
    try { return (await fs.getDoc(fs.doc(db, "staff", u.uid))).exists(); }
    catch (err) { log.w("Auth", "Couldn't check staff list", err); return false; }
  };

  const requireStaff = async () => {
    if (!(await isStaff())) {
      await fa.signOut(firebaseAuth);
      throw new Error("This account isn't set up as staff yet. Ask the owner to add you.");
    }
  };

  const auth = {
    // Firebase keeps the session in the browser, so people stay signed in across visits.
    onUser: (cb) => fa.onAuthStateChanged(firebaseAuth, (u) => cb(toUser(u))),
    currentUser: () => toUser(firebaseAuth.currentUser),

    async ensureCustomer() {
      if (firebaseAuth.currentUser) return firebaseAuth.currentUser.uid;
      log.d("Auth", "Creating guest session");
      return (await fa.signInAnonymously(firebaseAuth)).user.uid;
    },

    /** Google single sign-on in a popup. A guest is linked, so they keep their orders. */
    async signInWithGoogle() {
      const provider = new fa.GoogleAuthProvider();
      const current = firebaseAuth.currentUser;
      let result;
      if (current?.isAnonymous) {
        try {
          result = await fa.linkWithPopup(current, provider);
        } catch (err) {
          if (err.code !== "auth/credential-already-in-use") throw err;
          // This Google account already exists: sign in to it with the credential from the error
          log.w("Auth", "Google account already exists; signing in to it");
          result = await fa.signInWithCredential(firebaseAuth, fa.GoogleAuthProvider.credentialFromError(err));
        }
      } else {
        result = await fa.signInWithPopup(firebaseAuth, provider);
      }
      log.i("Auth", "Signed in with Google");
      return toUser(result.user);
    },

    async signInStaffEmail(email, password) {
      await fa.signInWithEmailAndPassword(firebaseAuth, email.trim(), password);
      await requireStaff();
      log.i("Auth", "Staff signed in with email");
    },

    async signInStaffGoogle() {
      await fa.signInWithPopup(firebaseAuth, new fa.GoogleAuthProvider());
      await requireStaff();
      log.i("Auth", "Staff signed in with Google");
    },

    isStaff,

    async signOut() {
      log.i("Auth", "Signing out");
      await fa.signOut(firebaseAuth);
    },

    /** Permanently deletes the signed-in account (required by Google Play for apps with sign-in). */
    async deleteAccount() {
      const u = firebaseAuth.currentUser;
      if (!u) return;
      try {
        await fa.deleteUser(u);
        log.i("Auth", "Account deleted");
      } catch (err) {
        if (err.code === "auth/requires-recent-login") {
          throw new Error("For your security, sign out, sign in again, then delete your account.");
        }
        throw err;
      }
    },
  };

  return { data, auth };
}
