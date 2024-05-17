package com.example.mobiiliprojekti.services

object BudgetHandler {
    private var monhtlyBudgetByMonth: Int = 0

    fun setMonhtlyBudgetByMonth(BudgetByMonth: Int) {
        monhtlyBudgetByMonth = BudgetByMonth
    }

    fun getMonhtlyBudgetByMonth(): Int {
        return monhtlyBudgetByMonth
    }

}