package com.example.warkit.domain.repository

import com.example.warkit.domain.model.Invoice
import com.example.warkit.domain.model.InvoiceItem
import com.example.warkit.domain.model.InvoiceStatus
import kotlinx.coroutines.flow.Flow

interface InvoiceRepository {
    fun getAllInvoices(): Flow<List<Invoice>>
    fun getInvoicesByCustomer(customerId: Long): Flow<List<Invoice>>
    fun getInvoicesByStatus(status: InvoiceStatus): Flow<List<Invoice>>
    fun getInvoicesByDateRange(startDate: Long, endDate: Long): Flow<List<Invoice>>
    suspend fun getInvoiceById(id: Long): Invoice?
    suspend fun getInvoiceWithItems(id: Long): Invoice?
    suspend fun generateInvoiceNumber(): String
    suspend fun createInvoice(invoice: Invoice, items: List<InvoiceItem>): Long
    suspend fun updateInvoiceStatus(invoiceId: Long, status: InvoiceStatus)
    suspend fun deleteInvoice(id: Long)
}
