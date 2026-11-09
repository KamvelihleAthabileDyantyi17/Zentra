package com.example.shannosfoodtruck.data

import kotlinx.coroutines.flow.Flow

/**
 * Everything the app reads or writes goes through here. Screens never talk to
 * Firebase directly, so there is exactly one place to change how data is stored.
 *
 * Two implementations:
 *  - [FirestoreRepository] — live data, real-time updates across phones.
 *  - [DemoRepository]      — in-memory, for tests and offline demos.
 */
interface FoodRepository {
    /** The full menu, updating live when staff change prices or mark items sold out. */
    fun menu(): Flow<List<MenuItem>>

    fun trucks(): Flow<List<Truck>>

    /** Saves a new order and returns its id. */
    suspend fun placeOrder(order: Order): String

    /** One order, updating live as staff move it through the stages. Emits null if it doesn't exist. */
    fun order(orderId: String): Flow<Order?>

    // ---- staff only ----
    fun openOrders(truckId: String): Flow<List<Order>>
    suspend fun setStage(orderId: String, stage: OrderStage)
    suspend fun setAvailable(itemId: String, available: Boolean)
    fun stock(truckId: String): Flow<List<StockItem>>
    suspend fun loadStarterData()

    // ---- feedback ----
    suspend fun submitFeedback(feedback: Feedback)
}

/** Who is using the app. Customers are signed in invisibly; staff sign in with email. */
interface AuthService {
    /** Makes sure the customer has an (anonymous) identity before ordering. Returns their uid. */
    suspend fun ensureCustomer(): String

    /** Signs in a staff member. Throws with a readable message on failure or if the account isn't staff. */
    suspend fun signInStaff(email: String, password: String)

    suspend fun isStaff(): Boolean

    fun signOutStaff()
}
