package com.example.mobiiliprojekti.ui.annual


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.mobiiliprojekti.R
import com.example.mobiiliprojekti.databinding.FragmentAnnualBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.example.mobiiliprojekti.services.DatabaseManager



class AnnualFragment : Fragment() {

    private var _binding: FragmentAnnualBinding? = null
    private lateinit var lineChart: LineChart
    private lateinit var databaseManager: DatabaseManager

    private val binding get() = _binding!!



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnnualBinding.inflate(inflater, container, false)

        val root: View = binding.root

        // Initialize DatabaseManager
        databaseManager = DatabaseManager(requireContext())

        // Get reference to LineChart
        lineChart = binding.lineChart

        // Fetch monthly budgets from database
        val monthlyBudgets = fetchMonthlyBudgetsFromDatabase()
        val monthlyExpenses = fetchMonthlyExpensesFromDatabase()


        // Create Entry objects for monthly budgets
        val budgetEntries = mutableListOf<Entry>()
        monthlyBudgets.forEachIndexed { index, budget ->
            budgetEntries.add(Entry(index.toFloat(), budget))
        }

        // Create Entry objects for monthly expenses
        val expenseEntries = mutableListOf<Entry>()
        monthlyExpenses.forEachIndexed { index, expense ->
            expenseEntries.add(Entry(index.toFloat(), expense))
        }

        // Create LineDataSet object for budgets
        val budgetDataSet = LineDataSet(budgetEntries, "Monthly Budget")
        val dark_green = ContextCompat.getColor(requireContext(), R.color.dark_green)
        val brown = ContextCompat.getColor(requireContext(), R.color.button)
        budgetDataSet.color = dark_green
        budgetDataSet.setCircleColor(dark_green)
        budgetDataSet.setDrawValues(false)

        // Create LineDataSet object for expenses
        val expenseDataSet = LineDataSet(expenseEntries, "Monthly Expenses")
        expenseDataSet.color = brown
        expenseDataSet.setCircleColor(brown)
        expenseDataSet.setDrawValues(false)

        // Create LineData object containing both budgets and expenses
        val lineData = LineData(budgetDataSet, expenseDataSet)

        // Set LineData to LineChart
        lineChart.data = lineData

        // Customize LineChart as needed
        lineChart.description.isEnabled = false
        lineChart.animateY(1000)

        // Set Y-axis labels (amounts)
        val yAxisLeft = lineChart.axisLeft
        yAxisLeft.granularity = 100f // Aseta välit 100 välein
        yAxisLeft.isGranularityEnabled = true

        val yAxisRight = lineChart.axisRight
        yAxisRight.isEnabled = false // Poista oikeanpuoleinen Y-akseli käytöstä

        val xAxis = lineChart.xAxis
        xAxis.granularity =12f
        xAxis.isGranularityEnabled = true



        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateProgressBar()
    }

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

        val progressBar = binding.progressBar
        val progressText = binding.progressText
        val treatText = binding.Treattext

        if (monthlyIncome > 0) {
            val progressPercentage = (monthlyExpenses / monthlyIncome) * 100
            progressBar.progress = progressPercentage.toInt()
            progressText.text = "$progressPercentage%"

            // Laske, kuinka monta kuukautta vastaa nykyistä edistymisprosenttia
            val monthsPassed = (progressPercentage / 25).toInt()
            treatText.text = "You have kept your budget $monthsPassed months a row! Keep going and you can treat yourself soon! "
        }
    }
}