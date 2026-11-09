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
    /** Short location shown in lists, e.g. "Cnr Sandown Rd & Discovery Dr, Rivergate". */
    val spot: String,
    val open: Boolean,
    /** Full street address for directions. */
    val address: String = "",
    /** A truck that's planned but not trading yet. Shown as "Coming soon" and can't take orders. */
    val comingSoon: Boolean = false,
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

/** Delivery pricing set by the owner: free inside Parklands, R50 anywhere else. */
enum class DeliveryArea(val label: String, val fee: Double) {
    PARKLANDS("In Parklands", 0.0),
    OUTSIDE("Outside Parklands", 50.0),
}

enum class PaymentMethod(val label: String) { CARD("Card"), CASH("Cash") }

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
    /** Only for delivery orders. */
    val deliveryArea: DeliveryArea? = null,
    /** "ASAP" or a time today like "14:30" (Task 1, FR-04: preferred pickup time). */
    val pickupTime: String = PickupTimes.ASAP,
) {
    val itemsTotal: Double get() = lines.sumOf { it.lineTotal }
    val deliveryFee: Double get() = if (fulfilment == Fulfilment.DELIVERY) deliveryArea?.fee ?: 0.0 else 0.0
    val total: Double get() = itemsTotal + deliveryFee
    val itemCount: Int get() = lines.sumOf { it.quantity }
}

/** One ingredient on a truck. Quantities are in the ingredient's unit (see Recipes.kt), e.g. loaves, kg, slices. */
data class StockItem(
    val id: String,
    val name: String,
    val onHand: Double,
    val reorderAt: Double,
    val unit: String = "",
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

/** 2, 0.5, 1.25: trims needless decimals for quantities. */
fun Double.toQty(): String =
    if (this == Math.floor(this)) String.format(Locale.US, "%.0f", this)
    else String.format(Locale.US, "%.2f", this).trimEnd('0').trimEnd('.')
