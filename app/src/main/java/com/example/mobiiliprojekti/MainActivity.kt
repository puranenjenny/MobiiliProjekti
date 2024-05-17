package com.example.mobiiliprojekti

import android.icu.text.SimpleDateFormat
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.mobiiliprojekti.databinding.ActivityMainBinding
import com.example.mobiiliprojekti.services.DatabaseManager
import com.example.mobiiliprojekti.services.SessionManager
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var databaseManager: DatabaseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        databaseManager = DatabaseManager(this)

        // Budget update if month changes
        isNewMonth()


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        navView.setupWithNavController(navController)
    }

    // When month changes new monthly budget and category budgets are added to database with previous budget values for current user.
    private fun isNewMonth() {
        val userId = SessionManager.getLoggedInUserId()
        val (_ , date) = databaseManager.fetchMonthlyBudget(userId)
        val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val currentTime = dateFormat.format(Date())

        val previousDate = dateFormat.parse(date)
        val currentDate = dateFormat.parse(currentTime)


        if (currentDate > previousDate){
            databaseManager.updateBudgetsForNewMonth(userId)

        }

    }
}
