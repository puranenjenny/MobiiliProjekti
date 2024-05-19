package com.example.mobiiliprojekti.ui.home

import android.app.DatePickerDialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.mobiiliprojekti.R
import com.example.mobiiliprojekti.services.DatabaseManager
import com.example.mobiiliprojekti.services.SessionManager
import com.example.mobiiliprojekti.services.Purchase
import java.util.Calendar

interface EditPurchaseDialogListener {
    fun onDialogDismissed()
}

class EditPurchase(private val purchase: Purchase, private val listener: EditPurchaseDialogListener) : DialogFragment() {

    private lateinit var homeFragment: HomeFragment

    private lateinit var etName: EditText
    private lateinit var etPrice: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnDate: Button
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var databaseManager: DatabaseManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_purchase, container, false)

        super.onViewCreated(view, savedInstanceState)

        etName = view.findViewById(R.id.edit_purchase_name)
        etPrice = view.findViewById(R.id.edit_purchase_price)
        spinnerCategory = view.findViewById(R.id.edit_spinner_category)
        btnDate = view.findViewById(R.id.edit_btn_date)
        btnSave = view.findViewById(R.id.edit_btn_save)
        btnCancel = view.findViewById(R.id.edit_btn_cancel)

        btnCancel.backgroundTintList = context?.let { ContextCompat.getColorStateList(it, R.color.button) }

        databaseManager = DatabaseManager(requireContext())



        etName.setText(purchase.name)
        etPrice.setText(purchase.price.toString())
        setupCategorySpinner()
        setupDateButton()

        btnSave.setOnClickListener {
            saveUpdatedPurchase()
        }

        btnCancel.setOnClickListener {
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

    private fun setupCategorySpinner() {
        val categories = databaseManager.allCategoryNames()
        if (categories.isEmpty()) {
            Log.e("SetupCategorySpinner", "No categories found in database")
            Toast.makeText(requireContext(), "No categories found!", Toast.LENGTH_SHORT).show()
        }

        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
    }


    private fun setupDateButton() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        btnDate.text = String.format("%d-%02d-%02d", year, month + 1, day)

        btnDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    btnDate.text = String.format("%d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                }, year, month, day
            )
            datePickerDialog.show()
        }
    }

    private fun saveUpdatedPurchase() {
        val name = etName.text.toString().trim()
        val price = etPrice.text.toString().toDoubleOrNull()
        val category = spinnerCategory.selectedItem.toString()
        val date = btnDate.text.toString()
        val userId = SessionManager.getLoggedInUserId()

        if (name.isEmpty() || price == null || userId == -1L) {
            Toast.makeText(context, "Please ensure all fields are correctly filled.", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedPurchase = Purchase(
            purchaseId = purchase.purchaseId,
            name = name,
            price = price,
            category = getCategoryID(category),
            date = date,
            userId = userId
        )

        val isSuccess = databaseManager.updatePurchase(updatedPurchase)
        if (isSuccess > 0) {
            Toast.makeText(context, "Purchase updated successfully!", Toast.LENGTH_SHORT).show()
            dismiss()
        } else {
            Toast.makeText(context, "Failed to update purchase.", Toast.LENGTH_SHORT).show()
        }
    }

    //get category id for the save updated purchase function
    private fun getCategoryID(categoryName: String): Int {
        return databaseManager.fetchCategoryBudgetIdByNameforPurchase(categoryName)
    }

    //for passing info to homeFragment about dismissing this dialog
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        listener?.onDialogDismissed()
    }
}
