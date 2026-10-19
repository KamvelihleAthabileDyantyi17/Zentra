package com.example.shannosfoodtruck.features.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.shannosfoodtruck.R
import com.example.shannosfoodtruck.database.UserRepository

class RegisterFragment : Fragment() {

    private lateinit var userRepository: UserRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userRepository = UserRepository(requireContext())

        val etFullName = view.findViewById<EditText>(R.id.etRegFullName)
        val etEmail = view.findViewById<EditText>(R.id.etRegEmail)
        val etPhone = view.findViewById<EditText>(R.id.etRegPhone)
        val etPassword = view.findViewById<EditText>(R.id.etRegPassword)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val fullName = etFullName.text.toString()
            val email = etEmail.text.toString()
            val phone = etPhone.text.toString()
            val password = etPassword.text.toString()

            if (email.isBlank() || password.isBlank() || fullName.isBlank()) {
                Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val success = userRepository.registerUser(email, password, fullName, phone)
            if (success) {
                Toast.makeText(requireContext(), "Account created successfully! Please log in.", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(requireContext(), "Registration failed. Email may already exist.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
