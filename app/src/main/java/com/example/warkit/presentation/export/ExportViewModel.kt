package com.example.warkit.presentation.export

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warkit.domain.model.Invoice
import com.example.warkit.domain.model.InvoiceStatus
import com.example.warkit.domain.repository.InvoiceRepository
import com.example.warkit.util.ExcelHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

data class ExportState(
    val isLoading: Boolean = false,
    val startDate: Long = getStartOfMonth(),
    val endDate: Long = System.currentTimeMillis(),
    val statusFilter: InvoiceStatus? = null, // null = all
    val includeItems: Boolean = true,
    val invoices: List<Invoice> = emptyList(),
    val summary: ExcelHelper.ExportSummary? = null,
    val exportedFile: File? = null,
    val error: String? = null
)

private fun getStartOfMonth(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

class ExportViewModel(
    private val invoiceRepository: InvoiceRepository
) : ViewModel() {
    
    var state by mutableStateOf(ExportState())
        private set
    
    init {
        loadInvoices()
    }
    
    fun onStartDateChange(date: Long) {
        state = state.copy(startDate = date)
        loadInvoices()
    }
    
    fun onEndDateChange(date: Long) {
        // Set to end of day
        val cal = Calendar.getInstance()
        cal.timeInMillis = date
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        state = state.copy(endDate = cal.timeInMillis)
        loadInvoices()
    }
    
    fun onStatusFilterChange(status: InvoiceStatus?) {
        state = state.copy(statusFilter = status)
        loadInvoices()
    }
    
    fun onIncludeItemsChange(include: Boolean) {
        state = state.copy(includeItems = include)
    }
    
    private fun loadInvoices() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            
            try {
                val allInvoices = invoiceRepository.getInvoicesByDateRange(
                    state.startDate, 
                    state.endDate
                ).first()
                
                // Load items for each invoice
                val invoicesWithItems = allInvoices.map { invoice ->
                    invoiceRepository.getInvoiceWithItems(invoice.id) ?: invoice
                }
                
                // Apply status filter
                val filtered = if (state.statusFilter != null) {
                    invoicesWithItems.filter { it.status == state.statusFilter }
                } else {
                    invoicesWithItems
                }
                
                val summary = ExcelHelper.getExportSummary(filtered)
                
                state = state.copy(
                    isLoading = false,
                    invoices = filtered,
                    summary = summary
                )
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }
    
    fun exportToFile(context: Context) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            
            try {
                val file = ExcelHelper.exportInvoicesToCsv(
                    context = context,
                    invoices = state.invoices,
                    includeItems = state.includeItems
                )
                
                state = state.copy(
                    isLoading = false,
                    exportedFile = file
                )
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    error = "Gagal export: ${e.message}"
                )
            }
        }
    }
    
    fun shareExportedFile(context: Context) {
        state.exportedFile?.let { file ->
            ExcelHelper.shareExportFile(context, file)
        }
    }
    
    fun resetExport() {
        state = state.copy(exportedFile = null)
    }
}
