package com.example.shannosfoodtruck.features.menu

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** One item on Shanon's menu (matches menu.html on the website). */
data class MenuItem(
    val id: Int,
    val name: String,
    val description: String,
    val price: Double,
    val category: String,   // "Mains" or "Drinks"
    val emoji: String
)

data class MenuUiState(
    val items: List<MenuItem> = emptyList(),
    val categories: List<String> = listOf("Mains", "Drinks"),
    val selectedCategory: String? = null,   // null = show All
    val query: String = ""
)

class MenuViewModel : ViewModel() {

    // Simulated data for now. Later this can come from the database.
    private val allItems = listOf(
        MenuItem(1, "Signature Kota", "Loaded bread, chips, russian & cheese, Shanon's secret sauce", 55.0, "Mains", "🥙"),
        MenuItem(2, "Half Kota", "All the flavour, half the size", 32.0, "Mains", "🌭"),
        MenuItem(3, "Amagwinya (4 pack)", "Soft, fluffy fried dough", 25.0, "Mains", "🍩"),
        MenuItem(4, "Slap Chips", "Extra portion of chips, salt & vinegar on request", 20.0, "Mains", "🍟"),
        MenuItem(5, "Cold Drink (330ml)", "Ask about today's flavours", 15.0, "Drinks", "🥤"),
        MenuItem(6, "Homemade Ice Tea", "Brewed fresh daily", 18.0, "Drinks", "🧃")
    )

    private val _state = MutableStateFlow(MenuUiState(items = allItems))
    val state: StateFlow<MenuUiState> = _state.asStateFlow()

    fun selectCategory(category: String?) {
        _state.update { it.copy(selectedCategory = category).withFilter() }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query).withFilter() }
    }

    /** Re-applies the category and search filters to the full list. */
    private fun MenuUiState.withFilter(): MenuUiState {
        val filtered = allItems.filter { item ->
            (selectedCategory == null || item.category == selectedCategory) &&
                    (query.isBlank() || item.name.contains(query.trim(), ignoreCase = true))
        }
        return copy(items = filtered)
    }
}