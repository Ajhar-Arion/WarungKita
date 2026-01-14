package com.example.warkit.presentation.purchase

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warkit.domain.model.Customer
import com.example.warkit.domain.model.Invoice
import com.example.warkit.domain.model.InvoiceItem
import com.example.warkit.domain.model.InvoiceStatus
import com.example.warkit.domain.model.Product
import com.example.warkit.domain.repository.CustomerRepository
import com.example.warkit.domain.repository.InvoiceRepository
import com.example.warkit.domain.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Cart Item
data class CartItem(
    val product: Product,
    val quantity: Int = 1
) {
    val subtotal: Double get() = product.price * quantity
}

// Purchase State
data class PurchaseState(
    val selectedCustomer: Customer? = null,
    val cartItems: List<CartItem> = emptyList(),
    val notes: String = "",
    val isLoading: Boolean = false,
    val isCompleted: Boolean = false,
    val createdInvoiceId: Long? = null,
    val errorMessage: String? = null
) {
    val totalAmount: Double get() = cartItems.sumOf { it.subtotal }
    val itemCount: Int get() = cartItems.sumOf { it.quantity }
    val canCheckout: Boolean get() = selectedCustomer != null && cartItems.isNotEmpty()
}

// Select Customer State
data class SelectCustomerState(
    val customers: List<Customer> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

// Select Product State
data class SelectProductState(
    val products: List<Product> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

class PurchaseViewModel(
    private val customerRepository: CustomerRepository,
    private val productRepository: ProductRepository,
    private val invoiceRepository: InvoiceRepository
) : ViewModel() {
    
    var state by mutableStateOf(PurchaseState())
        private set
    
    private val _customersSearchQuery = MutableStateFlow("")
    private val _productsSearchQuery = MutableStateFlow("")
    
    val customersState: StateFlow<SelectCustomerState> = customerRepository.getAllCustomers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        ).let { flow ->
            MutableStateFlow(SelectCustomerState()).also { stateFlow ->
                viewModelScope.launch {
                    flow.collect { customers ->
                        stateFlow.value = SelectCustomerState(
                            customers = customers,
                            isLoading = false
                        )
                    }
                }
            }
        }
    
    val productsState: StateFlow<SelectProductState> = productRepository.getAllProducts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        ).let { flow ->
            MutableStateFlow(SelectProductState()).also { stateFlow ->
                viewModelScope.launch {
                    flow.collect { products ->
                        // Filter out products with zero stock
                        val availableProducts = products.filter { it.stock > 0 }
                        stateFlow.value = SelectProductState(
                            products = availableProducts,
                            isLoading = false
                        )
                    }
                }
            }
        }
    
    fun selectCustomer(customer: Customer) {
        state = state.copy(selectedCustomer = customer)
    }
    
    fun clearCustomer() {
        state = state.copy(selectedCustomer = null)
    }
    
    fun addToCart(product: Product) {
        val existingItem = state.cartItems.find { it.product.id == product.id }
        
        if (existingItem != null) {
            // Increase quantity if not exceeding stock
            val newQuantity = existingItem.quantity + 1
            if (newQuantity <= product.stock) {
                val updatedItems = state.cartItems.map {
                    if (it.product.id == product.id) it.copy(quantity = newQuantity)
                    else it
                }
                state = state.copy(cartItems = updatedItems)
            }
        } else {
            // Add new item
            state = state.copy(
                cartItems = state.cartItems + CartItem(product)
            )
        }
    }
    
    fun removeFromCart(productId: Long) {
        state = state.copy(
            cartItems = state.cartItems.filter { it.product.id != productId }
        )
    }
    
    fun updateQuantity(productId: Long, quantity: Int) {
        if (quantity <= 0) {
            removeFromCart(productId)
            return
        }
        
        val updatedItems = state.cartItems.map { item ->
            if (item.product.id == productId) {
                val newQuantity = minOf(quantity, item.product.stock)
                item.copy(quantity = newQuantity)
            } else item
        }
        state = state.copy(cartItems = updatedItems)
    }
    
    fun increaseQuantity(productId: Long) {
        val item = state.cartItems.find { it.product.id == productId } ?: return
        if (item.quantity < item.product.stock) {
            updateQuantity(productId, item.quantity + 1)
        }
    }
    
    fun decreaseQuantity(productId: Long) {
        val item = state.cartItems.find { it.product.id == productId } ?: return
        updateQuantity(productId, item.quantity - 1)
    }
    
    fun onNotesChange(notes: String) {
        state = state.copy(notes = notes)
    }
    
    fun checkout() {
        val customer = state.selectedCustomer ?: return
        if (state.cartItems.isEmpty()) return
        
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            
            try {
                // Generate invoice number
                val invoiceNumber = invoiceRepository.generateInvoiceNumber()
                
                // Create invoice
                val invoice = Invoice(
                    invoiceNumber = invoiceNumber,
                    customerId = customer.id,
                    customerName = customer.name,
                    totalAmount = state.totalAmount,
                    status = InvoiceStatus.PENDING,
                    notes = state.notes
                )
                
                // Create invoice items
                val items = state.cartItems.map { cartItem ->
                    InvoiceItem(
                        productId = cartItem.product.id,
                        productName = cartItem.product.name,
                        quantity = cartItem.quantity,
                        unitPrice = cartItem.product.price,
                        subtotal = cartItem.subtotal
                    )
                }
                
                // Save invoice
                val invoiceId = invoiceRepository.createInvoice(invoice, items)
                
                // Reduce stock for each product
                for (cartItem in state.cartItems) {
                    productRepository.reduceStock(cartItem.product.id, cartItem.quantity)
                }
                
                state = state.copy(
                    isLoading = false,
                    isCompleted = true,
                    createdInvoiceId = invoiceId
                )
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    errorMessage = "Gagal membuat invoice: ${e.message}"
                )
            }
        }
    }
    
    fun resetPurchase() {
        state = PurchaseState()
    }
}
