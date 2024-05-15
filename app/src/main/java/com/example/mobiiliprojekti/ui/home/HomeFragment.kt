package com.example.mobiiliprojekti.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.mobiiliprojekti.databinding.FragmentHomeBinding
import com.example.mobiiliprojekti.services.SharedViewModel
import com.example.mobiiliprojekti.ui.addpurchase.AddPurchaseFragment
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private var currentMonthIndex: Int = LocalDate.now().monthValue
    private var currentYear: Int = LocalDate.now().year

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("HomeFragment", "onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("HomeFragment", "onCreateView")
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        sharedViewModel.userId.observe(viewLifecycleOwner) { userId ->
            Log.d("HomeFragment", "Observed User ID: $userId")
        }

        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        updateMonthYearDisplay()
        updateDaysLeftDisplay()

        binding.btnMonthBack.setOnClickListener {
            navigateMonths(-1)
        }
        binding.btnMonthForward.setOnClickListener {
            navigateMonths(1)
        }


        binding.btnAddNew.setOnClickListener {
            AddPurchaseFragment().show(childFragmentManager, "AddPurchaseDialog")
            sharedViewModel.userId.value?.let { userId ->
                Log.d("HomeFragment", "Home fragmentissa User ID: $userId")
            } ?: run {
                println("User ID is not available when clicked add new button")
            }
        }

        binding.btnPrintUserId.setOnClickListener {
            sharedViewModel.userId.value?.let { userId ->
                Log.d("HomeFragment", "Print User ID Button: $userId")
                Toast.makeText(context, "User ID: $userId", Toast.LENGTH_SHORT).show()
            } ?: run {
                Log.d("HomeFragment", "User ID is not available in home fragment button")
                Toast.makeText(context, "User ID is not available in home fragment button", Toast.LENGTH_SHORT).show()
            }
        }

        return root
    }

    override fun onStart() {
        super.onStart()
        Log.d("HomeFragment", "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d("HomeFragment", "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d("HomeFragment", "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d("HomeFragment", "onStop")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("HomeFragment", "onDestroyView")
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("HomeFragment", "onDestroy")
    }

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
            totalDaysInMonth
        } else if (currentYear < today.year || (currentYear == today.year && currentMonthIndex < today.monthValue)) {
            0
        } else {
            totalDaysInMonth - today.dayOfMonth + 1
        }

        binding.txtDaysleft.text = if (daysLeft > 0) "$daysLeft / $totalDaysInMonth" else "0"
    }
}
