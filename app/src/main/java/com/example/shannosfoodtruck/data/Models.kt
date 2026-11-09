package com.example.shannosfoodtruck.data

import java.util.Locale

/** Menu sections, in the order they appear in the app. */
enum class Category(val label: String) {
    KOTAS("Kotas"),
    SIDES("Sides"),
    ROLLS("Rolls"),
    DRINKS("Drinks");

    companion object {
        fun fromName(name: String?): Category =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) || it.label.equals(name, ignoreCase = true) }
                ?: KOTAS
    }
}

data class MenuItem(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val category: Category,
    /** Short word shown on the coloured tile, e.g. "KOTA". */
    val tag: String,
    val available: Boolean = true,
)

data class Truck(
    val id: String,
    val name: String,
    val spot: String,
    val open: Boolean,
)

/** The four steps an order moves through. Staff move it forward; customers watch it live. */
enum class OrderStage(val label: String, val hint: String, val staffAction: String?) {
    RECEIVED("Order received", "The truck has your order. We'll let you know when it hits the pan.", "Start cooking"),
    COOKING("On the pan", "The team is cooking it now. Usually 8–12 minutes.", "Mark ready"),
    READY("Ready to collect", "Come grab it while it's hot. Show your order number at the window.", "Collected"),
    COLLECTED("Collected", "Enjoy! Tell us how it was.", null);

    fun next(): OrderStage = entries.getOrElse(ordinal + 1) { this }

    companion object {
        fun fromName(name: String?): OrderStage = entries.firstOrNull { it.name == name } ?: RECEIVED
    }
}

enum class Fulfilment(val label: String) { PICKUP("Collect at truck"), DELIVERY("Deliver to me") }

enum class PaymentMethod(val label: String) { CARD("Card"), CASH("Cash at truck") }

data class OrderLine(
    val itemId: String,
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
) {
    val lineTotal: Double get() = quantity * unitPrice
}

data class Order(
    val id: String = "",
    /** Short, human-friendly number the customer shows at the window, e.g. SFT-10427. */
    val number: String,
    val truckId: String,
    val customerName: String,
    val phone: String,
    val fulfilment: Fulfilment,
    val address: String = "",
    val payment: PaymentMethod,
    val note: String = "",
    val lines: List<OrderLine>,
    val stage: OrderStage = OrderStage.RECEIVED,
    val createdAt: Long = System.currentTimeMillis(),
    val customerUid: String = "",
) {
    val total: Double get() = lines.sumOf { it.lineTotal }
    val itemCount: Int get() = lines.sumOf { it.quantity }
}

data class StockItem(
    val id: String,
    val name: String,
    val onHand: Int,
    val reorderAt: Int,
) {
    val status: StockStatus
        get() = when {
            onHand <= 0 -> StockStatus.OUT
            onHand < reorderAt -> StockStatus.LOW
            else -> StockStatus.OK
        }
}

enum class StockStatus(val label: String) { OK("OK"), LOW("Low"), OUT("Out") }

data class Feedback(
    val orderId: String,
    val rating: Int,
    val comment: String,
)

/** Rand formatting used everywhere, e.g. R55.00 */
fun Double.toRand(): String = String.format(Locale.US, "R%.2f", this)
