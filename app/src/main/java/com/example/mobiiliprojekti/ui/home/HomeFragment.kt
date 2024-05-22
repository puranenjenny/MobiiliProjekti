package com.example.mobiiliprojekti.ui.home

import android.graphics.Color
import android.icu.util.Calendar
import android.os.Bundle
import android.text.Html
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
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale


class HomeFragment : Fragment(), AddPurchaseDialogListener, EditPurchaseDialogListener, ChangeMonthlyBudgetDialogListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseManager: DatabaseManager
    private lateinit var barChart: HorizontalBarChart

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
        setupBarChart() // Initialize the bar chart
        displayBudgetUsageChart() // Display the budget usage chart


        // Set up FragmentResultListener
        parentFragmentManager.setFragmentResultListener("changeBudgetRequestKey", viewLifecycleOwner) { requestKey, bundle ->
            val newBudgetValue = bundle.getInt("newBudgetValue")
            println("new budget: $newBudgetValue")
            println("FragmentResultListener received new budget: $newBudgetValue") // Debug-tuloste
            handleNewBudget(newBudgetValue)
            displayBudgetUsageChart() // Update chart when budget changes
        }

        updateSavings()
        displayTreatMeter()

        return root
    }

    private fun setupBarChart() {
        barChart = binding.BarChartHome
        barChart.apply {
            description.isEnabled = false
            setDrawValueAboveBar(true)
            setPinchZoom(false)
            setDrawGridBackground(false)
            axisLeft.isEnabled = false
            axisRight.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            animateY(2000)
        }
    }

    private fun displayBudgetUsageChart() {
        val userId = SessionManager.getLoggedInUserId().toInt()
        val selectedMonth = currentMonthIndex
        val selectedYear = currentYear

        val categoryBudgets = fetchCategoryBudgetsFromDatabase(userId, selectedMonth, selectedYear)
        val categoryExpenses = fetchCategoryExpensesFromDatabase(userId, selectedMonth, selectedYear)


        if (categoryBudgets.isNotEmpty() && categoryExpenses.isNotEmpty()) {
            val entries = mutableListOf<BarEntry>()
            val labels = mutableListOf<String>()
            var index = 0f

            val barColors = mutableListOf<Int>()

            for ((category, budget) in categoryBudgets) {
                val expense = categoryExpenses[category] ?: 0f
                val usedPercentage = if (budget > 0) (expense / budget) * 100 else 0f

                val usedPercentageFormatted = if (usedPercentage > 100) 100f else usedPercentage

                val barColor = if (usedPercentageFormatted >= 100) {
                    ContextCompat.getColor(requireContext(), R.color.cancel)
                } else {
                    ContextCompat.getColor(requireContext(), R.color.dark_green)
                }

                entries.add(BarEntry(index, usedPercentageFormatted))
                labels.add(category)
                barColors.add(barColor)

                index += 1
            }

            val dataSet = BarDataSet(entries, "Budget Used %")
            dataSet.colors = barColors
            dataSet.valueTextSize = 10f
            dataSet.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "${value.toInt()} %"
                }
            }

            val data = BarData(dataSet)
            data.barWidth = 0.9f

            barChart.apply {
                this.data = data
                xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                xAxis.granularity = 1f
                setExtraOffsets(0f, 0f, 0f, 16f)
                invalidate()
            }
        } else {
            barChart.clear()

        }
    }

    private fun fetchCategoryBudgetsFromDatabase(userId: Int, month: Int, year: Int): Map<String, Float> {
        val categoryBudgets = mutableMapOf<String, Float>()


        val monthYear = String.format("%d-%02d", year, month)


        val cursor = databaseManager.readableDatabase.rawQuery(
            "SELECT c.category_name, cb.cat_budget FROM category_budget cb JOIN category c ON cb.category = c.category_id WHERE cb.user = ? AND strftime('%Y-%m', cb.date) = ? ORDER BY c.category_name DESC ",
            arrayOf(userId.toString(), monthYear)
        )
        try {
            if (cursor != null && cursor.count > 0) {
                val categoryIndex = cursor.getColumnIndex("category_name")
                val budgetIndex = cursor.getColumnIndex("cat_budget")
                if (categoryIndex != -1 && budgetIndex != -1) {
                    cursor.moveToFirst()
                    do {
                        val category = cursor.getString(categoryIndex)
                        val budget = cursor.getFloat(budgetIndex)
                        categoryBudgets[category] = budget
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return categoryBudgets
    }

    private fun fetchCategoryExpensesFromDatabase(userId: Int, month: Int, year: Int): Map<String, Float> {
        val categoryExpenses = mutableMapOf<String, Float>()


        val monthYear = String.format("%d-%02d", year, month)


        val cursor = databaseManager.readableDatabase.rawQuery(
            "SELECT c.category_name, SUM(p.value) AS total_expense FROM purchase p JOIN category c ON p.category = c.category_id WHERE p.user = ? AND strftime('%Y-%m', p.date) = ? GROUP BY c.category_name ORDER BY c.category_name DESC",
            arrayOf(userId.toString(), monthYear)
        )
        try {
            if (cursor != null && cursor.count > 0) {
                val categoryIndex = cursor.getColumnIndex("category_name")
                val expenseIndex = cursor.getColumnIndex("total_expense")
                if (categoryIndex != -1 && expenseIndex != -1) {
                    cursor.moveToFirst()
                    do {
                        val category = cursor.getString(categoryIndex)
                        val expense = cursor.getFloat(expenseIndex)
                        categoryExpenses[category] = expense
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return categoryExpenses

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
                    val categoryName = databaseManager.getCategoryNameById(purchase.category)
                    val purchaseView = TextView(context).apply {
                        text = "${purchase.date} - $categoryName - ${purchase.name} - ${purchase.price} €"
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
        val editPurchaseDialog = EditPurchase(purchase, this as EditPurchaseDialogListener)
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

        displayMoneySpent()
        displayMoneyLeft()
        displayLastPurchases()
        displayMonthlyBudget()
        displayBudgetUsageChart()
        displayTreatMeter()

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
    private fun displayMoneyLeft()  {
        val userId = SessionManager.getLoggedInUserId()
        val monthlyBudget: Double
        val selectedMonth = currentMonthIndex
        val selectedYear = currentYear

        val currentMonth = LocalDate.now().monthValue
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        monthlyBudget = if (selectedYear > currentYear || (selectedYear == currentYear && selectedMonth > currentMonth)) {
            val budget = databaseManager.getSelectedMonthsBudget(userId, selectedMonth, selectedYear) ?: 0
            if (budget != 0)
            (databaseManager.getSelectedMonthsBudget(userId, selectedMonth, selectedYear) ?: 0.0).toDouble()
            else{
                (databaseManager.getSelectedMonthsBudget(userId, currentMonth, currentYear) ?: 0.0).toDouble()
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

    //this function shows monthly budget on monthly basis
    private fun displayMonthlyBudget() {
        val userId = SessionManager.getLoggedInUserId()
        val monthlyBudget: Double
        val selectedMonth = currentMonthIndex
        val selectedYear = currentYear

        val monthNow = LocalDate.now().monthValue
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        println("current: $monthNow/$currentYear")



        monthlyBudget = if (selectedYear > currentYear || (selectedYear == currentYear && selectedMonth > monthNow)) {
            val budget = databaseManager.getSelectedMonthsBudget(userId, selectedMonth, selectedYear) ?: 0
            if (budget != 0)
                (databaseManager.getSelectedMonthsBudget(userId, selectedMonth, selectedYear) ?: 0.0).toDouble()
            else{
                (databaseManager.getSelectedMonthsBudget(userId, monthNow, currentYear) ?: 0.0).toDouble()
            }
        } else {
            //if displayed month is current month or in past shows specific budget for that month or 0 if budget doesn't exist
            (databaseManager.getSelectedMonthsBudget(userId, selectedMonth, selectedYear) ?: 0.0).toDouble()

        }


        binding.txtMonthlyBudetNum.text = "${String.format("%.0f", monthlyBudget)} €"

    }

    ///when add purchase dialog closes these functions are called to update view
    override fun onDialogDismissed() {
        displayLastPurchases()
        displayMoneySpent()
        displayMoneyLeft()
        displayBudgetUsageChart()
        displayTreatMeter()
    }

    ///when change monthly budget dialog closes these functions are called to update view
    override fun onDialogDismissed2() {
        handleNewBudget(BudgetHandler.getMonthlyBudgetByMonth())
        displayBudgetUsageChart()
    }

    override fun onResume() {
        super.onResume()
        displayTreatMeter()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //This function handles monthly budget change
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
            databaseManager.addCategoryBudgetsForNewMonthlyBudget(SessionManager.getLoggedInUserId(), futureDate)
        } else if (selectedYear < currentYear || (selectedYear == currentYear && selectedMonth < currentMonth)) {
            println("Past date: $pastDate") // Debug-tuloste
            databaseManager.changeMonthlyBudgetByMonth(newBudgetValue, pastDate)
            databaseManager.addCategoryBudgetsForNewMonthlyBudget(SessionManager.getLoggedInUserId(), pastDate)
        } else {
            println("current date: $formattedDate") // Debug-tuloste
            databaseManager.changeMonthlyBudgetByMonth(newBudgetValue, formattedDate)
        }

        displayMonthlyBudget()
        displayMoneySpent()
        displayMoneyLeft()
        displayBudgetUsageChart()
    }

    //This function saves money left value to savings table in db when month changes
    private fun updateSavings(){
        val savingsValueNew = moneyLeftLastMonth()

        val (savingsId, savingsValue, savingsDate) = databaseManager.getSavings()
        val month = savingsDate?.substring(5,7)?.toInt()
        var monthIndex = month?.plus(1)
        var year = savingsDate?.substring(0, 4)?.toInt()

        val monthNow = LocalDate.now().monthValue
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        if (monthNow == 1) {
            monthIndex = 1
            if (year != null) {
                year += 1
            }
        }

        if(year == currentYear && monthIndex == monthNow) {
            println("saved1: $savingsValueNew")
            if (savingsId != null && savingsValue != null) {
                val saved = savingsValue + (savingsValueNew)
                println("saved: $saved")
                databaseManager.updateSavings(savingsId, saved)
            }
        }
    }

    // this function checks money left value from last month to be saved in db
    private fun moneyLeftLastMonth(): Double {
        val userId = SessionManager.getLoggedInUserId()
        val monthlyBudget: Double
        val moneyLeft: Double
        var selectedMonth = LocalDate.now().monthValue - 1
        var selectedYear = Calendar.getInstance().get(Calendar.YEAR)

        if (selectedMonth == 0){
            selectedMonth = 12
            selectedYear = Calendar.getInstance().get(Calendar.YEAR) - 1
            println("$selectedMonth and $selectedYear")
        }

        monthlyBudget = (databaseManager.getSelectedMonthsBudget(userId, selectedMonth, selectedYear) ?: 0.0).toDouble()

        val homeExpenses = databaseManager.getSelectedMonthsPurchases(userId, selectedMonth, selectedYear).sum()

        moneyLeft = monthlyBudget - homeExpenses
        return moneyLeft
    }

    private fun displayTreatMeter() {
        val goalText = binding.txtGoalAchieved
        val (savingsId, savingsValue, savingsDate) = databaseManager.getSavings()
        val (treat, treatValue) = databaseManager.getTreat()
        var treatPercent = 0

        if (treatValue != null && savingsValue != null) {
            treatPercent = if (treatValue > 0) (savingsValue / treatValue * 100).toInt() else 0
        }
        println("treat% : $treatPercent")

        val treatRemainingPercent = if (treatPercent <= 100) treatPercent else 100

        val textViewProgress = binding.progressText2
        textViewProgress.text = if (treatPercent <= 100) "$treatRemainingPercent%" else "100%"

        val progressBar = binding.progressBar2
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