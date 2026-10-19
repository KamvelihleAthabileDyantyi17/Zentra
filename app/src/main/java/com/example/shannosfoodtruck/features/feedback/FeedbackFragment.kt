package com.example.shannosfoodtruck.features.feedback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.shannosfoodtruck.R

class FeedbackFragment : Fragment() {

    private val viewModel: FeedbackViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feedback, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ratingBar = view.findViewById<RatingBar>(R.id.ratingBar)
        val etComments = view.findViewById<EditText>(R.id.etFeedbackComments)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitFeedback)

        btnSubmit.setOnClickListener {
            val rating = ratingBar.rating
            val comments = etComments.text.toString()

            viewModel.submitFeedback(rating, comments)
            Toast.makeText(requireContext(), "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
            etComments.text.clear()
        }
    }
}
