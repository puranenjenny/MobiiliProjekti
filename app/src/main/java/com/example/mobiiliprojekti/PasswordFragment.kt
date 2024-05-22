package com.example.mobiiliprojekti

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.mobiiliprojekti.services.DatabaseManager
import com.example.mobiiliprojekti.services.EmailServices
import java.util.UUID

class PasswordFragment : DialogFragment() {

    private lateinit var databaseManager: DatabaseManager
    private lateinit var emailService: EmailServices

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.fragment_password, null)

        // set view for dialog
        builder.setView(view)

        // set up database manager
        databaseManager = DatabaseManager(requireContext())
        emailService = EmailServices()

        // Look for text fields and buttons
        val userNameEditText: EditText = view.findViewById(R.id.txt_psw_user)
        val emailEditText: EditText = view.findViewById(R.id.txt_psw_email)


        val registerButton: Button = view.findViewById(R.id.btn_new_psw)
        val cancelButton: Button =  view.findViewById(R.id.btn_cancel_psw)


        registerButton.setOnClickListener {
            // set values of text fields to variables
            val userName = userNameEditText.text.toString()
            val email = emailEditText.text.toString()

            // set generated password to variable
            val newPassword = generateRandomPassword()

            // Check that all fields are filled
            if (userName.isNotEmpty() && email.isNotEmpty()) {
                val result = databaseManager.resetPassword(userName, email, newPassword)

                if (result) {
                    // if username and email are correct new password is sent to email and fragment is closed
                    emailService.sendEmail(email, newPassword)
                    Toast.makeText(requireContext(), "New password is sent to your email!", Toast.LENGTH_SHORT).show()
                    dismiss()
                } else {
                    // Show toast if user name of email is wrong
                    Toast.makeText(requireContext(), "Error: Username and/or email incorrect!", Toast.LENGTH_SHORT).show()
                }
            } else {
                //show toast if some of the text fields is empty
                if (userName.isEmpty()) {
                    Toast.makeText(requireContext(), "Add username!", Toast.LENGTH_SHORT).show()
                }
                if (email.isEmpty()) {
                    Toast.makeText(requireContext(), "Add email address!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Set onClickListener for cancel-button
        cancelButton.setOnClickListener {
            // close forgot password
            dismiss()
        }

        return builder.create()
    }

    override fun onStart() { //makes the background transparent
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    // Generate a random password, for using UUID
    private fun generateRandomPassword(): String {
        return UUID.randomUUID().toString().substring(0, 8)
    }
}