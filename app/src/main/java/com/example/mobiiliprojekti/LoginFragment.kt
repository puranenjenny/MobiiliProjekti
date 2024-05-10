package com.example.mobiiliprojekti

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class LoginFragment : DialogFragment() {

    private lateinit var databaseManager: DatabaseManager
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.fragment_login, null)

        // set view for dialog
        builder.setView(view)

        // set up database manager
        databaseManager = DatabaseManager(requireContext())

        // Look for text fields and buttons
        val userNameEditText: EditText = view.findViewById(R.id.txt_login_user_name)
        val pswEditText: EditText = view.findViewById(R.id.txt_psw_login)

        val loginButton: Button = view.findViewById(R.id.btn_login_user)
        val cancelButton = view.findViewById<Button>(R.id.btn_cancel_login)

        // get admin user and set it to text field
        val adminUsername = databaseManager.fetchAdminUser()
        if (adminUsername != null) {
            userNameEditText.setText(adminUsername)
        }

        loginButton.setOnClickListener {
            // set values of text fields to variables
            val userName = userNameEditText.text.toString()
            val password = pswEditText.text.toString()

            // Check that all fields are filled
            if (userName.isNotEmpty() && password.isNotEmpty()) {
                val result = databaseManager.loginUser(userName, password)
                if (result) {
                    // If user and password are correct navigate to main screen and show toast
                    Toast.makeText(requireContext(), "Welcome $userName!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    startActivity(intent)
                    // close login activity
                    requireActivity().finish()
                } else {
                    // Show toast if username or password are incorrect
                    Toast.makeText(requireContext(), "Error: Username and/or password incorrect! ", Toast.LENGTH_SHORT).show()
                }
            } else {
                //show toast if some of the text fields is empty
                if (userName.isEmpty()) {
                    Toast.makeText(requireContext(), "Add username!", Toast.LENGTH_SHORT).show()
                }
                if (password.isEmpty()) {
                    Toast.makeText(requireContext(), "Add password!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Set onClickListener for cancel-button
        cancelButton.setOnClickListener {
            // close dialog
            dismiss()
        }

        // add onClickListener for txt_forgot_psw -text field
        view.findViewById<TextView>(R.id.txt_forgot_psw)?.setOnClickListener {
            // open PasswordFragment dialog-fragment
            val passwordFragment = PasswordFragment()
            passwordFragment.show(parentFragmentManager, "password_fragment")
            // close login fragment
            dismiss()
        }

        return builder.create()
    }

}
