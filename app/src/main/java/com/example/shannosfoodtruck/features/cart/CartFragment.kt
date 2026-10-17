package com.example.shannosfoodtruck.features.cart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.shannosfoodtruck.R
import com.example.shannosfoodtruck.features.orderstatus.OrderStatusFragment
import java.util.Locale // <-- Added to fix the yellow String format warning

class CartFragment : Fragment() {

    // Initialize the ViewModel
    private val viewModel: CartViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cart, container, false)

        val btnConfirm = view.findViewById<Button>(R.id.btnConfirmOrder)
        val tvTotal = view.findViewById<TextView>(R.id.tvTotalPrice)

        // Ask the ViewModel for the total and update the UI
        val finalTotal = viewModel.calculateTotal()
        tvTotal.text = String.format(Locale.getDefault(), "R%.2f", finalTotal)

        btnConfirm.setOnClickListener {
            Toast.makeText(requireContext(), "Order Confirmed!", Toast.LENGTH_SHORT).show()

            // 1. Create the Bundle with our data
            val bundle = Bundle()
            bundle.putString("ORDER_ID", "SFT-99999")
            bundle.putDouble("TOTAL_PRICE", finalTotal)

            // 2. Attach the bundle to the OrderStatusFragment
            val trackerFragment = OrderStatusFragment()
            trackerFragment.arguments = bundle

            // 3. Swap the screen
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, trackerFragment)
                .addToBackStack(null)
                .commit()
        }

        return view
    }
}