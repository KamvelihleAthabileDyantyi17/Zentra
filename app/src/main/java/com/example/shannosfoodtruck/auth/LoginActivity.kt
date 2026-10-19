package com.example.shannosfoodtruck.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.shannosfoodtruck.MainActivity
import com.example.shannosfoodtruck.R
import com.example.shannosfoodtruck.data.SeedData
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        val dark = getSharedPreferences("settings", MODE_PRIVATE).getBoolean("dark", false)
        AppCompatDelegate.setDefaultNightMode(
            if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        if (auth.currentUser != null) { goMain(); return }

        val email = findViewById<EditText>(R.id.inputEmail)
        val pass = findViewById<EditText>(R.id.inputPassword)

        fun valid(): Boolean {
            if (!Validators.isValidEmail(email.text.toString())) {
                email.error = "Enter a valid email"; return false }
            if (!Validators.isValidPassword(pass.text.toString())) {
                pass.error = "Password must be at least 6 characters"; return false }
            return true
        }

        findViewById<Button>(R.id.buttonLogin).setOnClickListener {
            if (!valid()) return@setOnClickListener
            auth.signInWithEmailAndPassword(email.text.toString().trim(), pass.text.toString())
                .addOnSuccessListener { goMain() }
                .addOnFailureListener { toast(it.localizedMessage ?: "Login failed") }
        }
        findViewById<Button>(R.id.buttonRegister).setOnClickListener {
            if (!valid()) return@setOnClickListener
            auth.createUserWithEmailAndPassword(email.text.toString().trim(), pass.text.toString())
                .addOnSuccessListener { goMain() }
                .addOnFailureListener { toast(it.localizedMessage ?: "Registration failed") }
        }
        findViewById<Button>(R.id.buttonGoogle).setOnClickListener {
            try {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(resources.getIdentifier("default_web_client_id", "string", packageName)))
                    .requestEmail().build()
                @Suppress("DEPRECATION")
                startActivityForResult(GoogleSignIn.getClient(this, gso).signInIntent, 100)
            } catch (e: Exception) { toast("Google sign-in not configured yet") }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 100) return
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
            auth.signInWithCredential(GoogleAuthProvider.getCredential(account.idToken, null))
                .addOnSuccessListener { goMain() }
                .addOnFailureListener { toast(it.localizedMessage ?: "Google sign-in failed") }
        } catch (e: ApiException) { toast("Google sign-in cancelled or failed") }
    }

    private fun goMain() {
        SeedData.seedIfEmpty()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()
}