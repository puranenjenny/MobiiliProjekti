import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.mobiiliprojekti.R

class RegisterFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.fragment_register, null)

        // set view for dialog
        builder.setView(view)

        // get "Cancel" -button and set onClickListener
        view.findViewById<Button>(R.id.btn_cancel)?.setOnClickListener {
            // close register-fragment
            dismiss()
        }

        return builder.create()
    }
}