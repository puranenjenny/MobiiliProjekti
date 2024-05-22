package com.example.mobiiliprojekti.services

// object for passing budget value from fragment to other
object BudgetHandler {
    private var monhtlyBudgetByMonth: Int = 0

    fun setMonthlyBudgetByMonth(budgetByMonth: Int) {
        monhtlyBudgetByMonth = budgetByMonth
    }

    fun getMonthlyBudgetByMonth(): Int {
        return monhtlyBudgetByMonth
    }
}

object CategoryBudgetHandler {
    private var categoryBudgetByMonth: Int = 0

    fun setCategoryBudgetByMonth(categoryBudget: Int) {
        categoryBudgetByMonth = categoryBudget
    }

    fun getMonthlyCategoryBudgetByMonth(): Int {
        return categoryBudgetByMonth
    }
}

object SelectedCategoryHandler {
    private lateinit var selectedCategory: String

    fun setSelectedCategory(category: String) {
        selectedCategory = category
    }

    fun getSelectedCategory(): String {
        return selectedCategory
    }
}