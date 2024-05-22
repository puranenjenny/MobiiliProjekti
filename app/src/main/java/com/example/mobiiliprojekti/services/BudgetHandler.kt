package com.example.mobiiliprojekti.services

object BudgetHandler {
    private var monhtlyBudgetByMonth: Int? = null

    fun setMonthlyBudgetByMonth(budgetByMonth: Int) {
        monhtlyBudgetByMonth = budgetByMonth
    }

    fun getMonthlyBudgetByMonth(): Int? {
        return monhtlyBudgetByMonth
    }
}

object CategoryBudgetHandler {
    private var categoryBudgetByMonth: Int? = null

    fun setCategoryBudgetByMonth(categoryBudget: Int) {
        categoryBudgetByMonth = categoryBudget
    }

    fun getMonthlyCategoryBudgetByMonth(): Int? {
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