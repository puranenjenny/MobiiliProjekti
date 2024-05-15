package com.example.mobiiliprojekti

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.mobiiliprojekti.services.DatabaseManager
import com.example.mobiiliprojekti.services.SessionManager

class DeleteUserFragment : DialogFragment() {

    private lateinit var databaseManager: DatabaseManager

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.fragment_delete_user, null)

        // set view for dialog
        builder.setView(view)

        // set up database manager
        databaseManager = DatabaseManager(requireContext())


        val deleteUserButton: Button = view.findViewById(R.id.btnDeleteUser)
        val cancelDeleteButton: Button =  view.findViewById(R.id.btnCancelDeleteUser)



        deleteUserButton.setOnClickListener {
            val userId = SessionManager.getLoggedInUserId()
            println("Delete user_id: $userId")
            if (databaseManager.deleteUserData(userId)) {
                Toast.makeText(requireContext(), "User deleted!", Toast.LENGTH_SHORT).show()
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
            } else {
                Toast.makeText(requireContext(), "Deleting user failed!", Toast.LENGTH_SHORT).show()
            }
        }

        // onClickListener for "Cancel"-button
        cancelDeleteButton.setOnClickListener {
            // close register-fragment if button is pressed
            dismiss()
        }

        return builder.create()
    }

}