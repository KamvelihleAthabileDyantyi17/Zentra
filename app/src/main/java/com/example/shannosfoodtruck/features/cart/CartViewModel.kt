package com.example.shannosfoodtruck.features.cart

import androidx.lifecycle.ViewModel

// A simple data structure for cart items
data class CartItem(val name: String, val quantity: Int, val price: Double)

class CartViewModel : ViewModel() {

    // This simulates the data that will eventually be passed over from the Menu
    val currentCart = listOf(
        CartItem("Amagwinya", 2, 20.0), // 2 * R20 = R40
        CartItem("Kota Special", 1, 55.0),
        CartItem("Iced Rooibos", 1, 20.0)
    )

    // Function to calculate the total price dynamically
    fun calculateTotal(): Double {
        var total = 0.0
        for (item in currentCart) {
            total += (item.price * item.quantity)
        }
        return total
    }
}