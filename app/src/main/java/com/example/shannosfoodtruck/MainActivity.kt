package com.example.shannosfoodtruck

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.shannosfoodtruck.features.cart.CartFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.shannosfoodtruck.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Load Cart by default when app opens just so you can see your work
        if (savedInstanceState == null) {
            loadFragment(CartFragment())
            bottomNav.selectedItemId = R.id.nav_cart
        }

        // Listen for tab clicks
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // TODO: loadFragment(HomeFragment())
                    true
                }
                R.id.nav_menu -> {
                    // TODO: loadFragment(MenuFragment())
                    true
                }
                R.id.nav_cart -> {
                    loadFragment(CartFragment())
                    true
                }
                R.id.nav_feedback -> {
                    // TODO: loadFragment(FeedbackFragment())
                    true
                }
                else -> false
            }
        }
    }

    // Helper function to swap the fragments
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}