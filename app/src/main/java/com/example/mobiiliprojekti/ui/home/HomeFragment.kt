package com.example.mobiiliprojekti.ui.home

import AddPurchaseFragment
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
            AddPurchaseFragment().show(childFragmentManager, "AddPurchaseDialog")
        }

        displayLastPurchases()

        return root
    }

    private fun displayLastPurchases() {
        val userId = SessionManager.getLoggedInUserId()
        val purchases = databaseManager.getLastPurchases(userId)
        val purchasesLayout = binding.linLastpaymentsHome

        purchases.forEach { purchase ->
            val purchaseView = TextView(context).apply {
                text = "${purchase.date}: ${purchase.name} - ${purchase.value} €"
                textSize = 16f
                setPadding(8, 8, 8, 8)
            }
            purchasesLayout.addView(purchaseView)
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



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}