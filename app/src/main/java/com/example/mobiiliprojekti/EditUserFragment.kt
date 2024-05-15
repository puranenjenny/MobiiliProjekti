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
import com.example.mobiiliprojekti.services.DatabaseManager

class EditUserFragment : DialogFragment() {

    private lateinit var databaseManager: DatabaseManager
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.fragment_edit_user, null)

        // set view for dialog
        builder.setView(view)

        // set up database manager
        databaseManager = DatabaseManager(requireContext())

        // Look for text fields and buttons
        val userNameEditText: EditText = view.findViewById(R.id.txt_user_name_edit)
        val emailEditText: EditText = view.findViewById(R.id.txt_email_edit)
        val passwordEditText: EditText = view.findViewById(R.id.txt_psw_edit)


        val editUserButton: Button = view.findViewById(R.id.btnSaveUserInfo)
        val cancelEditButton: Button =  view.findViewById(R.id.btnCancelEditUser)
        val editPrimaryUserSwitch: SwitchCompat =  view.findViewById(R.id.switchEditPrimaryUser)


        editUserButton.setOnClickListener {
            // set values of text fields to variables
            val userName = userNameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            // Check that all fields are filled
            if (userName.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                val result = databaseManager.addUser(userName, email, password)
                if (editPrimaryUserSwitch.isChecked) {
                    // If switch is selected set user as a primary user
                    databaseManager.updateUserAsAdmin(result)
                }
                if (result != -1L) {
                    // If user was added successfully navigate to main screen and show toast
                    Toast.makeText(requireContext(), "User information updated!", Toast.LENGTH_SHORT).show()
                    dismiss()
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
        cancelEditButton.setOnClickListener {
            // close edit fragment if button is pressed
            dismiss()
        }

        return builder.create()
    }

}