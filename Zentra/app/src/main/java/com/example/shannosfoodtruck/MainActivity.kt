package com.example.shannosfoodtruck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.shannosfoodtruck.common.AppNavigation
import com.example.shannosfoodtruck.ui.theme.ShannosFoodTruckTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShannosFoodTruckTheme {
                AppNavigation()
            }
        }
    }
}
