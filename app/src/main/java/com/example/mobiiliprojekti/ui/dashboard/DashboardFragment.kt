package com.example.mobiiliprojekti.ui.dashboard

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.mobiiliprojekti.services.DatabaseManager
import com.example.mobiiliprojekti.databinding.FragmentDashboardBinding
import java.time.LocalDate

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // db-manager object
    private lateinit var databaseManager: DatabaseManager


    private var currentMonthIndex: Int = LocalDate.now().monthValue

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize DatabaseManager
        databaseManager = DatabaseManager(requireContext())

        // Get categories from database
        val categories = databaseManager.allCategories()

        // Find TextView
        val textView: TextView = binding.textDashboard

        // Set text to TextView
        textView.text = categories

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}