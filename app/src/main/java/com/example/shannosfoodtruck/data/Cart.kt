package com.example.shannosfoodtruck.data

/**
 * The customer's cart. Immutable: every change returns a new Cart, which keeps
 * Compose state simple and makes the maths easy to unit test.
 *
 * Quantities are stored by menu item id; prices always come from the current menu,
 * so a price change by the owner shows up straight away.
 */
data class Cart(val quantities: Map<String, Int> = emptyMap()) {

    fun quantityOf(itemId: String): Int = quantities[itemId] ?: 0

    fun add(itemId: String): Cart = setQuantity(itemId, quantityOf(itemId) + 1)

    fun removeOne(itemId: String): Cart = setQuantity(itemId, quantityOf(itemId) - 1)

    fun setQuantity(itemId: String, quantity: Int): Cart {
        val updated = quantities.toMutableMap()
        if (quantity <= 0) updated.remove(itemId) else updated[itemId] = minOf(quantity, MAX_PER_ITEM)
        return Cart(updated)
    }

    /** Drops anything that is no longer on the menu or has been marked sold out. */
    fun keepOnly(menu: List<MenuItem>): Cart {
        val orderable = menu.filter { it.available }.map { it.id }.toSet()
        return Cart(quantities.filterKeys { it in orderable })
    }

    fun lines(menu: List<MenuItem>): List<OrderLine> =
        menu.mapNotNull { item ->
            val q = quantityOf(item.id)
            if (q > 0) OrderLine(item.id, item.name, q, item.price) else null
        }

    fun total(menu: List<MenuItem>): Double = lines(menu).sumOf { it.lineTotal }

    val itemCount: Int get() = quantities.values.sum()

    val isEmpty: Boolean get() = quantities.isEmpty()

    companion object {
        const val MAX_PER_ITEM = 20
    }
}
