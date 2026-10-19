package com.example.shannosfoodtruck.features.menu

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shannosfoodtruck.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MenuFragment : Fragment() {

    private val viewModel: MenuViewModel by viewModels()
    private lateinit var menuAdapter: MenuAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewMenu)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        menuAdapter = MenuAdapter(emptyList()) { menuItem ->
            Toast.makeText(requireContext(), "${menuItem.name} added to cart!", Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = menuAdapter

        // Search edit text
        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onQueryChange(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Category filter buttons
        view.findViewById<Button>(R.id.btnCatAll).setOnClickListener {
            viewModel.selectCategory(null)
        }
        view.findViewById<Button>(R.id.btnCatMains).setOnClickListener {
            viewModel.selectCategory("Mains")
        }
        view.findViewById<Button>(R.id.btnCatDrinks).setOnClickListener {
            viewModel.selectCategory("Drinks")
        }

        // Observe state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                menuAdapter.updateItems(state.items)
            }
        }
    }
}
