package com.example.shannosfoodtruck.database

import java.security.MessageDigest

object PasswordHasher {
    /**
     * Hashes a password string using SHA-256 for secure storage.
     * Never stores plain text passwords.
     */
    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
