package com.example.shannosfoodtruck.features.cart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.shannosfoodtruck.R
import com.example.shannosfoodtruck.features.orderstatus.OrderStatusFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class CartFragment : Fragment() {

    private val viewModel: CartViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cart, container, false)
        render(view)

        view.findViewById<Button>(R.id.btnConfirmOrder).setOnClickListener {
            val items = viewModel.currentCart
            if (items.isEmpty()) {
                Toast.makeText(requireContext(), "Your cart is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val total = viewModel.calculateTotal()
            val orderId = "SFT-${(10000..99999).random()}"

            // Save the order online so it appears in Firestore
            FirebaseFirestore.getInstance().collection("orders").document(orderId).set(
                mapOf(
                    "id" to orderId,
                    "customer" to (FirebaseAuth.getInstance().currentUser?.email ?: "guest"),
                    "total" to total,
                    "status" to "Received",
                    "items" to items.map { "${it.quantity} x ${it.name}" },
                    "createdAt" to System.currentTimeMillis()
                )
            ).addOnFailureListener {
                Toast.makeText(requireContext(), "Could not save order online", Toast.LENGTH_SHORT).show()
            }

            val tracker = OrderStatusFragment()
            tracker.arguments = Bundle().apply {
                putString("ORDER_ID", orderId)
                putDouble("TOTAL_PRICE", total)
            }
            CartStore.clear()
            Toast.makeText(requireContext(), "Order Confirmed!", Toast.LENGTH_SHORT).show()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, tracker)
                .addToBackStack(null)
                .commit()
        }
        return view
    }

    /** Draws the cart rows and the total from the shared cart. */
    private fun render(view: View) {
        val box = view.findViewById<LinearLayout>(R.id.cartItemsContainer)
        val tvTotal = view.findViewById<TextView>(R.id.tvTotalPrice)
        box.removeAllViews()

        if (viewModel.currentCart.isEmpty()) {
            box.addView(TextView(requireContext()).apply { text = "Your cart is empty. Add items from the Menu." })
        }
        viewModel.currentCart.forEach { item ->
            val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(TextView(requireContext()).apply {
                text = String.format(Locale.getDefault(), "%d x %s   R%.2f",
                    item.quantity, item.name, item.price * item.quantity)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(Button(requireContext()).apply {
                text = "Remove"
                setOnClickListener { CartStore.remove(item.name); render(view) }
            })
            box.addView(row)
        }
        tvTotal.text = String.format(Locale.getDefault(), "Total: R%.2f", viewModel.calculateTotal())
    }
}