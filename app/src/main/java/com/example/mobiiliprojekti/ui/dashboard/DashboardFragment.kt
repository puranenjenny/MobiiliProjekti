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
import androidx.core.content.ContextCompat
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
        spinner.dropDownVerticalOffset = 150
        spinner.dropDownHorizontalOffset = 40

        val categories = databaseManager.allCategories() //get categories
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

        updateEverything() //initial display setup

        return root
    }


    //updates all displays
    private fun updateEverything(){
        activity?.runOnUiThread {
            displayMoneySpent()
            displayMoneyLeft()
            updateMonthYearDisplay()
            updateDaysLeftDisplay()
            displayLastPaymenttxt()
            displayLastPurchases()
            displayBudgetProgressItems()
            selectedMonthsCategoryBudgetByCategory(SelectedCategoryHandler.getSelectedCategory())
            }
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
        updateEverything()
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

    //money spent
    private fun displayMoneySpent() {
        println("DashboardFragment, displayMoneySpent called")
        val userId = SessionManager.getLoggedInUserId()
        val selectedMonth = currentMonthIndex
        val selectedYear = currentYear
        val categoryName = SelectedCategoryHandler.getSelectedCategory()

        println("Selected month and year: $selectedMonth and $selectedYear")

        val totalMoneySpent = databaseManager.getTotalExpenses(userId, categoryName, selectedMonth, selectedYear)

        println("Total money spent: $totalMoneySpent")
        binding.txtMoneySpentDashboard.text = String.format("%.1f €", totalMoneySpent)
    }

    //money left
    private fun displayMoneyLeft() {
        println("DashboardFragment, displayMoneyLeft called")
        val userId = SessionManager.getLoggedInUserId()
        val selectedCategory = SelectedCategoryHandler.getSelectedCategory()
        val selectedMonth = currentMonthIndex
        val selectedYear = currentYear

        val monthNow = LocalDate.now().monthValue
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        val categoryBudget: Double = if (selectedYear > currentYear || (selectedYear == currentYear && selectedMonth > monthNow)) {
            val budget = databaseManager.getSelectedMonthsCategoryBudget(userId, selectedCategory, selectedMonth, selectedYear) ?: 0
            if (budget != 0)
                (databaseManager.getSelectedMonthsCategoryBudget(userId, selectedCategory, selectedMonth, selectedYear) ?: 0.0).toDouble()
            else{
                (databaseManager.getSelectedMonthsCategoryBudget(userId, selectedCategory, monthNow, currentYear) ?: 0.0).toDouble()
            }
        } else {
            //if displayed month is current month or in past shows specific budget for that month or 0 if budget doesn't exist
            (databaseManager.getSelectedMonthsCategoryBudget(userId, selectedCategory, selectedMonth, selectedYear) ?: 0.0).toDouble()
        }

        val totalExpenses = databaseManager.getTotalExpenses(userId, selectedCategory, selectedMonth, selectedYear)

        println("Total Expensies are: $totalExpenses")

        val moneyLeft = categoryBudget - totalExpenses

        println("Moneyleft $moneyLeft = $categoryBudget - $totalExpenses")

        binding.txtMoneyLeftDashboard.text = String.format("%.1f €", moneyLeft)

        if (moneyLeft < 0) {
            binding.txtMoneyLeftBackDash.setBackgroundResource(R.drawable.element_background_red)
        } else {
            binding.txtMoneyLeftBackDash.setBackgroundResource(R.drawable.element_background_orange)
        }
    }


    //dropdown spinner
    private fun setupCategorySpinner() {
        val categories = databaseManager.allCategoryNames()
        val adapter = ArrayAdapter(requireContext(), R.layout.custom_spinner_item, categories)
        binding.spinnerCategory.adapter = adapter

        // listenerr to update everything when a new category is selected
        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCategory = parent.getItemAtPosition(position).toString()
                SelectedCategoryHandler.setSelectedCategory(selectedCategory)
                binding.txtlastPayCategory.text = "Last payments in $selectedCategory"
                updateEverything()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // do nothing
            }
        }
    }

    //percentage bar items
    private fun displayBudgetProgressItems() {
        val userId = SessionManager.getLoggedInUserId()
        val categoryName = SelectedCategoryHandler.getSelectedCategory()
        val selectedMonth = currentMonthIndex
        val selectedYear = currentYear

        val totalBudget = databaseManager.getSelectedMonthsCategoryBudget(userId, categoryName, selectedMonth, selectedYear)?.toDouble() ?: 0.0
        val totalSpent = databaseManager.getTotalExpenses(userId, categoryName, selectedMonth, selectedYear)

        val budgetUsagePercent = if (totalBudget > 0) (totalSpent / totalBudget * 100).toInt() else 0
        val budgetRemainingPercent = if (budgetUsagePercent <= 100) 100 - budgetUsagePercent else 0

        println("Budget usage percent is $budgetUsagePercent")
        println("Budget remaining percent is $budgetRemainingPercent")

        val textViewProgress = binding.txtHomeProgressBudget //update progress text
        textViewProgress.text = "$budgetRemainingPercent%"

        val progressBar = binding.progressBarCategoryBudget

        if (budgetUsagePercent > 100) {
            progressBar.progressTintList = ContextCompat.getColorStateList(requireContext(), R.color.cancel) //change bar color to red
            progressBar.progress = progressBar.max //update to max
        } else {
            progressBar.progress = budgetUsagePercent //update progress bar to the right percent
        }

        progressBar.max = totalBudget.toInt()
        progressBar.progress = totalSpent.toInt()
    }


    //payments items
    private fun displayLastPaymenttxt() {
        val category = SelectedCategoryHandler.getSelectedCategory()
        selectedMonthsCategoryBudgetByCategory(category)
        binding.txtlastPayCategory.text = "Last payments in $category"
    }

    private fun displayLastPurchases(){
        val userId = SessionManager.getLoggedInUserId()
        val categoryName = SelectedCategoryHandler.getSelectedCategory()
        val selectedMonth = currentMonthIndex
        val selectedYear = currentYear
        val purchases = databaseManager.getSelectedMonthsAndCategoriesPurchases(userId, categoryName, selectedMonth, selectedYear)
        val purchasesLayout = binding.linLastpaymentsDash
        purchasesLayout.removeAllViews()

        if (purchases.isEmpty()) {
            val noPurchasesView = TextView(context).apply {
                text = "No purchases yet"
                textSize = 18f
                setPadding(20, 20, 20, 20)
            }
            purchasesLayout.addView(noPurchasesView)
        } else {
            purchases.forEach { purchase ->
                val purchaseView = TextView(context).apply {
                    text = "${purchase.date} - ${purchase.name} - ${purchase.price} €"
                    textSize = 18f
                    setPadding(20, 20, 20, 20)
                }
                purchasesLayout.addView(purchaseView)
            }
        }
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