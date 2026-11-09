package com.example.shannosfoodtruck.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

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

    /** A signed-in customer's own orders, newest first. */
    fun ordersForCustomer(customerUid: String): Flow<List<Order>>

    // ---- staff only ----
    fun openOrders(truckId: String): Flow<List<Order>>

    /** Every order for a truck since a time (any stage), for sales and purchase reports. */
    fun ordersSince(truckId: String, sinceMillis: Long): Flow<List<Order>>
    suspend fun setStage(orderId: String, stage: OrderStage)
    suspend fun setAvailable(itemId: String, available: Boolean)
    fun stock(truckId: String): Flow<List<StockItem>>
    suspend fun loadStarterData()

    // ---- feedback ----
    suspend fun submitFeedback(feedback: Feedback)
}

/** The person signed in, as the app sees them. */
data class AppUser(
    val uid: String,
    val name: String?,
    val email: String?,
    /** true = the invisible guest identity every customer gets; false = signed in with Google or email. */
    val isGuest: Boolean,
)

/**
 * Who is using the app.
 *  - Customers start as an invisible guest so they can order without an account.
 *  - Anyone can "Continue with Google" (single sign-on). A guest who does this keeps their orders.
 *  - Staff sign in with Google or email; they must also be listed in the Firestore `staff` collection.
 *
 * The session is remembered between app launches, so people stay signed in until they sign out.
 */
interface AuthService {
    /** The current user, updating live on sign-in and sign-out. null = nobody yet. */
    val currentUser: StateFlow<AppUser?>

    /** Makes sure the customer has an identity (a guest one if needed) before ordering. Returns their uid. */
    suspend fun ensureCustomer(): String

    /** Single sign-on: signs in with a Google ID token from Credential Manager. */
    suspend fun signInWithGoogle(idToken: String): AppUser

    /** Signs in a staff member by email. Throws with a readable message on failure or if the account isn't staff. */
    suspend fun signInStaff(email: String, password: String)

    /** Staff single sign-on with Google. Throws if the Google account isn't on the staff list. */
    suspend fun signInStaffWithGoogle(idToken: String)

    suspend fun isStaff(): Boolean

    fun signOut()

    /**
     * Permanently deletes the signed-in account (Google Play requires apps with sign-in to offer this).
     * Past orders stay in the truck's records but are no longer linked to a login.
     */
    suspend fun deleteAccount()
}
