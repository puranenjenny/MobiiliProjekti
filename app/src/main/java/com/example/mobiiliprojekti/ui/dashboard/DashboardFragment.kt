package com.example.mobiiliprojekti.ui.dashboard

import com.example.mobiiliprojekti.R
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.mobiiliprojekti.DatabaseManager
import com.example.mobiiliprojekti.databinding.FragmentDashboardBinding
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // db-manager object
    private lateinit var databaseManager: DatabaseManager

    // month and year variables
    private var currentMonthIndex: Int = LocalDate.now().monthValue
    private var currentYear: Int = LocalDate.now().year

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize DatabaseManager
        databaseManager = DatabaseManager(requireContext())


        val categories = databaseManager.allCategories() //get categories
        //val categorynames = databaseManager.allCategoryNames() // get only names for the spinner
        val textView: TextView = binding.textDashboard //binding

        textView.text = categories // set text
        setupCategorySpinner() // set categories to the dropdown spinner

        updateMonthYearDisplay() // setting initial month and year
        updateDaysLeftDisplay() // setting days left

        // click listeners for month navigation
        binding.btnMonthBackDashboard.setOnClickListener {
            navigateMonths(-1)
        }
        binding.btnMonthForwardDashboard.setOnClickListener {
            navigateMonths(1)
        }

        return root
    }


    // date functions
    private fun navigateMonths(direction: Int) {
        currentMonthIndex += direction
        if (currentMonthIndex < 1) {
            currentMonthIndex = 12
            currentYear -= 1
        } else if (currentMonthIndex > 12) {
            currentMonthIndex = 1
            currentYear += 1
        }
        updateMonthYearDisplay()
        updateDaysLeftDisplay()
    }
    private fun updateMonthYearDisplay() {
        val monthName = LocalDate.of(currentYear, currentMonthIndex, 1).month.getDisplayName(
            TextStyle.FULL, Locale.getDefault())
        binding.txtMonthYearDashboard.text = "$monthName $currentYear"
    }

    private fun updateDaysLeftDisplay() {
        val today = LocalDate.now()
        val yearMonth = YearMonth.of(currentYear, currentMonthIndex)
        val totalDaysInMonth = yearMonth.lengthOfMonth()

        val daysLeft = if (currentYear > today.year || (currentYear == today.year && currentMonthIndex > today.monthValue)) {
            // future month
            totalDaysInMonth
        } else if (currentYear < today.year || (currentYear == today.year && currentMonthIndex < today.monthValue)) {
            // past month
            0
        } else {
            // current month
            totalDaysInMonth - today.dayOfMonth + 1
        }

        binding.txtDaysLeftDashboard.text = if (daysLeft > 0) "$daysLeft / $totalDaysInMonth" else "0"
    }


    //dropdown spinner
    private fun setupCategorySpinner() {
        val categories = databaseManager.allCategoryNames()
        val adapter = ArrayAdapter(requireContext(), R.layout.custom_spinner_item, categories)
        binding.spinnerCategory.adapter = adapter
    }






    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}