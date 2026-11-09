package com.example.shannosfoodtruck.data

/**
 * Pickup / delivery time choices (Task 1, FR-04: "place an order with a preferred pickup time").
 *
 * Trading hours come from the Task 1 plan (NFR-04: 06:00 to 20:00). Customers can choose
 * "As soon as possible" or a 15-minute slot later today, starting at least 15 minutes from now
 * (the kitchen needs 10 to 15 minutes per order).
 *
 * Works with plain minutes-since-midnight so it runs on every Android version (java.time needs API 26).
 */
object PickupTimes {
    const val ASAP = "ASAP"
    const val OPEN_MINUTES = 6 * 60
    const val CLOSE_MINUTES = 20 * 60
    const val STEP_MINUTES = 15
    const val LEAD_MINUTES = 15

    /** Slots like "14:30" from the next possible time until closing. Empty if the truck is closed for today. */
    fun slots(nowMinutes: Int): List<String> {
        val earliest = maxOf(nowMinutes + LEAD_MINUTES, OPEN_MINUTES)
        val first = ((earliest + STEP_MINUTES - 1) / STEP_MINUTES) * STEP_MINUTES // round up to the next quarter hour
        return (first..CLOSE_MINUTES step STEP_MINUTES).map(::format)
    }

    fun isOpen(nowMinutes: Int): Boolean = nowMinutes in OPEN_MINUTES until CLOSE_MINUTES

    fun format(minutes: Int): String = String.format(java.util.Locale.US, "%02d:%02d", minutes / 60, minutes % 60)

    fun label(value: String): String = if (value == ASAP) "As soon as possible" else value

    fun nowMinutes(): Int {
        val c = java.util.Calendar.getInstance()
        return c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE)
    }
}
