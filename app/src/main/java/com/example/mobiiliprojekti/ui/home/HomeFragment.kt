package com.example.mobiiliprojekti.ui.home

import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.mobiiliprojekti.ChangeMonthlyBudgetDialogListener
import com.example.mobiiliprojekti.ChangeMonthlyBudgetFragment
import com.example.mobiiliprojekti.R
import com.example.mobiiliprojekti.databinding.FragmentHomeBinding
import com.example.mobiiliprojekti.services.BudgetHandler
import com.example.mobiiliprojekti.services.DatabaseManager
import com.example.mobiiliprojekti.services.Purchase
import com.example.mobiiliprojekti.services.SessionManager
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale


class HomeFragment : Fragment(), AddPurchaseDialogListener, EditPurchaseDialogListener, ChangeMonthlyBudgetDialogListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseManager: DatabaseManager

    private var currentMonthIndex: Int = LocalDate.now().monthValue
    private var currentYear: Int = LocalDate.now().year

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        databaseManager = DatabaseManager(requireContext())

        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        updateMonthYearDisplay() // initial month and year
        updateDaysLeftDisplay() // setting days left

        // click listeners for month navigation
        binding.btnMonthBack.setOnClickListener {
            navigateMonths(-1)
        }
        binding.btnMonthForward.setOnClickListener {
            navigateMonths(1)
        }

        binding.btnAddNew.setOnClickListener {
            val addPurchaseDialog = AddPurchase(this)
            addPurchaseDialog.show(childFragmentManager, "AddPurchaseDialog")
        }

        binding.linlaybudget.setOnClickListener {
            val changeMonthlyBudgetDialog = ChangeMonthlyBudgetFragment(this)
            changeMonthlyBudgetDialog.show(childFragmentManager, "changeMonthlyBudgetDialog")
        }

        displayLastPurchases()
        displayMoneySpent()
        displayMoneyLeft()
        displayMonthlyBudget()

        // Set up FragmentResultListener
        parentFragmentManager.setFragmentResultListener("changeBudgetRequestKey", viewLifecycleOwner) { requestKey, bundle ->
            val newBudgetValue = bundle.getInt("newBudgetValue")
            println("new budget: $newBudgetValue")
            println("FragmentResultListener received new budget: $newBudgetValue") // Debug-tuloste
            handleNewBudget(newBudgetValue)
        }

        return root
    }

// this months last purchases
private fun displayLastPurchases() {
    val userId = SessionManager.getLoggedInUserId()
    val selectedMonth = currentMonthIndex
    val selectedYear = currentYear
    val purchases = databaseManager.getLastPurchases(userId, selectedYear, selectedMonth)
    val purchasesLayout = binding.linLastpaymentsHome
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
                text = "${purchase.date}: ${purchase.category} - ${purchase.name} - ${purchase.price} €"
                textSize = 18f
                setPadding(20, 20, 20, 20)
                isClickable = true
                setBackgroundResource(R.drawable.ripple_effect)
                setOnClickListener {
                    showEditPurchaseDialog(purchase)
                }
            }
            purchasesLayout.addView(purchaseView)
        }
    }
}


    private fun showEditPurchaseDialog(purchase: Purchase) {
        val editPurchaseDialog = EditPurchase(purchase)
        editPurchaseDialog.show(parentFragmentManager, "editPurchaseDialog")
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
        updateProgressBar()
        displayMoneySpent()
        displayMoneyLeft()
        displayLastPurchases()
        displayMonthlyBudget()
        
    }
    private fun updateMonthYearDisplay() {
        val monthName = LocalDate.of(currentYear, currentMonthIndex, 1).month.getDisplayName(
            TextStyle.FULL, Locale.getDefault())
        binding.txtMonthYearHome.text = "$monthName $currentYear"
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

        binding.txtDaysleft.text = if (daysLeft > 0) "$daysLeft / $totalDaysInMonth" else "0"
    }


    //money spent
    private fun displayMoneySpent() {
        val userId = SessionManager.getLoggedInUserId()
        val selectedMonth = currentMonthIndex
        val selectedYear = currentYear

        println("$selectedMonth ja $selectedYear")

        val values = databaseManager.getSelectedMonthsPurchases(userId, selectedMonth, selectedYear)

        println("$values")
        val totalMoneySpent = values.sum()

        binding.txtMoneyspent.text = "${String.format("%.1f", totalMoneySpent)} €"
    }

    //money left
    private fun displayMoneyLeft() {
        val userId = SessionManager.getLoggedInUserId()
        val monthlyBudget: Double
        val selectedMonth = currentMonthIndex
        val selectedYear = currentYear

        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // TODO: Just testing. Delete when ready.
        databaseManager.getSelectedMonthsCategoryBudget(userId, "Food", selectedMonth, selectedYear)

        monthlyBudget = if (selectedYear > currentYear || (selectedYear == currentYear && selectedMonth > currentMonth)) {
            val budget = databaseManager.getSelectedMonthsBudget(userId, selectedMonth, selectedYear) ?: 0
            if (budget != 0)
            (databaseManager.getSelectedMonthsBudget(userId, selectedMonth, selectedYear) ?: 0.0).toDouble()
            else{
                val (monthlyBudgetValue, _) = databaseManager.fetchMonthlyBudget(userId)
                monthlyBudgetValue?.toDouble() ?: 0.0
            }
        } else {
            //if displayed month is current month or in past shows specific budget for that month or 0 if budget doesn't exist
            (databaseManager.getSelectedMonthsBudget(userId, selectedMonth, selectedYear) ?: 0.0).toDouble()
        }


        val homeExpenses = databaseManager.getSelectedMonthsPurchases(userId, selectedMonth, selectedYear).sum()

        val moneyLeft = monthlyBudget - homeExpenses
        binding.txtMoneyLeftHome.text = "${String.format("%.1f", moneyLeft)} €"

        // change the background color of money left element
        if (moneyLeft < 0) {
            binding.moneyleftBackground.setBackgroundResource(R.drawable.element_background_red)
        } else {
            binding.moneyleftBackground.setBackgroundResource(R.drawable.element_background_orange)
        }
    }

    private fun displayMonthlyBudget() {
        val userId = SessionManager.getLoggedInUserId()
        val monthlyBudget: Double
        val selectedMonth = currentMonthIndex
        val selectedYear = currentYear

        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)



        monthlyBudget = if (selectedYear > currentYear || (selectedYear == currentYear && selectedMonth > currentMonth)) {
            val budget = databaseManager.getSelectedMonthsBudget(userId, selectedMonth, selectedYear) ?: 0
            if (budget != 0)
                (databaseManager.getSelectedMonthsBudget(userId, selectedMonth, selectedYear) ?: 0.0).toDouble()
            else{
                val (monthlyBudgetValue, _) = databaseManager.fetchMonthlyBudget(userId)
                monthlyBudgetValue?.toDouble() ?: 0.0
            }
        } else {
            //if displayed month is current month or in past shows specific budget for that month or 0 if budget doesn't exist
            (databaseManager.getSelectedMonthsBudget(userId, selectedMonth, selectedYear) ?: 0.0).toDouble()

        }


        binding.txtMonthlyBudetNum.text = "${String.format("%.0f", monthlyBudget)} €"

    }


    //treat meterin setit

    private fun fetchMonthlyBudgetsFromDatabase(): List<Float> {
        val monthlyBudgets = mutableListOf<Float>()

        // Perform database query using DatabaseManager
        val cursor = databaseManager.readableDatabase.rawQuery(
            "SELECT month_budget FROM monthly_budget WHERE strftime('%Y-%m', date) = strftime('%Y-%m', 'now') GROUP BY strftime('%Y-%m', date)",
            null
        )
        try {
            if (cursor != null && cursor.count > 0) {
                val budgetIndex = cursor.getColumnIndex("month_budget")
                if (budgetIndex != -1) {
                    cursor.moveToFirst()
                    do {
                        val budget = cursor.getFloat(budgetIndex)
                        monthlyBudgets.add(budget)
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return monthlyBudgets
    }

    private fun fetchMonthlyExpensesFromDatabase(): List<Float> {
        val monthlyExpenses = mutableListOf<Float>()

        // Perform database query using DatabaseManager
        val cursor = databaseManager.readableDatabase.rawQuery(
            "SELECT SUM(value) AS monthly_expense FROM purchase WHERE strftime('%Y-%m', date) = strftime('%Y-%m', 'now') GROUP BY strftime('%Y-%m', date)",
            null
        )
        try {
            if (cursor != null && cursor.count > 0) {
                val expenseIndex = cursor.getColumnIndex("monthly_expense")
                if (expenseIndex != -1) {
                    cursor.moveToFirst()
                    do {
                        val expense = cursor.getFloat(expenseIndex)
                        monthlyExpenses.add(expense)
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return monthlyExpenses
    }

    private fun updateProgressBar() {
        val monthlyIncome = fetchMonthlyBudgetsFromDatabase().sum()
        val monthlyExpenses = fetchMonthlyExpensesFromDatabase().sum()

        val progressBar = binding.progressBar2
        val progressText = binding.progressText2


        if (monthlyIncome > 0) {
            val progressPercentage = (monthlyExpenses / monthlyIncome) * 100
            progressBar.progress = progressPercentage.toInt()
            progressText.text = "$progressPercentage%"

        }
    }

    ///when add purchase dialog closes these functions are called to update view
    override fun onDialogDismissed() {
        displayLastPurchases()
        displayMoneySpent()
        displayMoneyLeft()
    }

    override fun onDialogDismissed2() {
        handleNewBudget(BudgetHandler.getMonhtlyBudgetByMonth())
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun handleNewBudget(newBudgetValue: Int) {
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
            databaseManager.changeMonthlyBudgetByMonth(newBudgetValue, futureDate)
        } else if (selectedYear < currentYear || (selectedYear == currentYear && selectedMonth < currentMonth)) {
            println("Past date: $pastDate") // Debug-tuloste
            databaseManager.changeMonthlyBudgetByMonth(newBudgetValue, pastDate)
        } else {
            println("current date: $formattedDate") // Debug-tuloste
            databaseManager.changeMonthlyBudgetByMonth(newBudgetValue, formattedDate)
        }

        displayMonthlyBudget()
        displayMoneySpent()
        displayMoneyLeft()
    }


}