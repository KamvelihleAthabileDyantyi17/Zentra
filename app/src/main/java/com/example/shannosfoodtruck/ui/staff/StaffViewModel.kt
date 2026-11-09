package com.example.shannosfoodtruck.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shannosfoodtruck.data.AppLog
import com.example.shannosfoodtruck.data.AuthService
import com.example.shannosfoodtruck.data.FoodRepository
import com.example.shannosfoodtruck.data.MenuItem
import com.example.shannosfoodtruck.data.Order
import com.example.shannosfoodtruck.data.StockItem
import com.example.shannosfoodtruck.data.Truck
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StaffSection(val label: String) { ORDERS("Orders"), MENU("Menu"), STOCK("Stock") }

data class StaffUiState(
    /** null while we check whether this phone is already signed in as staff. */
    val signedIn: Boolean? = null,
    val signingIn: Boolean = false,
    val error: String? = null,
    val section: StaffSection = StaffSection.ORDERS,
    val trucks: List<Truck> = emptyList(),
    val truckId: String? = null,
    val orders: List<Order> = emptyList(),
    val menu: List<MenuItem> = emptyList(),
    val stock: List<StockItem> = emptyList(),
    val busy: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class StaffViewModel(
    private val repo: FoodRepository,
    private val auth: AuthService,
) : ViewModel() {

    private val _state = MutableStateFlow(StaffUiState())
    val state: StateFlow<StaffUiState> = _state.asStateFlow()

    private val selectedTruck = MutableStateFlow<String?>(null)
    private var started = false

    init {
        viewModelScope.launch {
            val staff = runCatching { auth.isStaff() }.getOrDefault(false)
            _state.update { it.copy(signedIn = staff) }
            if (staff) startListening()
        }
    }

    fun signIn(email: String, password: String) = attemptSignIn { auth.signInStaff(email, password) }

    /** Staff single sign-on with Google. The Google account must be on the staff list. */
    fun signInWithGoogle(idToken: String) = attemptSignIn { auth.signInStaffWithGoogle(idToken) }

    fun showError(message: String) = _state.update { it.copy(error = message) }

    private fun attemptSignIn(block: suspend () -> Unit) {
        if (_state.value.signingIn) return
        _state.update { it.copy(signingIn = true, error = null) }
        viewModelScope.launch {
            try {
                block()
                AppLog.i(TAG, "Staff dashboard opened")
                _state.update { it.copy(signingIn = false, signedIn = true) }
                startListening()
            } catch (e: Exception) {
                AppLog.w(TAG, "Staff sign-in failed", e)
                _state.update { it.copy(signingIn = false, error = e.message ?: "Sign-in failed") }
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _state.update { StaffUiState(signedIn = false) }
        started = false
    }

    fun selectSection(section: StaffSection) = _state.update { it.copy(section = section) }

    fun selectTruck(truckId: String) {
        selectedTruck.value = truckId
        _state.update { it.copy(truckId = truckId) }
    }

    fun advance(order: Order) = runAction {
        AppLog.d(TAG, "Moving ${order.number} from ${order.stage.name} to ${order.stage.next().name}")
        repo.setStage(order.id, order.stage.next())
    }

    fun toggleAvailable(item: MenuItem) = runAction { repo.setAvailable(item.id, !item.available) }

    fun loadStarterData() = runAction { repo.loadStarterData() }

    fun dismissError() = _state.update { it.copy(error = null) }

    private fun runAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                block()
            } catch (e: Exception) {
                AppLog.e(TAG, "Staff action failed", e)
                _state.update { it.copy(error = e.message ?: "Something went wrong") }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    private fun startListening() {
        if (started) return
        started = true
        viewModelScope.launch {
            repo.trucks().catch { e -> reportError(e) }.collect { trucks ->
                _state.update { it.copy(trucks = trucks) }
                if (selectedTruck.value == null && trucks.isNotEmpty()) selectTruck(trucks.first().id)
            }
        }
        viewModelScope.launch {
            repo.menu().catch { e -> reportError(e) }.collect { menu -> _state.update { it.copy(menu = menu) } }
        }
        viewModelScope.launch {
            selectedTruck.filterNotNull()
                .flatMapLatest { id -> repo.openOrders(id).catch { e -> reportError(e) } }
                .collect { orders -> _state.update { it.copy(orders = orders) } }
        }
        viewModelScope.launch {
            selectedTruck.filterNotNull()
                .flatMapLatest { id -> repo.stock(id).catch { e -> reportError(e) } }
                .collect { stock -> _state.update { it.copy(stock = stock) } }
        }
    }

    private fun reportError(e: Throwable) {
        AppLog.w(TAG, "Live data stopped", e)
        _state.update { it.copy(error = e.message ?: "Connection problem") }
    }

    private companion object { const val TAG = "StaffVM" }
}
