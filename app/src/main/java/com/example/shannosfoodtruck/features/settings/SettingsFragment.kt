package com.example.shannosfoodtruck.features.settings

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
import com.example.shannosfoodtruck.features.auth.LoginFragment
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsFragment : Fragment() {

    private lateinit var userRepository: UserRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userRepository = UserRepository(requireContext())

        val switchNotifs = view.findViewById<SwitchMaterial>(R.id.switchNotifications)
        val switchDark = view.findViewById<SwitchMaterial>(R.id.switchDarkMode)
        val etAddress = view.findViewById<EditText>(R.id.etSettingsAddress)
        val btnSave = view.findViewById<Button>(R.id.btnSaveSettings)
        val btnAccount = view.findViewById<Button>(R.id.btnAccountLogin)

        // Load current settings
        switchNotifs.isChecked = userRepository.getSetting("notifications_enabled", "true") == "true"
        switchDark.isChecked = userRepository.getSetting("dark_mode", "false") == "true"
        etAddress.setText(userRepository.getSetting("delivery_address", "Campus Main Gate, IIE"))

        btnSave.setOnClickListener {
            userRepository.saveSetting("notifications_enabled", switchNotifs.isChecked.toString())
            userRepository.saveSetting("dark_mode", switchDark.isChecked.toString())
            userRepository.saveSetting("delivery_address", etAddress.text.toString())
            Toast.makeText(requireContext(), "Settings saved successfully!", Toast.LENGTH_SHORT).show()
        }

        btnAccount.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}
