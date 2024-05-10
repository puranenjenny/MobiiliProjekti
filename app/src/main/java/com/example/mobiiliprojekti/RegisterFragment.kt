package com.example.mobiiliprojekti

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.DialogFragment

class RegisterFragment : DialogFragment() {

    private lateinit var databaseManager: DatabaseManager
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.fragment_register, null)

        // set view for dialog
        builder.setView(view)

        // set up database manager
        databaseManager = DatabaseManager(requireContext())

        // Look for text fields and buttons
        val userNameEditText: EditText = view.findViewById(R.id.txt_user_name)
        val emailEditText: EditText = view.findViewById(R.id.txt_email)
        val passwordEditText: EditText = view.findViewById(R.id.txt_psw_register)

        val registerButton: Button = view.findViewById(R.id.btn_register_user)
        val cancelButton: Button =  view.findViewById(R.id.btn_cancel)
        val primaryUserSwitch: SwitchCompat =  view.findViewById(R.id.switch_primary_user)


        registerButton.setOnClickListener {
            // set values of text fields to variables
            val userName = userNameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            // Check that all fields are filled
            if (userName.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                val result = databaseManager.addUser(userName, email, password)
                if (primaryUserSwitch.isChecked) {
                    // If switch is selected set user as a primary user
                    databaseManager.updateUserAsAdmin(result)
                }
                if (result != -1L) {
                    // If user was added successfully navigate to main screen and show toast
                    Toast.makeText(requireContext(), "New user added!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    startActivity(intent)
                    // close login activity
                    requireActivity().finish()
                } else {
                    // Show toast if user exist already in db
                    Toast.makeText(requireContext(), "Error: Username or email already exist", Toast.LENGTH_SHORT).show()
                }
            } else {
                //show toast if some of the text fields is empty
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

        // onClickListener for "Cancel"-button
        cancelButton.setOnClickListener {
            // close register-fragment if button is pressed
            dismiss()
        }

        return builder.create()
    }

}