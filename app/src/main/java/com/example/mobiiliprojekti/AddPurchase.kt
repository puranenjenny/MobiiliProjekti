package com.example.mobiiliprojekti.ui.addpurchase

import android.app.DatePickerDialog
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
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.example.mobiiliprojekti.R
import com.example.mobiiliprojekti.services.DatabaseManager
import com.example.mobiiliprojekti.services.SharedViewModel
import java.util.Calendar

class AddPurchaseFragment : DialogFragment() {

    private lateinit var etPrice: EditText
    private lateinit var etName: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnDate: Button
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var databaseManager: DatabaseManager
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_purchase, container, false)

        etPrice = view.findViewById(R.id.purchase_price)
        etName = view.findViewById(R.id.purchase_name)
        spinnerCategory = view.findViewById(R.id.spinner_category)
        btnDate = view.findViewById(R.id.btn_date)
        btnSave = view.findViewById(R.id.btn_save)
        btnCancel = view.findViewById(R.id.btn_cancel)

        databaseManager = DatabaseManager(requireContext())

        setupCategorySpinner()
        setupDateButton()

        sharedViewModel.userId.observe(viewLifecycleOwner, Observer { userId ->
            Log.d("AddPurchaseFragment", "Observed User ID in AddPurchaseFragment: $userId")
        })

        btnSave.setOnClickListener {
            savePurchase()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        return view
    }

    private fun setupCategorySpinner() {
        val categories = databaseManager.allCategoryNames()
        if (categories.isEmpty()) {
            Log.e("SetupCategorySpinner", "No categories found in database")
            Toast.makeText(requireContext(), "No categories found!", Toast.LENGTH_SHORT).show()
        }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
    }

    private fun setupDateButton() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Set the default date to today's date
        btnDate.text = "$day/${month + 1}/$year"

        btnDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    btnDate.text = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                }, year, month, day)

            datePickerDialog.show()
        }
    }


    private fun savePurchase() {
        val name = etName.text.toString()
        val price = etPrice.text.toString().toDoubleOrNull()
        val category = spinnerCategory.selectedItem.toString()
        val date = btnDate.text.toString()

        Log.d("SavePurchase", "Name: $name")
        Log.d("SavePurchase", "Price: $price")
        Log.d("SavePurchase", "Category: $category")
        Log.d("SavePurchase", "Date: $date")

        val userId = sharedViewModel.userId.value
        Log.d("SavePurchase", "User ID from ViewModel: $userId")

        if (name.isNotEmpty() && price != null && date.isNotEmpty()) {
            if (userId != null) {
                val result = databaseManager.addPurchase(name, price, category, date, userId)
                if (result != -1L) {
                    Log.d("SavePurchase", "Purchase saved successfully with ID: $result")
                    Toast.makeText(requireContext(), "Purchase saved successfully!", Toast.LENGTH_SHORT).show()
                    dismiss()
                } else {
                    Log.e("SavePurchase", "Error saving purchase.")
                    Toast.makeText(requireContext(), "Error saving purchase.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e("SavePurchase", "User ID is null")
                Toast.makeText(requireContext(), "User ID not available!", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (name.isEmpty()) {
                Log.e("SavePurchase", "Purchase name is empty.")
                Toast.makeText(requireContext(), "Please enter the purchase name!", Toast.LENGTH_SHORT).show()
            }
            if (price == null) {
                Log.e("SavePurchase", "Price is null or invalid.")
                Toast.makeText(requireContext(), "Please enter a valid price!", Toast.LENGTH_SHORT).show()
            }
            if (date.isEmpty()) {
                Log.e("SavePurchase", "Date is empty.")
                Toast.makeText(requireContext(), "Please select a date!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
