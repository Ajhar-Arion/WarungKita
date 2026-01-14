package com.example.warkit.data.repository

import com.example.warkit.data.local.dao.ProductDao
import com.example.warkit.domain.model.Product
import com.example.warkit.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepositoryImpl(
    private val productDao: ProductDao
) : ProductRepository {
    
    override fun getAllProducts(): Flow<List<Product>> {
        return productDao.getAllProducts().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getProductsByCategory(category: String): Flow<List<Product>> {
        return productDao.getProductsByCategory(category).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getLowStockProducts(): Flow<List<Product>> {
        return productDao.getLowStockProducts().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun searchProducts(query: String): Flow<List<Product>> {
        return productDao.searchProducts(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getAllCategories(): Flow<List<String>> {
        return productDao.getAllCategories()
    }
    
    override suspend fun getProductById(id: Long): Product? {
        return productDao.getProductById(id)?.toDomain()
    }
    
    override suspend fun getProductBySku(sku: String): Product? {
        return productDao.getProductBySku(sku)?.toDomain()
    }
    
    override suspend fun getProductsBySkus(skus: List<String>): List<Product> {
        return productDao.getProductsBySkus(skus).map { it.toDomain() }
    }
    
    override suspend fun insertProduct(product: Product): Long {
        return productDao.insertProduct(product.toEntity())
    }
    
    override suspend fun updateProduct(product: Product) {
        productDao.updateProduct(product.toEntity())
    }
    
    override suspend fun reduceStock(productId: Long, quantity: Int): Boolean {
        return productDao.reduceStock(productId, quantity) > 0
    }
    
    override suspend fun increaseStock(productId: Long, quantity: Int) {
        productDao.increaseStock(productId, quantity)
    }
    
    override suspend fun deleteProduct(id: Long) {
        productDao.getProductById(id)?.let { product ->
            productDao.deleteProduct(product)
        }
    }
}
