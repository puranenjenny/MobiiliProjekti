package com.example.mobiiliprojekti.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.mobiiliprojekti.DeleteUserFragment
import com.example.mobiiliprojekti.EditUserFragment
import com.example.mobiiliprojekti.LoginActivity
import com.example.mobiiliprojekti.LoginFragment
import com.example.mobiiliprojekti.R
import com.example.mobiiliprojekti.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // set up text fields and buttons to variables
        val btnEditUser: Button = root.findViewById(R.id.EditPasswordButton)
        val btnDeleteUser: Button = root.findViewById(R.id.DeleteUserBtn)
        val btnLogout: Button = root.findViewById(R.id.LogoutBtn)

        // Set click listener for edit user button
        btnEditUser.setOnClickListener {
            // New instance of edit user fragent
            val fragmentEditUser = EditUserFragment()
            // Show fragment as dialog
            fragmentEditUser.show(parentFragmentManager, "edit_user_dialog")
        }

        // Set click listener for edit user button
        btnDeleteUser.setOnClickListener {
            // New instance of Delete user fragent
            val fragmentDeleteUser = DeleteUserFragment()
            // Show fragment as dialog
            fragmentDeleteUser.show(parentFragmentManager, "Delete_user_dialog")
        }

        btnLogout.setOnClickListener {
            // create intent for starting a new LoginActivity
            val intent = Intent(requireContext(), LoginActivity::class.java)

            // Add flags CLEAR_TASK and NEW_TASK to intent to make sure that new activity
            // Opens to background and removes current activity from pile
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK

            // Start new activity
            startActivity(intent)

            // close current fragment
            parentFragmentManager.beginTransaction().remove(this).commit()

            // Close current activity
            requireActivity().finish()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
