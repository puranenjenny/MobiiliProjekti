package com.example.mobiiliprojekti.ui.annual


import android.database.sqlite.SQLiteDatabase
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
import java.time.LocalDate
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
        val budgetDataSet = LineDataSet(budgetEntries, "Monthly Budget")
        val dark_green = ContextCompat.getColor(requireContext(), R.color.dark_green)
        budgetDataSet.color = dark_green
        budgetDataSet.lineWidth = 5f
        budgetDataSet.setCircleColor(dark_green)
        budgetDataSet.setDrawValues(false)

        // Create LineDataSet object for expenses
        val expenseDataSet = LineDataSet(expenseEntries, "Monthly Expenses")
        val brown = ContextCompat.getColor(requireContext(), R.color.button)
        expenseDataSet.color = brown
        expenseDataSet.lineWidth = 5f
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
        yAxisRight.isEnabled = true // Poista oikeanpuoleinen Y-akseli käytöstä

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)

// Luo kuukausien nimet
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

// Aseta kuukausien nimet akselin arvoiksi
        xAxis.valueFormatter = IndexAxisValueFormatter(monthNames)
        xAxis.labelCount = monthNames.size

// Aseta akselin avainten välistä etäisyyttä
        xAxis.isGranularityEnabled = true
        xAxis.granularity = 1f

// Aseta akselin avainmatriisin tyyppi kuukausittaiseksi
        xAxis.setAvoidFirstLastClipping(true)
        return root

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                        monthlyBudgets.add(0f) // Asetetaan nolla, jos budjettia ei löydy
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



    private fun fetchMonthlyBudget(userId: Long, year: Int, month: Int): Float {
        val db = databaseManager.readableDatabase
        var totalBudget = 0f

        try {
            val cursor = db.rawQuery(
                "SELECT month_budget FROM monthly_budget WHERE strftime('%Y', date) = ? AND strftime('%m', date) = ? AND user = ?",
                arrayOf(year.toString(), (month + 1).toString().padStart(2, '0'), userId.toString())
            )

            if (cursor.moveToFirst()) {
                val budgetIndex = cursor.getColumnIndex("month_budget")
                if (budgetIndex != -1) {
                    totalBudget = cursor.getFloat(budgetIndex)
                }
            } else {
                // Lisää tyhjä budjetti, jos sitä ei löydetty
                println("No budget found for month $month, adding empty budget.")
                totalBudget = 0f
            }

            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }

        return totalBudget
    }

    private fun fetchMonthlyExpensesFromDatabase(userId: Long): List<Float> {
        val monthlyExpenses = mutableListOf<Float>()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // Haetaan kuukausittaiset menot nykyiselle vuodelle
        for (month in 1..12) {
            val expensesForMonth = getExpensesForMonth(month, currentYear, userId)
            monthlyExpenses.add(expensesForMonth)
        }

        return monthlyExpenses
    }

    private fun getExpensesForMonth(month: Int, year: Int, userId: Long): Float {
        val db = databaseManager.readableDatabase
        var totalExpense = 0f

        // Haetaan tietyn kuukauden menot nykyiselle vuodelle ja tietylle käyttäjälle
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
}


