package com.example.warkit.presentation.invoice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warkit.domain.model.Invoice
import com.example.warkit.domain.model.InvoiceStatus
import com.example.warkit.domain.repository.InvoiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InvoiceListState(
    val invoices: List<Invoice> = emptyList(),
    val selectedStatus: InvoiceStatus? = null,
    val isLoading: Boolean = true
)

class InvoiceListViewModel(
    private val invoiceRepository: InvoiceRepository
) : ViewModel() {
    
    private val _selectedStatus = MutableStateFlow<InvoiceStatus?>(null)
    
    val state: StateFlow<InvoiceListState> = combine(
        invoiceRepository.getAllInvoices(),
        _selectedStatus
    ) { invoices, status ->
        val filtered = if (status != null) {
            invoices.filter { it.status == status }
        } else {
            invoices
        }
        InvoiceListState(
            invoices = filtered,
            selectedStatus = status,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InvoiceListState()
    )
    
    fun onStatusFilterChange(status: InvoiceStatus?) {
        _selectedStatus.value = status
    }
}

data class InvoiceDetailState(
    val invoice: Invoice? = null,
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val errorMessage: String? = null
)

class InvoiceDetailViewModel(
    private val invoiceRepository: InvoiceRepository,
    private val invoiceId: Long
) : ViewModel() {
    
    var state by mutableStateOf(InvoiceDetailState())
        private set
    
    init {
        loadInvoice()
    }
    
    private fun loadInvoice() {
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            val invoice = invoiceRepository.getInvoiceWithItems(invoiceId)
            state = state.copy(
                invoice = invoice,
                isLoading = false,
                errorMessage = if (invoice == null) "Invoice tidak ditemukan" else null
            )
        }
    }
    
    fun updateStatus(newStatus: InvoiceStatus) {
        viewModelScope.launch {
            state = state.copy(isUpdating = true)
            try {
                invoiceRepository.updateInvoiceStatus(invoiceId, newStatus)
                loadInvoice()
            } catch (e: Exception) {
                state = state.copy(
                    isUpdating = false,
                    errorMessage = "Gagal mengubah status: ${e.message}"
                )
            }
        }
    }
}
