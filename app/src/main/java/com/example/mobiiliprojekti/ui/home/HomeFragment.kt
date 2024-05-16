package com.example.mobiiliprojekti.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.mobiiliprojekti.databinding.FragmentHomeBinding
import com.example.mobiiliprojekti.services.DatabaseManager
import com.example.mobiiliprojekti.services.SessionManager
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var databaseManager: DatabaseManager

    private var currentMonthIndex: Int = LocalDate.now().monthValue
    private var currentYear: Int = LocalDate.now().year

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

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
            AddPurchase().show(childFragmentManager, "AddPurchaseDialog")
        }

        displayLastPurchases()
        displayMoneySpent()
        displayMoneyLeft()

        return root
    }

// this months last purchases
    fun displayLastPurchases() {
        val userId = SessionManager.getLoggedInUserId()
        val selectedMonth = currentMonthIndex
        val selectedYear = currentYear
        val purchases = databaseManager.getLastPurchases(userId, selectedYear, selectedMonth )
        val purchasesLayout = binding.linLastpaymentsHome
        purchasesLayout.removeAllViews()

        if (purchases.isEmpty()) {
            val noPurchasesView = TextView(context).apply {
                text = "No purchases yet"
                textSize = 16f
                setPadding(12, 12, 12, 12)
            }
            purchasesLayout.addView(noPurchasesView)
        } else {
            purchases.forEach { purchase ->
                val purchaseView = TextView(context).apply {
                    text = "${purchase.date}: ${purchase.name} - ${purchase.value} €"
                    textSize = 16f
                    setPadding(12, 12, 12, 12)
                }
                purchasesLayout.addView(purchaseView)
            }
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
        updateMonthYearDisplay()
        updateDaysLeftDisplay()
        updateProgressBar()
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
        var totalMoneySpent = values.sum()

        binding.txtMoneyspent.text = "${totalMoneySpent} €"
    }

//money left
private fun displayMoneyLeft() {
    val userId = SessionManager.getLoggedInUserId()
    val selectedMonth = currentMonthIndex
    val selectedYear = currentYear

    val monthlyBudget = databaseManager.fetchMonthlyBudget(userId) ?: 0.0
    val homeExpenses = databaseManager.getSelectedMonthsPurchases(userId, selectedMonth, selectedYear).sum()

    val moneyLeft = monthlyBudget.toDouble() - homeExpenses
    binding.txtMoneyLeftHome.text = "${String.format("%.2f", moneyLeft)} €"
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}