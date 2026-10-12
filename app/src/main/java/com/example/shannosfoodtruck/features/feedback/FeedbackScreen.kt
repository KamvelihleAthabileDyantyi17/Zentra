package com.example.shannosfoodtruck.features.feedback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun FeedbackScreen(modifier: Modifier = Modifier, viewModel: FeedbackViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Feedback Screen (Placeholder)")
    }
}
