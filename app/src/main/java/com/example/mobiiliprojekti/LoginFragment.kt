package com.example.mobiiliprojekti

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.mobiiliprojekti.services.DatabaseManager
import com.example.mobiiliprojekti.services.SessionManager

class LoginFragment : DialogFragment() {

    private lateinit var databaseManager: DatabaseManager
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.fragment_login, null)

        // set view for dialog
        builder.setView(view)

        // set up text fields and buttons
        val userNameEditText: EditText = view.findViewById(R.id.txt_login_user_name)
        val pswEditText: EditText = view.findViewById(R.id.txt_psw_login)
        val loginButton: Button = view.findViewById(R.id.btn_login_user)
        val cancelButton = view.findViewById<Button>(R.id.btn_cancel_login)

        // Initialize database manager
        databaseManager = DatabaseManager(requireContext())

        // Set admin username to text-field if it is available
        // and also activate biometric authentication when primary user is set
        val (adminUserId, adminUsername) = databaseManager.fetchAdminUser()
        if (adminUsername != null) {
            userNameEditText.setText(adminUsername)

            // Initialize biometric prompt and prompt info
            biometricPrompt = createBiometricPrompt()
            promptInfo = createPromptInfo(adminUsername)

            biometricPrompt.authenticate(promptInfo)
        }

        // Set click listener for login button
        loginButton.setOnClickListener {
            // set values of text fields to variables
            val userName = userNameEditText.text.toString()
            val password = pswEditText.text.toString()

            // Check that all fields are filled
            if (userName.isNotEmpty() && password.isNotEmpty()) {
                val result = databaseManager.loginUser(userName, password)
                if (result) {
                    // If user and password are correct navigate to main screen
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

        // Show biometric prompt if admin user is available
        if (adminUsername != null && adminUserId != null) {
            SessionManager.setLoggedInUserId(adminUserId)
            biometricPrompt.authenticate(promptInfo)
        }

        return builder.create()
    }

    override fun onStart() { //makes the background transparent
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    // function for allowing biometric authentication
    private fun createBiometricPrompt(): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(requireContext())
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // Handle authentication error
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                // If user and password are correct navigate to main screen
                val intent = Intent(requireContext(), MainActivity::class.java)
                startActivity(intent)
                // close login activity
                requireActivity().finish()
            }

            override fun onAuthenticationFailed() {
                // Handle authentication failure
            }
        }
        return BiometricPrompt(this, executor, callback)
    }

    // function for building a ui for biometric authentication
    private fun createPromptInfo(username:String): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle("Welcome $username!")
            .setDescription("Place your finger on the fingerprint sensor to login.")
            .setNegativeButtonText("Login with password")
            .build()
    }
}
