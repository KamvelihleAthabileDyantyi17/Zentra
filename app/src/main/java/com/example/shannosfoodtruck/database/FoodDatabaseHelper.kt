package com.example.shannosfoodtruck.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class FoodDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "shannos_food_truck.db"
        private const val DATABASE_VERSION = 1

        // Tables
        const val TABLE_USERS = "users"
        const val TABLE_MENU_ITEMS = "menu_items"
        const val TABLE_ORDERS = "orders"
        const val TABLE_SETTINGS = "settings"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Users Table
        db.execSQL(
            """
            CREATE TABLE $TABLE_USERS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                email TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                full_name TEXT NOT NULL,
                phone TEXT
            )
            """.trimIndent()
        )

        // Menu Items Table
        db.execSQL(
            """
            CREATE TABLE $TABLE_MENU_ITEMS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                price REAL NOT NULL,
                category TEXT NOT NULL,
                emoji TEXT NOT NULL
            )
            """.trimIndent()
        )

        // Orders Table
        db.execSQL(
            """
            CREATE TABLE $TABLE_ORDERS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                order_number TEXT NOT NULL,
                user_email TEXT NOT NULL,
                items_summary TEXT NOT NULL,
                total_price REAL NOT NULL,
                status TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent()
        )

        // Settings Table
        db.execSQL(
            """
            CREATE TABLE $TABLE_SETTINGS (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
            """.trimIndent()
        )

        seedInitialData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MENU_ITEMS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ORDERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SETTINGS")
        onCreate(db)
    }

    private fun seedInitialData(db: SQLiteDatabase) {
        // Seed 10 Users
        for (i in 1..10) {
            val cv = ContentValues().apply {
                put("email", "student$i@iie.ac.za")
                put("password_hash", PasswordHasher.hashPassword("Password123!"))
                put("full_name", "Student User $i")
                put("phone", "082000000$i")
            }
            db.insert(TABLE_USERS, null, cv)
        }

        // Seed 10 Menu Items
        val menuItemsList = listOf(
            Triple("Signature Kota", Pair("Loaded bread, chips, russian & cheese, Shanon's secret sauce", 55.0), Pair("Mains", "🍔")),
            Triple("Half Kota", Pair("All the flavour, half the size", 32.0), Pair("Mains", "🍔")),
            Triple("Amagwinya (4 pack)", Pair("Soft, fluffy fried dough", 25.0), Pair("Mains", "🥯")),
            Triple("Slap Chips", Pair("Extra portion of chips, salt & vinegar on request", 20.0), Pair("Mains", "🍟")),
            Triple("Cheese Russian Roll", Pair("Grilled russian with melted cheddar cheese", 35.0), Pair("Mains", "🌭")),
            Triple("Boerewors Roll", Pair("Traditional boerewors with relish", 40.0), Pair("Mains", "🌭")),
            Triple("Cold Drink (330ml)", Pair("Ask about today's flavours", 15.0), Pair("Drinks", "🥤")),
            Triple("Homemade Ice Tea", Pair("Brewed fresh daily", 18.0), Pair("Drinks", "🧃")),
            Triple("Rooibos Iced Tea", Pair("Refreshing local herbal tea", 20.0), Pair("Drinks", "🍵")),
            Triple("Sparkling Water (500ml)", Pair("Pure chilled mineral water", 12.0), Pair("Drinks", "💧"))
        )

        for ((name, details, catEmoji) in menuItemsList) {
            val cv = ContentValues().apply {
                put("name", name)
                put("description", details.first)
                put("price", details.second)
                put("category", catEmoji.first)
                put("emoji", catEmoji.second)
            }
            db.insert(TABLE_MENU_ITEMS, null, cv)
        }

        // Seed 10 Orders
        for (i in 1..10) {
            val cv = ContentValues().apply {
                put("order_number", "SFT-1000$i")
                put("user_email", "student$i@iie.ac.za")
                put("items_summary", "1x Signature Kota, 1x Cold Drink")
                put("total_price", 70.0 + i * 5)
                put("status", if (i % 2 == 0) "Completed" else "Preparing")
            }
            db.insert(TABLE_ORDERS, null, cv)
        }

        // Seed 10 Settings records
        val defaultSettings = mapOf(
            "notifications_enabled" to "true",
            "dark_mode" to "false",
            "currency" to "ZAR",
            "language" to "English",
            "delivery_address" to "Campus Main Gate, IIE",
            "auto_receipts" to "true",
            "vibration_alerts" to "true",
            "favorite_location" to "Shanon's Truck Main Stand",
            "app_version" to "1.0.0",
            "support_email" to "support@shannosfoodtruck.co.za"
        )

        for ((key, value) in defaultSettings) {
            val cv = ContentValues().apply {
                put("key", key)
                put("value", value)
            }
            db.insert(TABLE_SETTINGS, null, cv)
        }
    }
}
