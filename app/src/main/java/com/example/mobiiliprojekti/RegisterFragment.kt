package com.example.mobiiliprojekti

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.example.mobiiliprojekti.services.DatabaseManager
import com.example.mobiiliprojekti.services.SharedViewModel

class RegisterFragment : DialogFragment() {

    private lateinit var databaseManager: DatabaseManager
    private lateinit var sharedViewModel: SharedViewModel
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.fragment_register, null)

        builder.setView(view)

        databaseManager = DatabaseManager(requireContext())
        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]

        val userNameEditText: EditText = view.findViewById(R.id.txt_user_name)
        val emailEditText: EditText = view.findViewById(R.id.txt_email)
        val passwordEditText: EditText = view.findViewById(R.id.txt_psw_register)

        val registerButton: Button = view.findViewById(R.id.btn_register_user)
        val cancelButton: Button =  view.findViewById(R.id.btn_cancel)
        val primaryUserSwitch: SwitchCompat =  view.findViewById(R.id.switch_primary_user)


        registerButton.setOnClickListener {
            val userName = userNameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (userName.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                val result = databaseManager.addUser(userName, email, password)
                if (primaryUserSwitch.isChecked) {
                    databaseManager.updateUserAsAdmin(result)
                }
                if (result != -1L) {
                    val userId = databaseManager.getUserId(userName)
                    if (userId != -1) {
                        sharedViewModel.setUserId(userId)
                        Log.d("RegisterFragment ", "Register User ID: $userId")

                        Toast.makeText(requireContext(), "New user added!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                } else {
                    Toast.makeText(requireContext(), "Error: Username or email already exist", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (userName.isEmpty()) {
                    Toast.makeText(requireContext(), "Add username!", Toast.LENGTH_SHORT).show()
                }
                if (email.isEmpty()) {
                    Toast.makeText(requireContext(), "Add email address!", Toast.LENGTH_SHORT).show()
                }
                if (password.isEmpty()) {
                    Toast.makeText(requireContext(), "Add password!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        cancelButton.setOnClickListener {
            dismiss()
        }


    }
        return builder.create()
    }}