package com.example.warkit.domain.model

data class Product(
    val id: Long = 0,
    val name: String,
    val sku: String = "",
    val price: Double = 0.0,
    val stock: Int = 0,
    val minStock: Int = 5,
    val category: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean get() = stock <= minStock
}
