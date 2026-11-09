package com.example.shannosfoodtruck.data

import android.content.Context

/** Remembers the customer's current order id, so tracking survives closing the app. */
interface ActiveOrderStore {
    fun get(): String?
    fun set(orderId: String?)
}

class PrefsActiveOrderStore(context: Context) : ActiveOrderStore {
    private val prefs = context.getSharedPreferences("shannos", Context.MODE_PRIVATE)
    override fun get(): String? = prefs.getString(KEY, null)
    override fun set(orderId: String?) = prefs.edit().putString(KEY, orderId).apply()

    private companion object { const val KEY = "active_order_id" }
}

class InMemoryActiveOrderStore(private var value: String? = null) : ActiveOrderStore {
    override fun get(): String? = value
    override fun set(orderId: String?) { value = orderId }
}
