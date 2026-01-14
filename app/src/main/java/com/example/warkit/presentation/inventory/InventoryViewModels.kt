package com.example.warkit.presentation.inventory

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warkit.domain.model.Product
import com.example.warkit.domain.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Filter options for inventory
enum class InventoryFilter {
    ALL,
    LOW_STOCK
}

data class InventoryListState(
    val products: List<Product> = emptyList(),
    val categories: List<String> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val filter: InventoryFilter = InventoryFilter.ALL,
    val isLoading: Boolean = true
)

class InventoryListViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _filter = MutableStateFlow(InventoryFilter.ALL)
    
    val state: StateFlow<InventoryListState> = combine(
        productRepository.getAllProducts(),
        productRepository.getAllCategories(),
        _searchQuery,
        _selectedCategory,
        _filter
    ) { products, categories, query, category, filter ->
        var filtered = products
        
        // Apply category filter
        if (category != null) {
            filtered = filtered.filter { it.category == category }
        }
        
        // Apply low stock filter
        if (filter == InventoryFilter.LOW_STOCK) {
            filtered = filtered.filter { it.isLowStock }
        }
        
        // Apply search filter
        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.sku.contains(query, ignoreCase = true)
            }
        }
        
        InventoryListState(
            products = filtered,
            categories = categories,
            searchQuery = query,
            selectedCategory = category,
            filter = filter,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InventoryListState()
    )
    
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
    
    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
    }
    
    fun onFilterChange(filter: InventoryFilter) {
        _filter.value = filter
    }
    
    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            productRepository.deleteProduct(productId)
        }
    }
}

// Add Product State
data class AddProductState(
    val name: String = "",
    val sku: String = "",
    val price: String = "",
    val stock: String = "",
    val minStock: String = "5",
    val category: String = "",
    val description: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val availableCategories: List<String> = emptyList()
)

class AddProductViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {
    
    var state by mutableStateOf(AddProductState())
        private set
    
    init {
        loadCategories()
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            productRepository.getAllCategories().collect { categories ->
                state = state.copy(availableCategories = categories)
            }
        }
    }
    
    fun onNameChange(name: String) {
        state = state.copy(name = name, errorMessage = null)
    }
    
    fun onSkuChange(sku: String) {
        state = state.copy(sku = sku)
    }
    
    fun onPriceChange(price: String) {
        // Only allow numbers and decimal point
        if (price.isEmpty() || price.matches(Regex("^\\d*\\.?\\d*$"))) {
            state = state.copy(price = price)
        }
    }
    
    fun onStockChange(stock: String) {
        // Only allow numbers
        if (stock.isEmpty() || stock.all { it.isDigit() }) {
            state = state.copy(stock = stock)
        }
    }
    
    fun onMinStockChange(minStock: String) {
        // Only allow numbers
        if (minStock.isEmpty() || minStock.all { it.isDigit() }) {
            state = state.copy(minStock = minStock)
        }
    }
    
    fun onCategoryChange(category: String) {
        state = state.copy(category = category)
    }
    
    fun onDescriptionChange(description: String) {
        state = state.copy(description = description)
    }
    
    fun saveProduct() {
        if (state.name.isBlank()) {
            state = state.copy(errorMessage = "Nama produk tidak boleh kosong")
            return
        }
        
        val price = state.price.toDoubleOrNull() ?: 0.0
        val stock = state.stock.toIntOrNull() ?: 0
        val minStock = state.minStock.toIntOrNull() ?: 5
        val skuToCheck = state.sku.trim()
        
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            try {
                // Check for SKU duplicate if SKU is not empty
                if (skuToCheck.isNotEmpty()) {
                    val existingProduct = productRepository.getProductBySku(skuToCheck)
                    if (existingProduct != null) {
                        state = state.copy(
                            isLoading = false,
                            errorMessage = "SKU '$skuToCheck' sudah digunakan oleh produk '${existingProduct.name}'"
                        )
                        return@launch
                    }
                }
                
                val product = Product(
                    name = state.name.trim(),
                    sku = skuToCheck,
                    price = price,
                    stock = stock,
                    minStock = minStock,
                    category = state.category.trim(),
                    description = state.description.trim()
                )
                productRepository.insertProduct(product)
                state = state.copy(isLoading = false, isSaved = true)
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    errorMessage = "Gagal menyimpan produk: ${e.message}"
                )
            }
        }
    }
}

// Edit Product State
data class EditProductState(
    val productId: Long = 0,
    val name: String = "",
    val sku: String = "",
    val price: String = "",
    val stock: String = "",
    val minStock: String = "5",
    val category: String = "",
    val description: String = "",
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val availableCategories: List<String> = emptyList()
)

class EditProductViewModel(
    private val productRepository: ProductRepository,
    private val productId: Long
) : ViewModel() {
    
    var state by mutableStateOf(EditProductState(productId = productId))
        private set
    
    init {
        loadProduct()
        loadCategories()
    }
    
    private fun loadProduct() {
        viewModelScope.launch {
            val product = productRepository.getProductById(productId)
            if (product != null) {
                state = state.copy(
                    name = product.name,
                    sku = product.sku,
                    price = if (product.price > 0) product.price.toString() else "",
                    stock = product.stock.toString(),
                    minStock = product.minStock.toString(),
                    category = product.category,
                    description = product.description,
                    isLoading = false
                )
            } else {
                state = state.copy(
                    isLoading = false,
                    errorMessage = "Produk tidak ditemukan"
                )
            }
        }
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            productRepository.getAllCategories().collect { categories ->
                state = state.copy(availableCategories = categories)
            }
        }
    }
    
    fun onNameChange(name: String) {
        state = state.copy(name = name, errorMessage = null)
    }
    
    fun onSkuChange(sku: String) {
        state = state.copy(sku = sku)
    }
    
    fun onPriceChange(price: String) {
        if (price.isEmpty() || price.matches(Regex("^\\d*\\.?\\d*$"))) {
            state = state.copy(price = price)
        }
    }
    
    fun onStockChange(stock: String) {
        if (stock.isEmpty() || stock.all { it.isDigit() }) {
            state = state.copy(stock = stock)
        }
    }
    
    fun onMinStockChange(minStock: String) {
        if (minStock.isEmpty() || minStock.all { it.isDigit() }) {
            state = state.copy(minStock = minStock)
        }
    }
    
    fun onCategoryChange(category: String) {
        state = state.copy(category = category)
    }
    
    fun onDescriptionChange(description: String) {
        state = state.copy(description = description)
    }
    
    fun saveProduct() {
        if (state.name.isBlank()) {
            state = state.copy(errorMessage = "Nama produk tidak boleh kosong")
            return
        }
        
        val price = state.price.toDoubleOrNull() ?: 0.0
        val stock = state.stock.toIntOrNull() ?: 0
        val minStock = state.minStock.toIntOrNull() ?: 5
        val skuToCheck = state.sku.trim()
        
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            try {
                // Check for SKU duplicate if SKU is not empty (exclude current product)
                if (skuToCheck.isNotEmpty()) {
                    val existingProduct = productRepository.getProductBySku(skuToCheck)
                    if (existingProduct != null && existingProduct.id != state.productId) {
                        state = state.copy(
                            isLoading = false,
                            errorMessage = "SKU '$skuToCheck' sudah digunakan oleh produk '${existingProduct.name}'"
                        )
                        return@launch
                    }
                }
                
                val product = Product(
                    id = state.productId,
                    name = state.name.trim(),
                    sku = skuToCheck,
                    price = price,
                    stock = stock,
                    minStock = minStock,
                    category = state.category.trim(),
                    description = state.description.trim()
                )
                productRepository.updateProduct(product)
                state = state.copy(isLoading = false, isSaved = true)
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    errorMessage = "Gagal menyimpan produk: ${e.message}"
                )
            }
        }
    }
}
