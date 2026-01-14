package com.example.warkit.presentation.`import`

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warkit.domain.model.Product
import com.example.warkit.domain.repository.ProductRepository
import com.example.warkit.util.ExcelHelper
import kotlinx.coroutines.launch

// Data class for duplicate product info
data class DuplicateProduct(
    val importProduct: Product,      // Product from import file
    val existingProduct: Product,    // Existing product in database
    val addStock: Int                 // Stock to add from import
)

data class ImportState(
    val isLoading: Boolean = false,
    val previewProducts: List<Product> = emptyList(),
    val newProducts: List<Product> = emptyList(),           // Products with new SKU
    val duplicateProducts: List<DuplicateProduct> = emptyList(), // Products with existing SKU
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val errors: List<String> = emptyList(),
    val importComplete: Boolean = false,
    val showPreview: Boolean = false,
    val showDuplicateDialog: Boolean = false,
    val importedCount: Int = 0,
    val stockUpdatedCount: Int = 0,
    val message: String = ""
)

class ImportInventoryViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {
    
    var state by mutableStateOf(ImportState())
        private set
    
    fun parseFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, errors = emptyList())
            
            try {
                val result = ExcelHelper.importFromCsv(context, uri)
                
                // Check for duplicate SKUs
                val skus = result.products.mapNotNull { 
                    it.sku.takeIf { sku -> sku.isNotBlank() } 
                }
                val existingProducts = if (skus.isNotEmpty()) {
                    productRepository.getProductsBySkus(skus)
                } else {
                    emptyList()
                }
                val existingSkuMap = existingProducts.associateBy { it.sku }
                
                val newProducts = mutableListOf<Product>()
                val duplicateProducts = mutableListOf<DuplicateProduct>()
                
                result.products.forEach { product ->
                    val existing = existingSkuMap[product.sku]
                    if (existing != null && product.sku.isNotBlank()) {
                        duplicateProducts.add(
                            DuplicateProduct(
                                importProduct = product,
                                existingProduct = existing,
                                addStock = product.stock
                            )
                        )
                    } else {
                        newProducts.add(product)
                    }
                }
                
                state = state.copy(
                    isLoading = false,
                    previewProducts = result.products,
                    newProducts = newProducts,
                    duplicateProducts = duplicateProducts,
                    successCount = result.successCount,
                    failedCount = result.failedCount,
                    errors = result.errors,
                    showPreview = result.products.isNotEmpty(),
                    showDuplicateDialog = duplicateProducts.isNotEmpty()
                )
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    errors = listOf("Error: ${e.message}"),
                    showPreview = false
                )
            }
        }
    }
    
    fun dismissDuplicateDialog() {
        state = state.copy(showDuplicateDialog = false)
    }
    
    fun confirmImport(updateDuplicateStock: Boolean) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, showDuplicateDialog = false)
            
            var imported = 0
            var stockUpdated = 0
            val newErrors = mutableListOf<String>()
            
            // Import new products
            state.newProducts.forEach { product ->
                try {
                    productRepository.insertProduct(product)
                    imported++
                } catch (e: Exception) {
                    newErrors.add("Produk '${product.name}': ${e.message}")
                }
            }
            
            // Update stock for duplicates if user confirmed
            if (updateDuplicateStock) {
                state.duplicateProducts.forEach { duplicate ->
                    try {
                        productRepository.increaseStock(
                            duplicate.existingProduct.id,
                            duplicate.addStock
                        )
                        stockUpdated++
                    } catch (e: Exception) {
                        newErrors.add("Update stok '${duplicate.existingProduct.name}': ${e.message}")
                    }
                }
            }
            
            val message = buildString {
                if (imported > 0) append("$imported produk baru diimport")
                if (stockUpdated > 0) {
                    if (isNotEmpty()) append(", ")
                    append("$stockUpdated produk stok diupdate")
                }
                if (isEmpty()) append("Tidak ada produk yang diimport")
            }
            
            state = state.copy(
                isLoading = false,
                importComplete = true,
                importedCount = imported,
                stockUpdatedCount = stockUpdated,
                errors = state.errors + newErrors,
                message = message
            )
        }
    }
    
    // Backward compatible - import only new products
    fun confirmImport() {
        confirmImport(updateDuplicateStock = false)
    }
    
    fun resetState() {
        state = ImportState()
    }
}
