package com.example.shannosfoodtruck.features.cart

import androidx.lifecycle.ViewModel

// A simple data structure for cart items
data class CartItem(val name: String, val quantity: Int, val price: Double)
class CartViewModel : ViewModel() {
    val currentCart: List<CartItem> get() = CartStore.items
    fun calculateTotal(): Double = CartStore.total()
}