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
import com.example.mobiiliprojekti.services.CategoryBudgetHandler
import com.example.mobiiliprojekti.ui.dashboard.DashboardFragment

interface ChangeCategoryBudgetDialogListener {
    fun onDialogDismissed3()
}
class ChangeCategoryBudgetFragment(private var dashboardFragment: DashboardFragment) : DialogFragment() {

    private lateinit var newCategoryBudget: EditText
    private lateinit var btnSaveCategoryBudget: Button
    private lateinit var btnCancelCategoryBudget: Button
    private var newCategoryBudgetValue: Int? = null
    private var listener: ChangeCategoryBudgetDialogListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_change_category_budget, container, false)

        newCategoryBudget = view.findViewById(R.id.newCategoryBudgetValueInput)
        btnSaveCategoryBudget = view.findViewById(R.id.btn_saveNewCategoryBudget)
        btnCancelCategoryBudget = view.findViewById(R.id.btn_cancelNewCategoryBudget)

        listener = dashboardFragment

        btnCancelCategoryBudget.backgroundTintList = context?.let { ContextCompat.getColorStateList(it, R.color.button) }

        btnSaveCategoryBudget.setOnClickListener {
            if (newCategoryBudget.editableText.isEmpty()) {
                Toast.makeText(requireContext(), "Input new budget!", Toast.LENGTH_SHORT).show()
            } else {
                newCategoryBudgetValue = newCategoryBudget.text.toString().toInt()
                CategoryBudgetHandler.setCategoryBudgetByMonth(newCategoryBudgetValue!!)
                dismiss()
            }
        }

        btnCancelCategoryBudget.setOnClickListener {
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
        listener?.onDialogDismissed3()
    }
}
