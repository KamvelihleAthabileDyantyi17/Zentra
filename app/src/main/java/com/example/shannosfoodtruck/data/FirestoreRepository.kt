package com.example.shannosfoodtruck.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Live data in Cloud Firestore.
 *
 * Collections:
 *   menu/{itemId}                      name, description, price, category, tag, available, sort
 *   trucks/{truckId}                   name, spot, address, open, comingSoon
 *   trucks/{truckId}/stock/{stockId}   name, onHand, reorderAt, unit   (stockId = ingredient id)
 *   orders/{orderId}                   number, truckId, customerUid, customerName, phone, fulfilment,
 *                                      address, deliveryArea, deliveryFee, pickupTime, payment, note,
 *                                      lines[], total, stage, createdAt
 *   feedback/{id}                      orderId, rating, comment, createdAt
 *   staff/{uid}                        exists = this user may use the staff dashboard
 *
 * Security rules that match this layout are in /firestore.rules at the repo root.
 *
 * References:
 *  - Firebase, "Get realtime updates with Cloud Firestore",
 *    https://firebase.google.com/docs/firestore/query-data/listen
 *  - Kotlin, "callbackFlow", https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/callback-flow.html
 */
class FirestoreRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : FoodRepository {

    private val menuCol get() = db.collection("menu")
    private val trucksCol get() = db.collection("trucks")
    private val ordersCol get() = db.collection("orders")

    override fun menu(): Flow<List<MenuItem>> =
        listen(menuCol.orderBy("sort")) { it.toMenuItem() }

    override fun trucks(): Flow<List<Truck>> =
        listen(trucksCol.orderBy("name")) { it.toTruck() }

    override suspend fun placeOrder(order: Order): String {
        val ref = ordersCol.document()
        AppLog.d(TAG, "Placing order ${order.number} (${order.itemCount} items) for ${order.truckId}")
        ref.set(order.toMap()).await()
        AppLog.d(TAG, "Order ${order.number} saved as ${ref.id}")
        return ref.id
    }

    override fun order(orderId: String): Flow<Order?> = callbackFlow {
        val reg = ordersCol.document(orderId).addSnapshotListener { snap, error ->
            if (error != null) {
                AppLog.w(TAG, "Order listener for $orderId stopped", error)
                close(error); return@addSnapshotListener
            }
            trySend(snap?.takeIf { it.exists() }?.toOrder())
        }
        awaitClose { reg.remove() }
    }

    override fun ordersForCustomer(customerUid: String): Flow<List<Order>> =
        listen<Order>(
            ordersCol.whereEqualTo("customerUid", customerUid).limit(30),
            sort = { list -> list.sortedByDescending { it.createdAt } },
        ) { it.toOrder() }

    /**
     * Orders for a truck since a time, for the sales and purchase report. Filters on createdAt only
     * (a single-field index Firestore creates automatically) and picks the truck on the phone,
     * so no composite index has to be set up in the console.
     */
    override fun ordersSince(truckId: String, sinceMillis: Long): Flow<List<Order>> =
        listen<Order>(
            ordersCol.whereGreaterThanOrEqualTo("createdAt", sinceMillis),
            sort = { list -> list.filter { it.truckId == truckId } },
        ) { it.toOrder() }

    override fun openOrders(truckId: String): Flow<List<Order>> =
        listen<Order>(
            ordersCol
                .whereEqualTo("truckId", truckId)
                .whereIn("stage", listOf(OrderStage.RECEIVED.name, OrderStage.COOKING.name, OrderStage.READY.name)),
            sort = { list -> list.sortedBy { it.createdAt } },
        ) { it.toOrder() }

    override suspend fun setStage(orderId: String, stage: OrderStage) {
        AppLog.d(TAG, "Order $orderId -> ${stage.name}")
        ordersCol.document(orderId).update("stage", stage.name).await()
    }

    override suspend fun setAvailable(itemId: String, available: Boolean) {
        AppLog.d(TAG, "Menu item $itemId available=$available")
        menuCol.document(itemId).update("available", available).await()
    }

    override fun stock(truckId: String): Flow<List<StockItem>> =
        listen(trucksCol.document(truckId).collection("stock").orderBy("name")) { it.toStockItem() }

    /** Fills an empty database with the starter menu, trucks and stock list. Never overwrites existing data. */
    override suspend fun loadStarterData() {
        val batch = db.batch()
        if (menuCol.limit(1).get().await().isEmpty) {
            StarterData.menu.forEachIndexed { i, item -> batch.set(menuCol.document(item.id), item.toMap(sort = i)) }
        }
        if (trucksCol.limit(1).get().await().isEmpty) {
            StarterData.trucks.forEach { truck ->
                batch.set(
                    trucksCol.document(truck.id),
                    mapOf("name" to truck.name, "spot" to truck.spot, "address" to truck.address, "open" to truck.open, "comingSoon" to truck.comingSoon),
                )
                StarterData.stock.forEach { s ->
                    batch.set(
                        trucksCol.document(truck.id).collection("stock").document(s.id),
                        mapOf("name" to s.name, "onHand" to s.onHand, "reorderAt" to s.reorderAt, "unit" to s.unit),
                    )
                }
            }
        }
        batch.commit().await()
        AppLog.i(TAG, "Starter data loaded (only empty collections were filled)")
    }

    override suspend fun submitFeedback(feedback: Feedback) {
        db.collection("feedback").add(
            mapOf(
                "orderId" to feedback.orderId,
                "rating" to feedback.rating,
                "comment" to feedback.comment,
                "customerUid" to FirebaseAuth.getInstance().currentUser?.uid,
                "createdAt" to System.currentTimeMillis(),
            )
        ).await()
    }

    private companion object { const val TAG = "FirestoreRepository" }

    // ---------- helpers ----------

    private fun <T> listen(
        query: Query,
        sort: (List<T>) -> List<T> = { it },
        map: (DocumentSnapshot) -> T?,
    ): Flow<List<T>> = callbackFlow {
        val reg = query.addSnapshotListener { snap, error ->
            if (error != null) {
                AppLog.w(TAG, "Live query stopped", error)
                close(error); return@addSnapshotListener
            }
            val docs = snap?.documents.orEmpty()
            AppLog.d(TAG, "Live update: ${docs.size} document(s)" + if (snap?.metadata?.isFromCache == true) " (offline cache)" else "")
            trySend(sort(docs.mapNotNull(map)))
        }
        awaitClose { reg.remove() }
    }
}

// ---------- Firestore <-> model mapping ----------

private fun MenuItem.toMap(sort: Int) = mapOf(
    "name" to name, "description" to description, "price" to price,
    "category" to category.name, "tag" to tag, "available" to available, "sort" to sort,
)

private fun DocumentSnapshot.toMenuItem(): MenuItem? {
    val name = getString("name") ?: return null
    return MenuItem(
        id = id,
        name = name,
        description = getString("description").orEmpty(),
        price = getDouble("price") ?: return null,
        category = Category.fromName(getString("category")),
        tag = getString("tag") ?: name.substringBefore(' ').take(5).uppercase(),
        available = getBoolean("available") ?: true,
    )
}

private fun DocumentSnapshot.toTruck(): Truck? {
    val name = getString("name") ?: return null
    return Truck(
        id = id, name = name, spot = getString("spot").orEmpty(), open = getBoolean("open") ?: false,
        address = getString("address").orEmpty(), comingSoon = getBoolean("comingSoon") ?: false,
    )
}

private fun DocumentSnapshot.toStockItem(): StockItem? {
    val name = getString("name") ?: return null
    return StockItem(
        id = id,
        name = name,
        onHand = getDouble("onHand") ?: 0.0,
        reorderAt = getDouble("reorderAt") ?: 0.0,
        unit = getString("unit").orEmpty(),
    )
}

private fun Order.toMap() = mapOf(
    "number" to number,
    "truckId" to truckId,
    "customerUid" to customerUid,
    "customerName" to customerName,
    "phone" to phone,
    "fulfilment" to fulfilment.name,
    "address" to address,
    "deliveryArea" to deliveryArea?.name,
    "deliveryFee" to deliveryFee,
    "pickupTime" to pickupTime,
    "payment" to payment.name,
    "note" to note,
    "lines" to lines.map { mapOf("itemId" to it.itemId, "name" to it.name, "quantity" to it.quantity, "unitPrice" to it.unitPrice) },
    "total" to total,
    "stage" to stage.name,
    "createdAt" to createdAt,
)

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.toOrder(): Order? {
    val rawLines = get("lines") as? List<Map<String, Any?>> ?: return null
    return Order(
        id = id,
        number = getString("number") ?: id.take(6),
        truckId = getString("truckId").orEmpty(),
        customerUid = getString("customerUid").orEmpty(),
        customerName = getString("customerName").orEmpty(),
        phone = getString("phone").orEmpty(),
        fulfilment = runCatching { Fulfilment.valueOf(getString("fulfilment")!!) }.getOrDefault(Fulfilment.PICKUP),
        address = getString("address").orEmpty(),
        deliveryArea = getString("deliveryArea")?.let { name -> DeliveryArea.entries.firstOrNull { it.name == name } },
        pickupTime = getString("pickupTime") ?: PickupTimes.ASAP,
        payment = runCatching { PaymentMethod.valueOf(getString("payment")!!) }.getOrDefault(PaymentMethod.CASH),
        note = getString("note").orEmpty(),
        lines = rawLines.map {
            OrderLine(
                itemId = it["itemId"] as? String ?: "",
                name = it["name"] as? String ?: "",
                quantity = (it["quantity"] as? Number)?.toInt() ?: 0,
                unitPrice = (it["unitPrice"] as? Number)?.toDouble() ?: 0.0,
            )
        },
        stage = OrderStage.fromName(getString("stage")),
        createdAt = getLong("createdAt") ?: 0L,
    )
}

/**
 * Firebase Authentication.
 *
 * Google single sign-on: the screen gets a Google ID token from Android Credential Manager
 * (see ui/auth/GoogleSignIn.kt) and passes it here. If the person was ordering as a guest,
 * we *link* Google to the guest account so their uid (and their orders) stay the same.
 *
 * References:
 *  - Firebase, "Authenticate with Google on Android",
 *    https://firebase.google.com/docs/auth/android/google-signin
 *  - Firebase, "Link multiple auth providers to an account",
 *    https://firebase.google.com/docs/auth/android/account-linking
 */
class FirebaseAuthService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : AuthService {

    private val user = MutableStateFlow(auth.currentUser?.toAppUser())
    override val currentUser: StateFlow<AppUser?> = user.asStateFlow()

    init {
        // Firebase remembers the session on the phone; this keeps our StateFlow in step with it.
        auth.addAuthStateListener { a ->
            val u = a.currentUser?.toAppUser()
            AppLog.d(TAG, if (u == null) "Signed out" else "Session: ${if (u.isGuest) "guest" else "signed in"} uid=${u.uid.take(6)}…")
            user.value = u
        }
    }

    override suspend fun ensureCustomer(): String {
        auth.currentUser?.let { return it.uid }
        AppLog.d(TAG, "Creating guest session")
        val result = auth.signInAnonymously().await()
        return result.user?.uid ?: error("Could not connect. Check your internet and try again.")
    }

    override suspend fun signInWithGoogle(idToken: String): AppUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val current = auth.currentUser
        val linkedOrSignedIn: FirebaseUser? = try {
            if (current != null && current.isAnonymous) {
                AppLog.d(TAG, "Linking guest account to Google")
                current.linkWithCredential(credential).await().user
            } else {
                auth.signInWithCredential(credential).await().user
            }
        } catch (e: FirebaseAuthUserCollisionException) {
            // This Google account already has its own Firebase account: just sign in to that one.
            AppLog.w(TAG, "Google account already exists; signing in to it instead of linking")
            auth.signInWithCredential(credential).await().user
        }
        val signedIn: FirebaseUser = linkedOrSignedIn ?: error("Google sign-in failed")
        // Linking doesn't always fire the auth listener, so refresh our copy of the user.
        signedIn.reload().await()
        val appUser = (auth.currentUser ?: signedIn).toAppUser()
        user.value = appUser
        AppLog.i(TAG, "Signed in with Google")
        return appUser
    }

    override suspend fun signInStaff(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        requireStaff()
        AppLog.i(TAG, "Staff signed in with email")
    }

    override suspend fun signInStaffWithGoogle(idToken: String) {
        signInWithGoogle(idToken)
        requireStaff()
        AppLog.i(TAG, "Staff signed in with Google")
    }

    private suspend fun requireStaff() {
        if (!isStaff()) {
            AppLog.w(TAG, "Signed-in account is not on the staff list")
            auth.signOut()
            error("This account isn't set up as staff yet. Ask the owner to add you.")
        }
    }

    override suspend fun isStaff(): Boolean {
        val u = auth.currentUser ?: return false
        if (u.isAnonymous) return false
        return runCatching { db.collection("staff").document(u.uid).get().await().exists() }
            .onFailure { AppLog.w(TAG, "Couldn't check staff list", it) }
            .getOrDefault(false)
    }

    override fun signOut() {
        AppLog.i(TAG, "Signing out")
        auth.signOut()
    }

    override suspend fun deleteAccount() {
        val u = auth.currentUser ?: return
        try {
            u.delete().await()
            AppLog.i(TAG, "Account deleted")
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            // Firebase only allows deleting right after signing in, to protect people
            AppLog.w(TAG, "Delete needs a fresh sign-in", e)
            error("For your security, sign out, sign in again, then delete your account.")
        }
    }

    private fun FirebaseUser.toAppUser() = AppUser(
        uid = uid,
        name = displayName?.takeIf { it.isNotBlank() } ?: providerData.firstNotNullOfOrNull { it.displayName?.takeIf(String::isNotBlank) },
        email = email?.takeIf { it.isNotBlank() } ?: providerData.firstNotNullOfOrNull { it.email?.takeIf(String::isNotBlank) },
        isGuest = isAnonymous,
    )

    private companion object { const val TAG = "Auth" }
}
