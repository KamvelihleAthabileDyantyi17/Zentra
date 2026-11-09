package com.example.shannosfoodtruck.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * In-memory data. Behaves like the live database (including live order updates)
 * but nothing leaves the phone. Used by unit tests and when USE_FIREBASE = false.
 */
class DemoRepository(
    menu: List<MenuItem> = StarterData.menu,
    trucks: List<Truck> = StarterData.trucks,
) : FoodRepository {

    private val menuFlow = MutableStateFlow(menu)
    private val trucksFlow = MutableStateFlow(trucks)
    private val ordersFlow = MutableStateFlow<Map<String, Order>>(emptyMap())
    private val stockFlow = MutableStateFlow(StarterData.stock)
    val feedback = mutableListOf<Feedback>()

    override fun menu(): Flow<List<MenuItem>> = menuFlow

    override fun trucks(): Flow<List<Truck>> = trucksFlow

    override suspend fun placeOrder(order: Order): String {
        val id = UUID.randomUUID().toString()
        ordersFlow.update { it + (id to order.copy(id = id)) }
        return id
    }

    override fun order(orderId: String): Flow<Order?> = ordersFlow.map { it[orderId] }

    override fun openOrders(truckId: String): Flow<List<Order>> = ordersFlow.map { all ->
        all.values
            .filter { it.truckId == truckId && it.stage != OrderStage.COLLECTED }
            .sortedBy { it.createdAt }
    }

    override suspend fun setStage(orderId: String, stage: OrderStage) {
        ordersFlow.update { all ->
            val order = all[orderId] ?: return@update all
            all + (orderId to order.copy(stage = stage))
        }
    }

    override suspend fun setAvailable(itemId: String, available: Boolean) {
        menuFlow.update { items -> items.map { if (it.id == itemId) it.copy(available = available) else it } }
    }

    override fun stock(truckId: String): Flow<List<StockItem>> = stockFlow

    override suspend fun loadStarterData() {
        if (menuFlow.value.isEmpty()) menuFlow.value = StarterData.menu
        if (trucksFlow.value.isEmpty()) trucksFlow.value = StarterData.trucks
    }

    override suspend fun submitFeedback(feedback: Feedback) {
        this.feedback += feedback
    }
}

/** Demo sign-in: any email with a 6+ character password counts as staff. */
class DemoAuthService : AuthService {
    private var staff = false

    override suspend fun ensureCustomer(): String = "demo-customer"

    override suspend fun signInStaff(email: String, password: String) {
        require(email.contains("@") && password.length >= 6) { "Enter a valid email and a password of 6+ characters" }
        staff = true
    }

    override suspend fun isStaff(): Boolean = staff

    override fun signOutStaff() {
        staff = false
    }
}
