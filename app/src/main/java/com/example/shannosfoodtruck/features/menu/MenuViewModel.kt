package com.example.shannosfoodtruck.features.menu

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.shannosfoodtruck.data.MenuRepository
import com.google.firebase.firestore.ListenerRegistration

/** One item on Shanon's menu. */
data class MenuItem(
    val id: Int,
    val name: String,
    val description: String,
    val price: Double,
    val category: String,   // "Mains" or "Drinks"
    val emoji: String,
    val docId: String = ""  // Added so Firestore delete/updates work properly
)

data class MenuUiState(
    val items: List<MenuItem> = emptyList(),
    val categories: List<String> = listOf("Mains", "Drinks"),
    val selectedCategory: String? = null,   // null = show All
    val query: String = ""
)

class MenuViewModel : ViewModel() {

    private var allItems: List<MenuItem> = emptyList()
    private var registration: ListenerRegistration? = null

    private val _state = MutableStateFlow(MenuUiState())
    val state: StateFlow<MenuUiState> = _state.asStateFlow()

    init {
        // Real-time: runs again whenever menu_items changes in Firestore
        registration = MenuRepository.listen({ items ->
            allItems = items
            _state.update { it.withFilter() }
        })
    }

    fun selectCategory(category: String?) {
        _state.update { it.copy(selectedCategory = category).withFilter() }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query).withFilter() }
    }

    private fun MenuUiState.withFilter(): MenuUiState {
        val filtered = allItems.filter { item ->
            (selectedCategory == null || item.category == selectedCategory) &&
                    (query.isBlank() || item.name.contains(query.trim(), ignoreCase = true))
        }
        return copy(items = filtered)
    }

    override fun onCleared() {
        registration?.remove()
        super.onCleared()
    }
}