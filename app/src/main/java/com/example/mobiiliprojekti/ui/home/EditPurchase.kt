package com.example.mobiiliprojekti.ui.home

import android.app.DatePickerDialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

        spinnerCategory.dropDownVerticalOffset = 110 // Set vertical offset to zero or adjust as needed
        spinnerCategory.dropDownHorizontalOffset = 30 // Set horizontal offset to zero or adjust as needed

        databaseManager = DatabaseManager(requireContext())



        etName.setText(purchase.name)
        etPrice.setText(purchase.price.toString())
        setupCategorySpinner(purchase.category)
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

    private fun setupCategorySpinner(currentCategoryId: Int) {
        val categoryNames = databaseManager.allCategoryNames()

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        val currentCategoryName = databaseManager.getCategoryNameById(currentCategoryId)
        val categoryPosition = adapter.getPosition(currentCategoryName)
        spinnerCategory.setSelection(categoryPosition)
    }


    private fun setupDateButton() {
        btnDate.text = purchase.date

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val purchaseDate = dateFormat.parse(purchase.date)
        val calendar = Calendar.getInstance().apply {
            time = purchaseDate ?: Date() // fallback to current date if parsing fails
        }

        btnDate.setOnClickListener {
            DatePickerDialog(requireContext(), //update the selected date when picked
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedDate = Calendar.getInstance().apply {
                        set(selectedYear, selectedMonth, selectedDay)
                    }
                    btnDate.text = dateFormat.format(selectedDate.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
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
