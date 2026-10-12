package com.example.shannosfoodtruck.features.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun MenuScreen(modifier: Modifier = Modifier, viewModel: MenuViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Menu Screen (Placeholder)")
    }
}
