package com.example.warkit.domain.repository

import com.example.warkit.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getAllProducts(): Flow<List<Product>>
    fun getProductsByCategory(category: String): Flow<List<Product>>
    fun getLowStockProducts(): Flow<List<Product>>
    fun searchProducts(query: String): Flow<List<Product>>
    fun getAllCategories(): Flow<List<String>>
    suspend fun getProductById(id: Long): Product?
    suspend fun getProductBySku(sku: String): Product?
    suspend fun getProductsBySkus(skus: List<String>): List<Product>
    suspend fun insertProduct(product: Product): Long
    suspend fun updateProduct(product: Product)
    suspend fun reduceStock(productId: Long, quantity: Int): Boolean
    suspend fun increaseStock(productId: Long, quantity: Int)
    suspend fun deleteProduct(id: Long)
}
