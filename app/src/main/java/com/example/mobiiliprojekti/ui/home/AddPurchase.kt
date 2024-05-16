package com.example.mobiiliprojekti.ui.home

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
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
import java.util.Calendar


class AddPurchase : DialogFragment() {

    private lateinit var etPrice: EditText
    private lateinit var etName: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnDate: Button
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var databaseManager: DatabaseManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_purchase, container, false)

        super.onViewCreated(view, savedInstanceState)

        etPrice = view.findViewById(R.id.purchase_price)
        etName = view.findViewById(R.id.purchase_name)
        spinnerCategory = view.findViewById(R.id.spinner_category)
        btnDate = view.findViewById(R.id.btn_date)
        btnSave = view.findViewById(R.id.btn_save)
        btnCancel = view.findViewById(R.id.btn_cancel)

       // btnDate.backgroundTintList = context?.let { ContextCompat.getColorStateList(it, R.color.button) }
        btnCancel.backgroundTintList = context?.let { ContextCompat.getColorStateList(it, R.color.button) }

        databaseManager = DatabaseManager(requireContext())

        setupCategorySpinner()
        setupDateButton()


        btnSave.setOnClickListener {
            savePurchase()
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


    private fun savePurchase() {
        val name = etName.text.toString()
        val price = etPrice.text.toString().toDoubleOrNull()
        val category = spinnerCategory.selectedItem.toString()
        val date = btnDate.text.toString()
        val userId = SessionManager.getLoggedInUserId()

        if (name.isNotEmpty() && price != null && date.isNotEmpty() && userId != -1L) {
            val result = databaseManager.addPurchase(name, price, category, date, userId)
            activity?.runOnUiThread {
                if (result != -1L) {
                    Toast.makeText(requireContext(), "Purchase saved successfully!", Toast.LENGTH_SHORT).show()
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), "Error saving purchase.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            activity?.runOnUiThread {
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter the purchase name!", Toast.LENGTH_SHORT).show()
                }
                if (price == null) {
                    Toast.makeText(requireContext(), "Please enter a valid price!", Toast.LENGTH_SHORT).show()
                }
                if (date.isEmpty()) {
                    Toast.makeText(requireContext(), "Please select a date!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}
