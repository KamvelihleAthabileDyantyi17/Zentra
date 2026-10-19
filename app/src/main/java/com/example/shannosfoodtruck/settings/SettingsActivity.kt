package com.example.shannosfoodtruck.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import com.example.shannosfoodtruck.R
import com.example.shannosfoodtruck.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        findViewById<Button>(R.id.buttonManageMenu).setOnClickListener {
            startActivity(Intent(this, com.example.shannosfoodtruck.admin.AdminMenuActivity::class.java))
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val name = findViewById<EditText>(R.id.inputDisplayName)
        val notif = findViewById<SwitchCompat>(R.id.switchNotifications)
        val dark = findViewById<SwitchCompat>(R.id.switchDark)
        name.setText(prefs.getString("name", ""))
        notif.isChecked = prefs.getBoolean("notifications", true)
        dark.isChecked = prefs.getBoolean("dark", false)

        findViewById<Button>(R.id.buttonSave).setOnClickListener {
            prefs.edit().putString("name", name.text.toString().trim())
                .putBoolean("notifications", notif.isChecked)
                .putBoolean("dark", dark.isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (dark.isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.buttonLogout).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }
    }
}