import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.mobiiliprojekti.R

class LoginFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.fragment_login, null)

        // set view for dialog
        builder.setView(view)

        // get "Cancel" -button and set onClickListener
        view.findViewById<Button>(R.id.btn_cancel_login)?.setOnClickListener {
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
