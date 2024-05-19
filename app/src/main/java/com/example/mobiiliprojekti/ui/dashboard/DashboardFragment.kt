package com.example.mobiiliprojekti.ui.dashboard

import android.icu.util.Calendar
import com.example.mobiiliprojekti.R

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mobiiliprojekti.ChangeCategoryBudgetDialogListener
import com.example.mobiiliprojekti.ChangeCategoryBudgetFragment
import com.example.mobiiliprojekti.services.DatabaseManager
import com.example.mobiiliprojekti.databinding.FragmentDashboardBinding
import com.example.mobiiliprojekti.services.CategoryBudgetHandler
import com.example.mobiiliprojekti.services.SelectedCategoryHandler
import com.example.mobiiliprojekti.services.SessionManager
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class DashboardFragment : Fragment(), ChangeCategoryBudgetDialogListener {

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

        val spinner: Spinner = root.findViewById(R.id.spinner_category)
        spinner.dropDownVerticalOffset = 150 // Set vertical offset to zero or adjust as needed
        spinner.dropDownHorizontalOffset = 40 // Set horizontal offset to zero or adjust as needed

        val categories = databaseManager.allCategories() //get categories
        //val categorynames = databaseManager.allCategoryNames() // get only names for the spinner
        val textView: TextView = binding.textDashboard //binding

        textView.text = categories // set text
        setupCategorySpinner() // set categories to the dropdown spinner

        updateMonthYearDisplay() // setting initial month and year
        updateDaysLeftDisplay() // setting days left

        var selectedCategoryString = spinner.selectedItem?.toString() ?: ""
        SelectedCategoryHandler.setSelectedCategory(selectedCategoryString)

        selectedMonthsCategoryBudgetByCategory(SelectedCategoryHandler.getSelectedCategory())

        // save spinner selection to variable and to SelectedCategoryHandler
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCategory = parent.getItemAtPosition(position).toString()
                selectedCategoryString = selectedCategory
                SelectedCategoryHandler.setSelectedCategory(selectedCategoryString)
                selectedMonthsCategoryBudgetByCategory(selectedCategoryString)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // No-op
            }
        }

        // click listeners for month navigation
        binding.btnMonthBackDashboard.setOnClickListener {
            navigateMonths(-1)
        }
        binding.btnMonthForwardDashboard.setOnClickListener {
            navigateMonths(1)
        }

        binding.linlaymonthly.setOnClickListener {
            val changeCategoryBudgetDialog = ChangeCategoryBudgetFragment(this)
            changeCategoryBudgetDialog.show(childFragmentManager, "changeCategoryBudgetDialog")
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
        selectedMonthsCategoryBudgetByCategory(SelectedCategoryHandler.getSelectedCategory())
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

    fun selectedMonthsCategoryBudgetByCategory(categoryName: String) {
        val userId = SessionManager.getLoggedInUserId()
        val monthlyBudget: Double
        val selectedMonth = currentMonthIndex
        val selectedYear = currentYear

        val monthNow = LocalDate.now().monthValue
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)



        monthlyBudget = if (selectedYear > currentYear || (selectedYear == currentYear && selectedMonth > monthNow)) {
            val budget = databaseManager.getSelectedMonthsCategoryBudget(userId, categoryName, selectedMonth, selectedYear) ?: 0
            if (budget != 0)
                (databaseManager.getSelectedMonthsCategoryBudget(userId, categoryName, selectedMonth, selectedYear) ?: 0.0).toDouble()
            else{
                (databaseManager.getSelectedMonthsCategoryBudget(userId, categoryName, monthNow, currentYear) ?: 0.0).toDouble()
            }
        } else {
            //if displayed month is current month or in past shows specific budget for that month or 0 if budget doesn't exist
            (databaseManager.getSelectedMonthsCategoryBudget(userId, categoryName, selectedMonth, selectedYear) ?: 0.0).toDouble()

        }


        binding.txtMonthlyBudetNum.text = "${String.format("%.0f", monthlyBudget)} €"
    }

    override fun onDialogDismissed3() {
        val selectedCategoryString = SelectedCategoryHandler.getSelectedCategory()
        handleNewCategoryBudget(CategoryBudgetHandler.getMonthlyCategoryBudgetByMonth(), selectedCategoryString)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun handleNewCategoryBudget(newBudgetValue: Int, category: String) {
        val selectedMonth = currentMonthIndex
        val selectedYear = currentYear
        println("month: $selectedMonth") // Debug-tuloste
        println("year: $selectedYear") // Debug-tuloste

        val currentDateTime = java.time.LocalDateTime.now()


        val yearMonth = YearMonth.of(selectedYear, selectedMonth)
        val totalDaysInMonth = yearMonth.lengthOfMonth()

        val futureDate : String?
        val pastDate : String?

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedDate = currentDateTime.format(formatter)

        val formatterYear = DateTimeFormatter.ofPattern("yyyy")
        val currentYear = currentDateTime.format(formatterYear).toInt()

        val formatterMonth = DateTimeFormatter.ofPattern("MM")
        val currentMonth = currentDateTime.format(formatterMonth).toInt()



        if (selectedMonth < 10){
            futureDate = "$selectedYear-0$selectedMonth-01 00:00:01"
            pastDate = "$selectedYear-0$selectedMonth-$totalDaysInMonth 00:00:01"
        }
        else {
            futureDate = "$selectedYear-$selectedMonth-01 00:00:01"
            pastDate = "$selectedYear-$selectedMonth-$totalDaysInMonth 23:59:59"
        }

        println("Handling new budget: $newBudgetValue") // Debug-tuloste

        if (selectedYear > currentYear || (selectedYear == currentYear && selectedMonth > currentMonth)) {
            println("Future date: $futureDate + $selectedMonth vs. $currentMonth") // Debug-tuloste
            databaseManager.changeCategoryBudgetByMonth(newBudgetValue, futureDate, category)
        } else if (selectedYear < currentYear || (selectedYear == currentYear && selectedMonth < currentMonth)) {
            println("Past date: $pastDate") // Debug-tuloste
            databaseManager.changeCategoryBudgetByMonth(newBudgetValue, pastDate, category)
        } else {
            println("current date: $formattedDate") // Debug-tuloste
            databaseManager.changeCategoryBudgetByMonth(newBudgetValue, formattedDate, category)
        }

        val category = SelectedCategoryHandler.getSelectedCategory()
            selectedMonthsCategoryBudgetByCategory(category)
    }
}