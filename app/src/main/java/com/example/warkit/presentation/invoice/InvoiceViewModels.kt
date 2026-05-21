package com.example.warkit.presentation.invoice

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warkit.domain.model.Customer
import com.example.warkit.domain.model.Invoice
import com.example.warkit.domain.model.InvoiceStatus
import com.example.warkit.domain.repository.InvoiceRepository
import com.example.warkit.util.ExcelHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

enum class HistoryPeriod(val label: String) {
    Today("Hari Ini"),
    Week("Minggu Ini"),
    Month("Bulan Ini");

    fun startMillis(now: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = now
            when (this@HistoryPeriod) {
                Today -> {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                Week -> {
                    firstDayOfWeek = Calendar.MONDAY
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                Month -> {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            }
        }.timeInMillis
    }

    fun contains(timestamp: Long, now: Long = System.currentTimeMillis()): Boolean {
        return timestamp in startMillis(now)..now
    }
}

data class InvoiceListState(
    val invoices: List<Invoice> = emptyList(),
    val selectedStatus: InvoiceStatus? = null,
    val isLoading: Boolean = true,
    val isExporting: Boolean = false,
    val exportMessage: String? = null
)

private data class InvoiceExportState(
    val isExporting: Boolean = false,
    val message: String? = null
)

class InvoiceListViewModel(
    private val invoiceRepository: InvoiceRepository
) : ViewModel() {
    
    private val _selectedStatus = MutableStateFlow<InvoiceStatus?>(null)
    private val _exportState = MutableStateFlow(InvoiceExportState())
    
    val state: StateFlow<InvoiceListState> = combine(
        invoiceRepository.getAllInvoices(),
        _selectedStatus,
        _exportState
    ) { invoices, status, exportState ->
        val filtered = if (status != null) {
            invoices.filter { it.status == status }
        } else {
            invoices
        }
        InvoiceListState(
            invoices = filtered,
            selectedStatus = status,
            isLoading = false,
            isExporting = exportState.isExporting,
            exportMessage = exportState.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InvoiceListState()
    )
    
    fun onStatusFilterChange(status: InvoiceStatus?) {
        _selectedStatus.value = status
    }

    fun exportHistory(
        context: Context,
        period: HistoryPeriod,
        customers: List<Customer> = emptyList()
    ) {
        viewModelScope.launch {
            _exportState.value = InvoiceExportState(isExporting = true)

            try {
                val now = System.currentTimeMillis()
                val startDate = period.startMillis(now)
                val invoices = invoiceRepository
                    .getInvoicesByDateRange(startDate, now)
                    .first()
                    .map { invoice ->
                        invoiceRepository.getInvoiceWithItems(invoice.id) ?: invoice
                    }

                if (invoices.isEmpty()) {
                    _exportState.value = InvoiceExportState(
                        message = "Tidak ada transaksi untuk ${period.label.lowercase()}"
                    )
                    return@launch
                }

                val file = withContext(Dispatchers.IO) {
                    ExcelHelper.exportTransactionHistoryToExcel(
                        context = context,
                        invoices = invoices,
                        periodLabel = period.label,
                        customers = customers,
                        startDateMillis = startDate,
                        endDateMillis = now
                    )
                }

                ExcelHelper.openTransactionHistoryFile(context, file)
                _exportState.value = InvoiceExportState()
            } catch (e: Exception) {
                _exportState.value = InvoiceExportState(
                    message = "Gagal download history: ${e.message}"
                )
            }
        }
    }

    fun clearExportMessage() {
        _exportState.value = _exportState.value.copy(message = null)
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
            refreshInvoice(showLoading = true)
        }
    }
    
    fun updateStatus(newStatus: InvoiceStatus) {
        viewModelScope.launch {
            state = state.copy(isUpdating = true, errorMessage = null)
            try {
                invoiceRepository.updateInvoiceStatus(invoiceId, newStatus)
                refreshInvoice(showLoading = false)
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    isUpdating = false,
                    errorMessage = "Gagal mengubah status: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun refreshInvoice(showLoading: Boolean) {
        if (showLoading) {
            state = state.copy(isLoading = true, errorMessage = null)
        }
        
        try {
            val invoice = invoiceRepository.getInvoiceWithItems(invoiceId)
            state = state.copy(
                invoice = invoice,
                isLoading = false,
                isUpdating = false,
                errorMessage = if (invoice == null) "Invoice tidak ditemukan" else null
            )
        } catch (e: Exception) {
            state = state.copy(
                isLoading = false,
                isUpdating = false,
                errorMessage = "Gagal memuat invoice: ${e.message}"
            )
        }
    }
}
