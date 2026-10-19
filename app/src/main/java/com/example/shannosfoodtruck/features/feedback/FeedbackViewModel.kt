package com.example.shannosfoodtruck.features.feedback

import androidx.lifecycle.ViewModel

class FeedbackViewModel : ViewModel() {
    fun submitFeedback(rating: Float, comments: String): Boolean {
        // Logic to submit or save feedback
        return comments.isNotBlank() || rating > 0
    }
}
