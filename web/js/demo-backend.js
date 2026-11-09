// Demo backend: behaves like Firebase but keeps everything in this browser's localStorage.
// Changes made in one tab (e.g. staff moving an order) show up live in other tabs too,
// thanks to the browser's "storage" event, so you can demo the whole flow on one computer.

import { log } from "./util.js";
import { STARTER_MENU, STARTER_TRUCKS, STARTER_STOCK } from "./starter-data.js";

const DB_KEY = "shannos-demo-db";
const DB_VERSION = 2; // bump when the demo data's shape changes, so old browsers start fresh
// The shop and the staff page keep separate demo sessions, as if they were different phones,
// so you can be a customer in one tab and staff in another.
const USER_KEY = location.pathname.includes("staff") ? "shannos-demo-staff-user" : "shannos-demo-user";
const STAFF_KEY = "shannos-demo-staff";

function readDb() {
  try {
    const db = JSON.parse(localStorage.getItem(DB_KEY));
    if (db && db.menu && db.version === DB_VERSION) return db;
  } catch (_) { /* fall through to a fresh database */ }
  return { version: DB_VERSION, menu: STARTER_MENU, trucks: STARTER_TRUCKS, stock: STARTER_STOCK, orders: {}, feedback: [] };
}

function writeDb(db) {
  localStorage.setItem(DB_KEY, JSON.stringify(db));
  notify();
}

// ---- tiny live-query system ----
const listeners = new Set();
function watch(select, cb) {
  const run = () => cb(select(readDb()));
  listeners.add(run);
  queueMicrotask(run); // first result straight away, like Firestore
  return () => listeners.delete(run);
}
function notify() { listeners.forEach((run) => run()); }
window.addEventListener("storage", (e) => {
  if (e.key === DB_KEY) notify();
  if (e.key === USER_KEY) emitUser();
});

const newId = () => Math.random().toString(36).slice(2, 12);

export const data = {
  onMenu: (cb) => watch((db) => [...db.menu].sort((a, b) => a.sort - b.sort), cb),
  onTrucks: (cb) => watch((db) => db.trucks, cb),

  async placeOrder(order) {
    const db = readDb();
    const id = newId();
    db.orders[id] = { ...order, id };
    writeDb(db);
    log.d("DemoData", `Order ${order.number} placed`);
    return id;
  },

  onOrder: (id, cb) => watch((db) => db.orders[id] || null, cb),

  onCustomerOrders: (uid, cb) =>
    watch((db) => Object.values(db.orders).filter((o) => o.customerUid === uid).sort((a, b) => b.createdAt - a.createdAt), cb),

  onOrdersSince: (truckId, since, cb) =>
    watch((db) => Object.values(db.orders).filter((o) => o.truckId === truckId && o.createdAt >= since), cb),

  onOpenOrders: (truckId, cb) =>
    watch((db) => Object.values(db.orders)
      .filter((o) => o.truckId === truckId && o.stage !== "COLLECTED")
      .sort((a, b) => a.createdAt - b.createdAt), cb),

  async setStage(id, stage) {
    const db = readDb();
    if (db.orders[id]) db.orders[id].stage = stage;
    writeDb(db);
  },

  async setAvailable(itemId, available) {
    const db = readDb();
    db.menu = db.menu.map((m) => (m.id === itemId ? { ...m, available } : m));
    writeDb(db);
  },

  onStock: (_truckId, cb) => watch((db) => db.stock, cb),

  async loadStarterData() {
    const db = readDb();
    if (!db.menu.length) db.menu = STARTER_MENU;
    if (!db.trucks.length) db.trucks = STARTER_TRUCKS;
    writeDb(db);
  },

  async submitFeedback(feedback) {
    const db = readDb();
    db.feedback.push({ ...feedback, createdAt: Date.now() });
    writeDb(db);
  },

  /** Demo only: wipe everything back to the starter data. */
  reset() {
    localStorage.removeItem(DB_KEY);
    notify();
  },
};

// ---- demo auth ----
const userListeners = new Set();
const currentUser = () => JSON.parse(localStorage.getItem(USER_KEY) || "null");
function setUser(u) {
  if (u) localStorage.setItem(USER_KEY, JSON.stringify(u)); else localStorage.removeItem(USER_KEY);
  emitUser();
}
function emitUser() { const u = currentUser(); userListeners.forEach((cb) => cb(u)); }

export const auth = {
  onUser(cb) {
    userListeners.add(cb);
    queueMicrotask(() => cb(currentUser()));
    return () => userListeners.delete(cb);
  },
  currentUser,

  async ensureCustomer() {
    const u = currentUser();
    if (u) return u.uid;
    const guest = { uid: "guest-" + newId(), name: null, email: null, isGuest: true };
    setUser(guest);
    return guest.uid;
  },

  /** Demo "Continue with Google": signs in a demo person, keeping a guest's uid (like account linking). */
  async signInWithGoogle() {
    const uid = currentUser()?.uid || "demo-google-user";
    const u = { uid, name: "Thando Demo", email: "thando@example.com", isGuest: false };
    setUser(u);
    log.i("DemoAuth", "Signed in with Google (demo)");
    return u;
  },

  async signInStaffEmail(email, password) {
    if (!email.includes("@") || password.length < 6) throw new Error("Enter a valid email and a password of 6+ characters");
    localStorage.setItem(STAFF_KEY, "1");
    setUser({ uid: "demo-staff", name: "Staff", email, isGuest: false });
  },

  async signInStaffGoogle() {
    localStorage.setItem(STAFF_KEY, "1");
    await this.signInWithGoogle();
  },

  async isStaff() { return localStorage.getItem(STAFF_KEY) === "1" && !!currentUser(); },

  async signOut() {
    localStorage.removeItem(STAFF_KEY);
    setUser(null);
    log.i("DemoAuth", "Signed out");
  },

  async deleteAccount() {
    await this.signOut();
    log.i("DemoAuth", "Account deleted (demo)");
  },
};
