package com.example.warkit.domain.model

data class Customer(
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val photoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
