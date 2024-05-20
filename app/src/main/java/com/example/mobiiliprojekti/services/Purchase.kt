package com.example.mobiiliprojekti.services

data class Purchase(
    val purchaseId: Long,
    val name: String,
    val price: Double,
    val category: Int,
    val date: String,
    val userId: Long
)
