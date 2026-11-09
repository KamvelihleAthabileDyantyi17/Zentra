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

/**
 * The customer's settings (Account → Settings). Saved on the phone only.
 *  - truckId: the truck they usually order from
 *  - rememberDetails: keep name and number for next time
 */
interface AppSettings {
    var truckId: String?
    var rememberDetails: Boolean
    var savedName: String
    var savedPhone: String
}

class PrefsAppSettings(context: Context) : AppSettings {
    private val prefs = context.getSharedPreferences("shannos_settings", Context.MODE_PRIVATE)

    override var truckId: String?
        get() = prefs.getString("truck_id", null)
        set(value) = prefs.edit().putString("truck_id", value).apply()

    override var rememberDetails: Boolean
        get() = prefs.getBoolean("remember_details", true)
        set(value) = prefs.edit().putBoolean("remember_details", value).apply()

    override var savedName: String
        get() = prefs.getString("saved_name", "").orEmpty()
        set(value) = prefs.edit().putString("saved_name", value).apply()

    override var savedPhone: String
        get() = prefs.getString("saved_phone", "").orEmpty()
        set(value) = prefs.edit().putString("saved_phone", value).apply()
}

class InMemoryAppSettings(
    override var truckId: String? = null,
    override var rememberDetails: Boolean = true,
    override var savedName: String = "",
    override var savedPhone: String = "",
) : AppSettings
