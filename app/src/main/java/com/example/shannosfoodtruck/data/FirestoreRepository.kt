package com.example.shannosfoodtruck.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Live data in Cloud Firestore.
 *
 * Collections:
 *   menu/{itemId}                      name, description, price, category, tag, available, sort
 *   trucks/{truckId}                   name, spot, open
 *   trucks/{truckId}/stock/{stockId}   name, onHand, reorderAt
 *   orders/{orderId}                   number, truckId, customerUid, customerName, phone, fulfilment,
 *                                      address, payment, note, lines[], stage, createdAt
 *   feedback/{id}                      orderId, rating, comment, createdAt
 *   staff/{uid}                        exists = this user may use the staff dashboard
 *
 * Security rules that match this layout are in /firestore.rules at the repo root.
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
        ref.set(order.toMap()).await()
        return ref.id
    }

    override fun order(orderId: String): Flow<Order?> = callbackFlow {
        val reg = ordersCol.document(orderId).addSnapshotListener { snap, error ->
            if (error != null) {
                close(error); return@addSnapshotListener
            }
            trySend(snap?.takeIf { it.exists() }?.toOrder())
        }
        awaitClose { reg.remove() }
    }

    override fun openOrders(truckId: String): Flow<List<Order>> =
        listen<Order>(
            ordersCol
                .whereEqualTo("truckId", truckId)
                .whereIn("stage", listOf(OrderStage.RECEIVED.name, OrderStage.COOKING.name, OrderStage.READY.name)),
            sort = { list -> list.sortedBy { it.createdAt } },
        ) { it.toOrder() }

    override suspend fun setStage(orderId: String, stage: OrderStage) {
        ordersCol.document(orderId).update("stage", stage.name).await()
    }

    override suspend fun setAvailable(itemId: String, available: Boolean) {
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
                batch.set(trucksCol.document(truck.id), mapOf("name" to truck.name, "spot" to truck.spot, "open" to truck.open))
                StarterData.stock.forEach { s ->
                    batch.set(
                        trucksCol.document(truck.id).collection("stock").document(s.id),
                        mapOf("name" to s.name, "onHand" to s.onHand, "reorderAt" to s.reorderAt),
                    )
                }
            }
        }
        batch.commit().await()
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

    // ---------- helpers ----------

    private fun <T> listen(
        query: Query,
        sort: (List<T>) -> List<T> = { it },
        map: (DocumentSnapshot) -> T?,
    ): Flow<List<T>> = callbackFlow {
        val reg = query.addSnapshotListener { snap, error ->
            if (error != null) {
                close(error); return@addSnapshotListener
            }
            trySend(sort(snap?.documents.orEmpty().mapNotNull(map)))
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
    return Truck(id = id, name = name, spot = getString("spot").orEmpty(), open = getBoolean("open") ?: false)
}

private fun DocumentSnapshot.toStockItem(): StockItem? {
    val name = getString("name") ?: return null
    return StockItem(
        id = id,
        name = name,
        onHand = getLong("onHand")?.toInt() ?: 0,
        reorderAt = getLong("reorderAt")?.toInt() ?: 0,
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

class FirebaseAuthService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : AuthService {

    override suspend fun ensureCustomer(): String {
        auth.currentUser?.let { return it.uid }
        val result = auth.signInAnonymously().await()
        return result.user?.uid ?: error("Could not connect. Check your internet and try again.")
    }

    override suspend fun signInStaff(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        if (!isStaff()) {
            auth.signOut()
            error("This account isn't set up as staff yet.")
        }
    }

    override suspend fun isStaff(): Boolean {
        val user = auth.currentUser ?: return false
        if (user.isAnonymous) return false
        return runCatching { db.collection("staff").document(user.uid).get().await().exists() }.getOrDefault(false)
    }

    override fun signOutStaff() = auth.signOut()
}
