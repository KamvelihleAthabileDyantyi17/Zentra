package com.example.shannosfoodtruck.features.orderstatus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.shannosfoodtruck.R
import android.app.fragment
class OrderStatusFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflates the fragment_order_status.xml layout we made earlier
        return inflater.inflate(R.layout.fragment_order_status, container, false)
    }
}