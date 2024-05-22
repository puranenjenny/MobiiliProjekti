package com.example.mobiiliprojekti.ui.annual

import android.graphics.Color
import android.os.Bundle
import android.text.Html
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
import java.util.Calendar
import com.example.mobiiliprojekti.services.SessionManager

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

        // Get logged in user ID
        val loggedInUserId = SessionManager.getLoggedInUserId()

        // Get reference to LineChart
        lineChart = binding.lineChart

        // Fetch monthly budgets and expenses from database
        val monthlyExpenses = fetchMonthlyExpensesFromDatabase(loggedInUserId)
        val monthlyBudgets = fetchMonthlyBudgetsFromDatabase(loggedInUserId)

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
        val budgetDataSet = LineDataSet(budgetEntries, "Monthly Budget    ")
        val darkGreen = ContextCompat.getColor(requireContext(), R.color.dark_green)
        budgetDataSet.color = darkGreen
        budgetDataSet.lineWidth = 5f
        budgetDataSet.setCircleColor(darkGreen)
        budgetDataSet.setDrawValues(false)
        budgetDataSet.valueTextSize = 14f

        // Create LineDataSet object for expenses
        val expenseDataSet = LineDataSet(expenseEntries, "Monthly Expenses")
        val brown = ContextCompat.getColor(requireContext(), R.color.button)
        expenseDataSet.color = brown
        expenseDataSet.lineWidth = 5f
        expenseDataSet.setCircleColor(brown)
        expenseDataSet.setDrawValues(false)
        expenseDataSet.valueTextSize = 14f


        // Create LineData object containing both budgets and expenses
        val lineData = LineData(budgetDataSet, expenseDataSet)

        // Set LineData to LineChart
        lineChart.data = lineData

        // Customize LineChart as needed
        lineChart.description.isEnabled = false
        lineChart.animateY(1000)
        lineChart.setExtraOffsets(0f, 0f, 0f, 16f) // Add extra space (example. 16f)

        // Set Y-axis labels (amounts)
        val yAxisLeft = lineChart.axisLeft
        yAxisLeft.granularity = 100f // Set gaps
        yAxisLeft.isGranularityEnabled = true
        yAxisLeft.textSize = 14f


        val yAxisRight = lineChart.axisRight
        yAxisRight.isEnabled = true
        yAxisRight.textSize = 14f


        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)

    // Customize legend text size
        val legend = lineChart.legend
        legend.textSize = 14f // Set text size for legend labels


    // array of months names
        val monthNames = arrayOf(
            "Jan",
            "Feb",
            "Mar",
            "Apr",
            "May",
            "Jun",
            "Jul",
            "Aug",
            "Sep",
            "Oct",
            "Nov",
            "Dec"
        )

    // set month names as x axis value
        xAxis.valueFormatter = IndexAxisValueFormatter(monthNames)
        xAxis.labelCount = monthNames.size

    // set gaps to x-axis
        xAxis.isGranularityEnabled = true
        xAxis.granularity = 1f

    // set axis key matrix value
        xAxis.setAvoidFirstLastClipping(true)

        displayTreatMeterAnnual()

        return root

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        displayTreatMeterAnnual()
    }

    private fun fetchMonthlyBudgetsFromDatabase(userId: Long): List<Float> {
        val monthlyBudgets = mutableListOf<Float>()

        try {
            val db = databaseManager.readableDatabase

            for (month in Calendar.JANUARY..Calendar.DECEMBER) {
                val cursor = db.rawQuery(
                    "SELECT month_budget FROM monthly_budget WHERE strftime('%Y', date) = ? AND strftime('%m', date) = ? AND user = ? ORDER BY date DESC LIMIT 1",
                    arrayOf(Calendar.getInstance().get(Calendar.YEAR).toString(), (month + 1).toString().padStart(2, '0'), userId.toString())
                )

                if (cursor != null) {
                    val budgetIndex = cursor.getColumnIndex("month_budget")

                    if (budgetIndex != -1 && cursor.moveToFirst()) {
                        val monthBudget = cursor.getFloat(budgetIndex)
                        monthlyBudgets.add(monthBudget)
                    } else {
                        monthlyBudgets.add(0f) // set zero if budget doesn't exist
                    }
                    cursor.close()
                }
            }
            db.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return monthlyBudgets
    }

    private fun fetchMonthlyExpensesFromDatabase(userId: Long): List<Float> {
        val monthlyExpenses = mutableListOf<Float>()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // get monthly expenses for current year
        for (month in 1..12) {
            val expensesForMonth = getExpensesForMonth(month, currentYear, userId)
            monthlyExpenses.add(expensesForMonth)
        }

        return monthlyExpenses
    }

    private fun getExpensesForMonth(month: Int, year: Int, userId: Long): Float {
        val db = databaseManager.readableDatabase
        var totalExpense = 0f

        // get monthly expenses for logged in user
        val cursor = db.rawQuery(
            "SELECT SUM(value) AS total_expense FROM purchase WHERE strftime('%Y-%m', date) = ? AND user = ?",
            arrayOf(String.format("%d-%02d", year, month), userId.toString())
        )

        try {
            if (cursor.moveToFirst()) {
                val expenseIndex = cursor.getColumnIndex("total_expense")
                if (expenseIndex != -1) {
                    totalExpense = cursor.getFloat(expenseIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
            db.close()
        }

        return totalExpense
    }

    // function for showing treat meters value
    private fun displayTreatMeterAnnual() {
        val goalText = binding.txtGoalTextAnnual
        val (_, savingsValue, _) = databaseManager.getSavings()
        val (_, treatValue) = databaseManager.getTreat()
        var treatPercent = 0

        if (treatValue != null && savingsValue != null) {
            treatPercent = if (treatValue > 0) (savingsValue / treatValue * 100).toInt() else 0
        }

        val treatRemainingPercent = if (treatPercent <= 100) treatPercent else 100

        val textViewProgress = binding.progressText
        textViewProgress.text = if (treatPercent <= 100) "$treatRemainingPercent%" else "100%"

        val progressBar = binding.progressBarAnnual
        progressBar.max = 100

        when {
            treatPercent >= 100 -> {
                progressBar.progressTintList = ContextCompat.getColorStateList(requireContext(), R.color.button)
                progressBar.progress = 100
                val layoutParams = goalText.layoutParams
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                goalText.layoutParams = layoutParams
                val formattedText = "<b>Great job!</b> You have achieved your goal!<br>Set new goal at profile page!"
                goalText.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)
            }
            treatPercent < 0 -> {
                progressBar.progress = 100
                progressBar.progressTintList = ContextCompat.getColorStateList(requireContext(), R.color.cancel)
                textViewProgress.setTextColor(Color.WHITE)
                goalText.text = ""
            }
            treatPercent > 53 -> {
                progressBar.progress = treatPercent
                progressBar.progressTintList = ContextCompat.getColorStateList(requireContext(), R.color.main)
                textViewProgress.setTextColor(Color.WHITE)
                goalText.text = ""
            }
            treatPercent == 0 -> {
                progressBar.progress = 0
                progressBar.progressTintList = ContextCompat.getColorStateList(requireContext(), R.color.main)
                goalText.text = ""
            }
            else -> {
                progressBar.progress = treatPercent
                progressBar.progressTintList = ContextCompat.getColorStateList(requireContext(), R.color.main)
                goalText.text = ""
            }
        }
    }

}


