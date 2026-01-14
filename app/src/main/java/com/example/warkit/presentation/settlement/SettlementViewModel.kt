package com.example.warkit.presentation.settlement

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warkit.domain.model.Customer
import com.example.warkit.domain.model.Invoice
import com.example.warkit.domain.model.InvoiceStatus
import com.example.warkit.domain.repository.CustomerRepository
import com.example.warkit.domain.repository.InvoiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettlementState(
    val pendingInvoices: List<Invoice> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val selectedCustomerId: Long? = null,
    val selectedInvoiceIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
) {
    val filteredInvoices: List<Invoice>
        get() = if (selectedCustomerId != null) {
            pendingInvoices.filter { it.customerId == selectedCustomerId }
        } else {
            pendingInvoices
        }
    
    val totalSelected: Double
        get() = pendingInvoices
            .filter { it.id in selectedInvoiceIds }
            .sumOf { it.totalAmount }
    
    val selectedCount: Int
        get() = selectedInvoiceIds.size
}

class SettlementViewModel(
    private val invoiceRepository: InvoiceRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {
    
    private val _selectedCustomerId = MutableStateFlow<Long?>(null)
    private val _selectedInvoiceIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _isProcessing = MutableStateFlow(false)
    private val _successMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    
    val state: StateFlow<SettlementState> = combine(
        invoiceRepository.getInvoicesByStatus(InvoiceStatus.PENDING),
        customerRepository.getAllCustomers(),
        _selectedCustomerId,
        _selectedInvoiceIds,
        _isProcessing
    ) { invoices, customers, customerId, selectedIds, processing ->
        SettlementState(
            pendingInvoices = invoices,
            customers = customers,
            selectedCustomerId = customerId,
            selectedInvoiceIds = selectedIds,
            isLoading = false,
            isProcessing = processing,
            successMessage = _successMessage.value,
            errorMessage = _errorMessage.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettlementState()
    )
    
    fun onCustomerFilterChange(customerId: Long?) {
        _selectedCustomerId.value = customerId
        // Clear selections when filter changes
        _selectedInvoiceIds.value = emptySet()
    }
    
    fun toggleInvoiceSelection(invoiceId: Long) {
        val current = _selectedInvoiceIds.value
        _selectedInvoiceIds.value = if (invoiceId in current) {
            current - invoiceId
        } else {
            current + invoiceId
        }
    }
    
    fun selectAll() {
        val currentState = state.value
        _selectedInvoiceIds.value = currentState.filteredInvoices.map { it.id }.toSet()
    }
    
    fun clearSelection() {
        _selectedInvoiceIds.value = emptySet()
    }
    
    fun settleSelected() {
        val selectedIds = _selectedInvoiceIds.value
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch {
            _isProcessing.value = true
            _errorMessage.value = null
            
            try {
                var successCount = 0
                selectedIds.forEach { invoiceId ->
                    invoiceRepository.updateInvoiceStatus(invoiceId, InvoiceStatus.PAID)
                    successCount++
                }
                
                _selectedInvoiceIds.value = emptySet()
                _successMessage.value = "$successCount invoice berhasil dilunasi"
                
            } catch (e: Exception) {
                _errorMessage.value = "Gagal melunasi: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    fun settleSingle(invoiceId: Long) {
        viewModelScope.launch {
            _isProcessing.value = true
            _errorMessage.value = null
            
            try {
                invoiceRepository.updateInvoiceStatus(invoiceId, InvoiceStatus.PAID)
                _selectedInvoiceIds.value = _selectedInvoiceIds.value - invoiceId
                _successMessage.value = "Invoice berhasil dilunasi"
            } catch (e: Exception) {
                _errorMessage.value = "Gagal melunasi: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    fun clearMessages() {
        _successMessage.value = null
        _errorMessage.value = null
    }
}
