package com.example.mobiiliprojekti.ui.annual

import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.mobiiliprojekti.databinding.FragmentAnnualBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class AnnualFragment : Fragment() {

    private var _binding: FragmentAnnualBinding? = null
    private lateinit var lineChart: LineChart

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val annualViewModel =
            ViewModelProvider(this).get(AnnualViewModel::class.java)

        _binding = FragmentAnnualBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Get reference to TextView
        //val textView: TextView = binding.textAnnual
       // annualViewModel.text.observe(viewLifecycleOwner) {
         //   textView.text = it
       // }

        // Get reference to LineChart
        lineChart = binding.lineChart



        // Simulated data for income
        val incomeEntries = mutableListOf<Entry>()
        incomeEntries.add(Entry(0f, 1000f))
        incomeEntries.add(Entry(1f, 1500f))
        incomeEntries.add(Entry(2f, 2000f))
        incomeEntries.add(Entry(3f, 1800f))
        incomeEntries.add(Entry(4f, 1000f))
        incomeEntries.add(Entry(5f, 1500f))
        incomeEntries.add(Entry(6f, 2000f))
        incomeEntries.add(Entry(7f, 1800f))
        incomeEntries.add(Entry(8f, 1800f))
        incomeEntries.add(Entry(9f, 1000f))
        incomeEntries.add(Entry(10f, 1500f))
        incomeEntries.add(Entry(11f, 2000f))

        // Simulated data for expenses
        val expenseEntries = mutableListOf<Entry>()
        expenseEntries.add(Entry(0f, 500f))
        expenseEntries.add(Entry(1f, 700f))
        expenseEntries.add(Entry(2f, 1000f))
        expenseEntries.add(Entry(3f, 1200f))
        expenseEntries.add(Entry(4f, 500f))
        expenseEntries.add(Entry(5f, 700f))
        expenseEntries.add(Entry(6f, 1000f))
        expenseEntries.add(Entry(7f, 1200f))
        expenseEntries.add(Entry(8f, 500f))
        expenseEntries.add(Entry(9f, 700f))
        expenseEntries.add(Entry(10f, 1000f))
        expenseEntries.add(Entry(11f, 1200f))

        // Create datasets for LineChart
        val incomeDataSet = LineDataSet(incomeEntries, "Income")
        val expenseDataSet = LineDataSet(expenseEntries, "Expenses")

        incomeDataSet.color = Color.GREEN
        expenseDataSet.color = Color.GRAY

        // Combine datasets into one data object
        val lineData = LineData(incomeDataSet, expenseDataSet)

        // Set data to LineChart
        lineChart.data = lineData

        // Customize LineChart as needed
        lineChart.description.isEnabled = false
        lineChart.animateY(1000)

        // Set X-axis labels (months)
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val xAxis = lineChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(months)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setCenterAxisLabels(true)
        xAxis.granularity = 1f
        xAxis.isGranularityEnabled = true
        xAxis.labelCount = months.size

        // Set Y-axis labels (amounts)
        val yAxisLeft = lineChart.axisLeft
        yAxisLeft.axisMinimum = 100f
        yAxisLeft.axisMaximum = 2500f
        yAxisLeft.granularity = 200f
        yAxisLeft.isGranularityEnabled = true

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}