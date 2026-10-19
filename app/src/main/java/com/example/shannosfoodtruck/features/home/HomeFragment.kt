package com.example.shannosfoodtruck.features.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.shannosfoodtruck.R
import com.example.shannosfoodtruck.features.menu.MenuFragment
import com.example.shannosfoodtruck.features.orderstatus.OrderStatusFragment

class HomeFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        // Build one card per featured item
        val featuredContainer = view.findViewById<LinearLayout>(R.id.featuredContainer)
        val layoutInflater = LayoutInflater.from(requireContext())

        viewModel.featuredItems.forEach { item ->
            val card = layoutInflater.inflate(R.layout.item_featured, featuredContainer, false)
            card.findViewById<TextView>(R.id.itemName).text = item.name
            card.findViewById<TextView>(R.id.itemDescription).text = item.description
            card.findViewById<TextView>(R.id.itemPrice).text = item.price
            featuredContainer.addView(card)
        }

        view.findViewById<Button>(R.id.buttonOrderNow)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MenuFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<Button>(R.id.buttonTrackOrder)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, OrderStatusFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}
