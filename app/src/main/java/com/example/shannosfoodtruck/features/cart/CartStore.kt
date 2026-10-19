package com.example.shannosfoodtruck.features.cart

/** One shared, in-memory cart. Starts empty. */
object CartStore {
    private val _items = mutableListOf<CartItem>()
    val items: List<CartItem> get() = _items.toList()

    fun add(name: String, price: Double) {
        val i = _items.indexOfFirst { it.name == name }
        if (i >= 0) _items[i] = _items[i].copy(quantity = _items[i].quantity + 1)
        else _items.add(CartItem(name, 1, price))
    }

    fun remove(name: String) { _items.removeAll { it.name == name } }

    fun clear() = _items.clear()

    fun total(): Double = _items.sumOf { it.price * it.quantity }
}