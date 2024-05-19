package com.example.mobiiliprojekti

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.mobiiliprojekti.services.BudgetHandler
import com.example.mobiiliprojekti.ui.home.HomeFragment

interface ChangeMonthlyBudgetDialogListener {
    fun onDialogDismissed2()
}
class ChangeMonthlyBudgetFragment(private var homeFragment: HomeFragment) : DialogFragment() {

    private lateinit var newBudget: EditText
    private lateinit var btnSaveBudget: Button
    private lateinit var btnCancelBudget: Button
    private var newBudgetValue: Int? = null
    private var listener: ChangeMonthlyBudgetDialogListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_change_monthly_budget, container, false)

        newBudget = view.findViewById(R.id.newBudgetValueInput)
        btnSaveBudget = view.findViewById(R.id.btn_saveNewBudget)
        btnCancelBudget = view.findViewById(R.id.btn_cancelNewBudget)
        //listener = homeFragment
        listener = homeFragment

        btnCancelBudget.backgroundTintList = context?.let { ContextCompat.getColorStateList(it, R.color.button) }

        btnSaveBudget.setOnClickListener {
            if (newBudget.editableText.isEmpty()) {
                Toast.makeText(requireContext(), "Input new budget!", Toast.LENGTH_SHORT).show()
            } else {
                newBudgetValue = newBudget.text.toString().toInt()
                BudgetHandler.setMonthlyBudgetByMonth(newBudgetValue!!)
                dismiss()
            }
        }

        btnCancelBudget.setOnClickListener {
            dismiss()
        }

        return view
    }

    override fun onStart() { //makes the background transparent
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        listener?.onDialogDismissed2()
    }
}
