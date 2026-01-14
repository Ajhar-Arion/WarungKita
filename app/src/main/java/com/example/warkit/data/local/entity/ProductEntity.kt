package com.example.warkit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sku: String = "",
    val price: Double = 0.0,
    val stock: Int = 0,
    val minStock: Int = 5,
    val category: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
