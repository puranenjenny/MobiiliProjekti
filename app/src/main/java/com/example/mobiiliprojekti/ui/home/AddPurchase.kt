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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date

interface AddPurchaseDialogListener {
    fun onDialogDismissed()
}
class AddPurchase(private var homeFragment: HomeFragment) : DialogFragment() {

    private lateinit var etPrice: EditText
    private lateinit var etName: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnDate: Button
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var databaseManager: DatabaseManager
    private var listener: AddPurchaseDialogListener? = null

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

        spinnerCategory.dropDownVerticalOffset = 110
        spinnerCategory.dropDownHorizontalOffset = 30

        btnCancel.backgroundTintList = context?.let { ContextCompat.getColorStateList(it, R.color.button) }

        databaseManager = DatabaseManager(requireContext())

        listener = homeFragment

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

        val selectedMonth = date.substring(5,7).toInt()
        val selectedYear = date.substring(0, 4).toInt()

        val month = LocalDate.now().monthValue
        val year = android.icu.util.Calendar.getInstance().get(android.icu.util.Calendar.YEAR)

        val goalDate = databaseManager.getTreatDate()
        println("date1: $goalDate")
        println("date2: $date")
        var goalDateTime : LocalDate? = null
        var selectedDateTime2 : LocalDate? = null
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        if (goalDate != null){
            goalDateTime = LocalDate.parse(goalDate, formatter)
            selectedDateTime2 = LocalDate.parse("$date 23:59:59", formatter)
        }


        Log.d("AddPurchaseFragment", "Name: $name")
        Log.d("AddPurchaseFragment", "Price: $price")
        Log.d("AddPurchaseFragment", "Category: $category")
        Log.d("AddPurchaseFragment", "Date: $date")
        Log.d("AddPurchaseFragment", "User ID: $userId")

        if (name.isNotEmpty() && price != null && date.isNotEmpty() && userId != -1L) {
            val result = databaseManager.addPurchase(name, price, category, date, userId)
            if (goalDateTime != null) {
                if (selectedYear < year && goalDateTime.isBefore(selectedDateTime2)|| selectedYear == year && selectedMonth < month && goalDateTime.isBefore(selectedDateTime2)){
                    updateSavings(price)
                }
            }
            if (result != -1L) {
                    Log.d("AddPurchaseFragment", "Purchase saved successfully with ID: $result")
                    Toast.makeText(
                        requireContext(),
                        "Purchase saved successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    dismiss()
            } else {
                if (name.isEmpty()) {
                    Log.e("AddPurchaseFragment", "Purchase name is empty.")
                    Toast.makeText(
                        requireContext(),
                        "Please enter the purchase name!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                if (date.isEmpty()) {
                    Log.e("AddPurchaseFragment", "Date is empty.")
                    Toast.makeText(requireContext(), "Please select a date!", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    //for passing info to homeFragment about dismissing this dialog
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        listener?.onDialogDismissed()
    }

    //for updating savings in db if purchase is added after month has changed
    private fun updateSavings(price: Double){

        val (savingsId, savingsValue, savingsDate) = databaseManager.getSavings()

        if (savingsId != null && savingsValue != 0.0) {
            val saved = savingsValue?.minus(price)
            if (saved != null) {
                databaseManager.updateSavings(savingsId, saved)
            }
        }
    }

}
