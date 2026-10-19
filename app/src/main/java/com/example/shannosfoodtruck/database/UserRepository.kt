package com.example.shannosfoodtruck.database

import android.content.ContentValues
import android.content.Context

class UserRepository(context: Context) {
    private val dbHelper = FoodDatabaseHelper(context)

    fun registerUser(email: String, passwordRaw: String, fullName: String, phone: String): Boolean {
        val db = dbHelper.writableDatabase
        val passwordHash = PasswordHasher.hashPassword(passwordRaw)
        val cv = ContentValues().apply {
            put("email", email.trim())
            put("password_hash", passwordHash)
            put("full_name", fullName.trim())
            put("phone", phone.trim())
        }
        val result = db.insert(FoodDatabaseHelper.TABLE_USERS, null, cv)
        return result != -1L
    }

    fun loginUser(email: String, passwordRaw: String): Boolean {
        val db = dbHelper.readableDatabase
        val passwordHash = PasswordHasher.hashPassword(passwordRaw)
        val cursor = db.query(
            FoodDatabaseHelper.TABLE_USERS,
            arrayOf("id"),
            "email = ? AND password_hash = ?",
            arrayOf(email.trim(), passwordHash),
            null,
            null,
            null
        )
        val isValid = cursor.count > 0
        cursor.close()
        return isValid
    }

    fun getSetting(key: String, defaultValue: String): String {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            FoodDatabaseHelper.TABLE_SETTINGS,
            arrayOf("value"),
            "key = ?",
            arrayOf(key),
            null,
            null,
            null
        )
        var value = defaultValue
        if (cursor.moveToFirst()) {
            value = cursor.getString(0)
        }
        cursor.close()
        return value
    }

    fun saveSetting(key: String, value: String) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("key", key)
            put("value", value)
        }
        db.insertWithOnConflict(
            FoodDatabaseHelper.TABLE_SETTINGS,
            null,
            cv,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }
}
