package com.example.shannosfoodtruck.auth

object Validators {
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    fun isValidEmail(email: String) = emailRegex.matches(email.trim())
    fun isValidPassword(password: String) = password.length >= 6
}