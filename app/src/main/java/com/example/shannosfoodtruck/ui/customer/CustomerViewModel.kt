package com.example.shannosfoodtruck.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shannosfoodtruck.data.ActiveOrderStore
import com.example.shannosfoodtruck.data.AuthService
import com.example.shannosfoodtruck.data.Cart
import com.example.shannosfoodtruck.data.Category
import com.example.shannosfoodtruck.data.Feedback
import com.example.shannosfoodtruck.data.FoodRepository
import com.example.shannosfoodtruck.data.Fulfilment
import com.example.shannosfoodtruck.data.MenuItem
import com.example.shannosfoodtruck.data.Order
import com.example.shannosfoodtruck.data.OrderLine
import com.example.shannosfoodtruck.data.OrderStage
import com.example.shannosfoodtruck.data.PaymentMethod
import com.example.shannosfoodtruck.data.StarterData
import com.example.shannosfoodtruck.data.Truck
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

object CheckoutFormFields {
    const val NAME = "name"
    const val PHONE = "phone"
    const val ADDRESS = "address"
}

data class CheckoutForm(
    val name: String = "",
    val phone: String = "",
    val fulfilment: Fulfilment = Fulfilment.PICKUP,
    val address: String = "",
    val payment: PaymentMethod = PaymentMethod.CARD,
    val note: String = "",
) {
    /** Field name -> problem. Empty map means the form is good to go. */
    fun errors(): Map<String, String> = buildMap {
        if (name.isBlank()) put(CheckoutFormFields.NAME, "Add your name so we can call your order")
        if (!isValidSaPhone(phone)) put(CheckoutFormFields.PHONE, "Enter a valid cellphone number, e.g. 082 000 0000")
        if (fulfilment == Fulfilment.DELIVERY && address.isBlank()) put(CheckoutFormFields.ADDRESS, "Where should we deliver?")
    }

    companion object {
        /** 0821234567, 082 123 4567 or +27821234567 */
        fun isValidSaPhone(raw: String): Boolean {
            val digits = raw.filter { it.isDigit() || it == '+' }
            return Regex("^(0\\d{9}|\\+27\\d{9})$").matches(digits)
        }
    }
}

data class CustomerUiState(
    val menu: List<MenuItem> = emptyList(),
    val trucks: List<Truck> = emptyList(),
    val selectedTruckId: String? = null,
    val category: Category? = null, // null = All
    val cart: Cart = Cart(),
    val form: CheckoutForm = CheckoutForm(),
    val showFormErrors: Boolean = false,
    val placing: Boolean = false,
    val activeOrderId: String? = null,
    val activeOrder: Order? = null,
    val loadingMenu: Boolean = true,
    val message: String? = null,
) {
    val selectedTruck: Truck?
        get() = trucks.firstOrNull { it.id == selectedTruckId && it.open } ?: trucks.firstOrNull { it.open }

    val visibleMenu: List<MenuItem>
        get() = menu.filter { category == null || it.category == category }

    val favourites: List<MenuItem>
        get() = StarterData.favouriteIds.mapNotNull { id -> menu.firstOrNull { it.id == id } }
            .ifEmpty { menu.take(3) }

    val cartLines: List<OrderLine> get() = cart.lines(menu)
    val cartTotal: Double get() = cart.total(menu)
    val cartCount: Int get() = cart.itemCount

    /** An order that is placed but not yet collected. */
    val hasLiveOrder: Boolean get() = activeOrder != null && activeOrder.stage != OrderStage.COLLECTED
}

class CustomerViewModel(
    private val repo: FoodRepository,
    private val auth: AuthService,
    private val activeOrderStore: ActiveOrderStore,
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerUiState())
    val state: StateFlow<CustomerUiState> = _state.asStateFlow()

    private var orderJob: Job? = null

    init {
        viewModelScope.launch {
            repo.menu()
                .catch { e -> _state.update { it.copy(loadingMenu = false, message = "Couldn't load the menu. ${e.message.orEmpty()}") } }
                .collect { items ->
                    _state.update {
                        it.copy(
                            menu = items,
                            loadingMenu = false,
                            // Drop anything that just went sold out
                            cart = if (items.isEmpty()) it.cart else it.cart.keepOnly(items),
                        )
                    }
                }
        }
        viewModelScope.launch {
            repo.trucks()
                .catch { /* trucks are optional; keep going with none */ }
                .collect { trucks -> _state.update { it.copy(trucks = trucks) } }
        }
        // Pick up an order placed before the app was closed
        activeOrderStore.get()?.let { watchOrder(it) }
    }

    // ---------- browsing ----------

    fun selectTruck(truckId: String) = _state.update { it.copy(selectedTruckId = truckId) }

    fun selectCategory(category: Category?) = _state.update { it.copy(category = category) }

    // ---------- cart ----------

    fun add(itemId: String) = _state.update { s ->
        if (s.menu.firstOrNull { it.id == itemId }?.available == false) s else s.copy(cart = s.cart.add(itemId))
    }

    fun removeOne(itemId: String) = _state.update { it.copy(cart = it.cart.removeOne(itemId)) }

    // ---------- checkout ----------

    fun updateForm(transform: (CheckoutForm) -> CheckoutForm) = _state.update { it.copy(form = transform(it.form)) }

    /** Places the order. Calls [onPlaced] once it's saved, so the screen can move to tracking. */
    fun placeOrder(onPlaced: () -> Unit) {
        val s = _state.value
        if (s.placing || s.cart.isEmpty) return
        if (s.form.errors().isNotEmpty()) {
            _state.update { it.copy(showFormErrors = true) }
            return
        }
        val truck = s.selectedTruck
        if (truck == null) {
            _state.update { it.copy(message = "No truck is open right now. Try again later.") }
            return
        }
        _state.update { it.copy(placing = true, message = null) }
        viewModelScope.launch {
            try {
                val uid = auth.ensureCustomer()
                val order = Order(
                    number = "SFT-" + Random.nextInt(10000, 99999),
                    truckId = truck.id,
                    customerUid = uid,
                    customerName = s.form.name.trim(),
                    phone = s.form.phone.trim(),
                    fulfilment = s.form.fulfilment,
                    address = if (s.form.fulfilment == Fulfilment.DELIVERY) s.form.address.trim() else "",
                    payment = s.form.payment,
                    note = s.form.note.trim(),
                    lines = s.cartLines,
                )
                val id = repo.placeOrder(order)
                activeOrderStore.set(id)
                _state.update {
                    it.copy(
                        placing = false,
                        cart = Cart(),
                        form = it.form.copy(note = ""), // keep name/phone for next time
                        showFormErrors = false,
                        activeOrder = order.copy(id = id),
                    )
                }
                watchOrder(id)
                onPlaced()
            } catch (e: Exception) {
                _state.update { it.copy(placing = false, message = "Order didn't go through. ${e.message.orEmpty()}") }
            }
        }
    }

    // ---------- tracking ----------

    private fun watchOrder(orderId: String) {
        orderJob?.cancel()
        _state.update { it.copy(activeOrderId = orderId) }
        orderJob = viewModelScope.launch {
            repo.order(orderId)
                .catch { /* lost access or offline: keep the last known status */ }
                .collect { order ->
                    if (order == null) {
                        clearActiveOrder()
                    } else {
                        _state.update { it.copy(activeOrder = order) }
                    }
                }
        }
    }

    fun submitFeedback(rating: Int, comment: String, onDone: () -> Unit) {
        val orderId = _state.value.activeOrderId ?: ""
        viewModelScope.launch {
            try {
                repo.submitFeedback(Feedback(orderId, rating.coerceIn(1, 5), comment.trim()))
            } catch (_: Exception) {
                // Feedback is nice-to-have; never block the customer on it
            }
            clearActiveOrder()
            onDone()
        }
    }

    fun clearActiveOrder() {
        orderJob?.cancel()
        activeOrderStore.set(null)
        _state.update { it.copy(activeOrderId = null, activeOrder = null) }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }
}
