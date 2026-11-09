package com.example.shannosfoodtruck.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shannosfoodtruck.data.ActiveOrderStore
import com.example.shannosfoodtruck.data.AppLog
import com.example.shannosfoodtruck.data.AppSettings
import com.example.shannosfoodtruck.data.DeliveryArea
import com.example.shannosfoodtruck.data.InMemoryAppSettings
import com.example.shannosfoodtruck.data.PickupTimes
import com.example.shannosfoodtruck.data.AppUser
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    val deliveryArea: DeliveryArea = DeliveryArea.PARKLANDS,
    /** "ASAP" or a slot like "14:30" (see PickupTimes). */
    val pickupTime: String = PickupTimes.ASAP,
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
    /** The person signed in with Google, or null for a guest. */
    val user: AppUser? = null,
    val signingIn: Boolean = false,
    /** Signed-in customer's past and current orders. */
    val myOrders: List<Order> = emptyList(),
    /** Settings → "Remember my details". */
    val rememberDetails: Boolean = true,
) {
    val isSignedIn: Boolean get() = user != null && !user.isGuest

    /** The chosen truck if it's trading, otherwise the first truck that is. */
    val selectedTruck: Truck?
        get() = trucks.firstOrNull { it.id == selectedTruckId && it.open && !it.comingSoon }
            ?: trucks.firstOrNull { it.open && !it.comingSoon }

    val visibleMenu: List<MenuItem>
        get() = menu.filter { category == null || it.category == category }

    val favourites: List<MenuItem>
        get() = StarterData.favouriteIds.mapNotNull { id -> menu.firstOrNull { it.id == id } }
            .ifEmpty { menu.take(3) }

    val cartLines: List<OrderLine> get() = cart.lines(menu)
    val cartTotal: Double get() = cart.total(menu)
    val cartCount: Int get() = cart.itemCount
    val deliveryFee: Double get() = if (form.fulfilment == Fulfilment.DELIVERY) form.deliveryArea.fee else 0.0
    val checkoutTotal: Double get() = cartTotal + deliveryFee

    /** An order that is placed but not yet collected. */
    val hasLiveOrder: Boolean get() = activeOrder != null && activeOrder.stage != OrderStage.COLLECTED
}

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerViewModel(
    private val repo: FoodRepository,
    private val auth: AuthService,
    private val activeOrderStore: ActiveOrderStore,
    private val settings: AppSettings = InMemoryAppSettings(),
) : ViewModel() {

    private val _state = MutableStateFlow(
        CustomerUiState(
            selectedTruckId = settings.truckId,
            rememberDetails = settings.rememberDetails,
            form = if (settings.rememberDetails) CheckoutForm(name = settings.savedName, phone = settings.savedPhone) else CheckoutForm(),
        )
    )
    val state: StateFlow<CustomerUiState> = _state.asStateFlow()

    private var orderJob: Job? = null

    init {
        viewModelScope.launch {
            repo.menu()
                .catch { e ->
                    AppLog.e(TAG, "Menu failed to load", e)
                    _state.update { it.copy(loadingMenu = false, message = "Couldn't load the menu. ${e.message.orEmpty()}") }
                }
                .collect { items ->
                    AppLog.d(TAG, "Menu loaded: ${items.size} items, ${items.count { !it.available }} sold out")
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
        // Keep track of who is signed in (Firebase remembers the session between launches)
        viewModelScope.launch {
            auth.currentUser.collect { u ->
                _state.update { s ->
                    val signedIn = u?.takeUnless { it.isGuest }
                    val googleName = signedIn?.name
                    s.copy(
                        user = signedIn,
                        // Pre-fill checkout with the Google name if the customer hasn't typed one
                        form = if (googleName != null && s.form.name.isBlank()) s.form.copy(name = googleName) else s.form,
                    )
                }
            }
        }
        // A signed-in customer's order history, live
        viewModelScope.launch {
            auth.currentUser
                .map { u -> u?.takeUnless { it.isGuest }?.uid }
                .distinctUntilChanged()
                .flatMapLatest { uid ->
                    if (uid == null) flowOf(emptyList<Order>())
                    else repo.ordersForCustomer(uid).catch { e ->
                        AppLog.w(TAG, "Couldn't load order history", e)
                        emit(emptyList())
                    }
                }
                .collect { orders -> _state.update { it.copy(myOrders = orders) } }
        }
        // Pick up an order placed before the app was closed
        activeOrderStore.get()?.let {
            AppLog.d(TAG, "Resuming tracking for saved order")
            watchOrder(it)
        }
    }

    // ---------- account (Google single sign-on) ----------

    fun signInWithGoogle(idToken: String) {
        if (_state.value.signingIn) return
        _state.update { it.copy(signingIn = true) }
        viewModelScope.launch {
            try {
                val u = auth.signInWithGoogle(idToken)
                _state.update { it.copy(signingIn = false, message = "Signed in as ${u.name ?: u.email ?: "you"}") }
            } catch (e: Exception) {
                AppLog.e(TAG, "Google sign-in failed", e)
                _state.update { it.copy(signingIn = false, message = "Sign-in didn't work. ${e.message.orEmpty()}") }
            }
        }
    }

    fun signOut() {
        auth.signOut()
        // A tracked order belongs to the account that placed it, so stop tracking it
        clearActiveOrder()
        _state.update { it.copy(form = CheckoutForm(), message = "Signed out") }
    }

    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                auth.deleteAccount()
                clearActiveOrder()
                settings.savedName = ""
                settings.savedPhone = ""
                _state.update { it.copy(form = CheckoutForm(), message = "Your account has been deleted") }
                onDeleted()
            } catch (e: Exception) {
                AppLog.e(TAG, "Account deletion failed", e)
                _state.update { it.copy(message = e.message ?: "Couldn't delete your account. Please try again.") }
            }
        }
    }

    fun showMessage(text: String) = _state.update { it.copy(message = text) }

    // ---------- browsing ----------

    fun selectTruck(truckId: String) {
        settings.truckId = truckId
        _state.update { it.copy(selectedTruckId = truckId) }
    }

    // ---------- settings ----------

    fun setRememberDetails(remember: Boolean) {
        settings.rememberDetails = remember
        if (!remember) {
            settings.savedName = ""
            settings.savedPhone = ""
        }
        AppLog.d(TAG, "Remember details: $remember")
        _state.update { it.copy(rememberDetails = remember) }
    }

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
            AppLog.d(TAG, "Checkout blocked: ${s.form.errors().keys}")
            _state.update { it.copy(showFormErrors = true) }
            return
        }
        val truck = s.selectedTruck
        if (truck == null) {
            _state.update { it.copy(message = "No truck is open right now. Try again later.") }
            return
        }
        if (s.form.pickupTime != PickupTimes.ASAP && s.form.pickupTime !in PickupTimes.slots(PickupTimes.nowMinutes())) {
            _state.update { it.copy(form = it.form.copy(pickupTime = PickupTimes.ASAP), message = "That time has passed. Please choose another time.") }
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
                    deliveryArea = if (s.form.fulfilment == Fulfilment.DELIVERY) s.form.deliveryArea else null,
                    pickupTime = s.form.pickupTime,
                    payment = s.form.payment,
                    note = s.form.note.trim(),
                    lines = s.cartLines,
                )
                // Feedback from Task 2: only confirm and clear the cart AFTER the order is saved
                val id = repo.placeOrder(order)
                activeOrderStore.set(id)
                if (settings.rememberDetails) {
                    settings.savedName = order.customerName
                    settings.savedPhone = order.phone
                }
                _state.update {
                    it.copy(
                        placing = false,
                        cart = Cart(),
                        form = it.form.copy(note = "", pickupTime = PickupTimes.ASAP), // keep name/phone for next time
                        showFormErrors = false,
                        activeOrder = order.copy(id = id),
                    )
                }
                watchOrder(id)
                onPlaced()
            } catch (e: Exception) {
                AppLog.e(TAG, "Placing order failed", e)
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
                .catch { e -> AppLog.w(TAG, "Stopped tracking order (keeping last known status)", e) }
                .collect { order ->
                    if (order != null) AppLog.d(TAG, "Order ${order.number} is now ${order.stage.name}")
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
                AppLog.d(TAG, "Feedback sent: $rating stars")
            } catch (e: Exception) {
                // Feedback is nice-to-have; never block the customer on it
                AppLog.w(TAG, "Feedback not saved", e)
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

    private companion object { const val TAG = "CustomerVM" }
}
