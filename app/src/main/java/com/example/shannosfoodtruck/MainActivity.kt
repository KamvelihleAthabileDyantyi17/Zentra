package com.example.shannosfoodtruck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.shannosfoodtruck.ui.ShannosNavHost
import com.example.shannosfoodtruck.ui.theme.ShannosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as ShannosApp).container
        setContent {
            ShannosTheme {
                ShannosNavHost(container)
            }
        }
    }
}
