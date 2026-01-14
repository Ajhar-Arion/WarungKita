package com.example.warkit.data.repository

import com.example.warkit.data.local.dao.CustomerDao
import com.example.warkit.data.local.dao.InvoiceDao
import com.example.warkit.domain.model.Invoice
import com.example.warkit.domain.model.InvoiceItem
import com.example.warkit.domain.model.InvoiceStatus
import com.example.warkit.domain.repository.InvoiceRepository
import com.example.warkit.util.InvoiceNumberGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InvoiceRepositoryImpl(
    private val invoiceDao: InvoiceDao,
    private val customerDao: CustomerDao
) : InvoiceRepository {
    
    override fun getAllInvoices(): Flow<List<Invoice>> {
        return invoiceDao.getAllInvoices().map { entities ->
            entities.map { entity ->
                val customerName = customerDao.getCustomerById(entity.customerId)?.name ?: ""
                entity.toDomain(customerName = customerName)
            }
        }
    }
    
    override fun getInvoicesByCustomer(customerId: Long): Flow<List<Invoice>> {
        return invoiceDao.getInvoicesByCustomer(customerId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getInvoicesByStatus(status: InvoiceStatus): Flow<List<Invoice>> {
        return invoiceDao.getInvoicesByStatus(status.toEntity()).map { entities ->
            entities.map { entity ->
                val customerName = customerDao.getCustomerById(entity.customerId)?.name ?: ""
                entity.toDomain(customerName = customerName)
            }
        }
    }
    
    override fun getInvoicesByDateRange(startDate: Long, endDate: Long): Flow<List<Invoice>> {
        return invoiceDao.getInvoicesByDateRange(startDate, endDate).map { entities ->
            entities.map { entity ->
                val customerName = customerDao.getCustomerById(entity.customerId)?.name ?: ""
                entity.toDomain(customerName = customerName)
            }
        }
    }
    
    override suspend fun getInvoiceById(id: Long): Invoice? {
        return invoiceDao.getInvoiceById(id)?.let { entity ->
            val customerName = customerDao.getCustomerById(entity.customerId)?.name ?: ""
            entity.toDomain(customerName = customerName)
        }
    }
    
    override suspend fun getInvoiceWithItems(id: Long): Invoice? {
        val invoiceEntity = invoiceDao.getInvoiceById(id) ?: return null
        val items = invoiceDao.getInvoiceItems(id).map { it.toDomain() }
        val customerName = customerDao.getCustomerById(invoiceEntity.customerId)?.name ?: ""
        return invoiceEntity.toDomain(items = items, customerName = customerName)
    }
    
    override suspend fun generateInvoiceNumber(): String {
        val datePrefix = InvoiceNumberGenerator.getDatePrefix()
        val lastInvoice = invoiceDao.getLastInvoiceByDatePrefix("INV-$datePrefix")
        return InvoiceNumberGenerator.generate(lastInvoice?.invoiceNumber)
    }
    
    override suspend fun createInvoice(invoice: Invoice, items: List<InvoiceItem>): Long {
        val invoiceId = invoiceDao.insertInvoice(invoice.toEntity())
        val itemEntities = items.map { it.copy(invoiceId = invoiceId).toEntity() }
        invoiceDao.insertInvoiceItems(itemEntities)
        return invoiceId
    }
    
    override suspend fun updateInvoiceStatus(invoiceId: Long, status: InvoiceStatus) {
        invoiceDao.updateInvoiceStatus(invoiceId, status.toEntity())
    }
    
    override suspend fun deleteInvoice(id: Long) {
        invoiceDao.getInvoiceById(id)?.let { invoice ->
            invoiceDao.deleteInvoice(invoice)
        }
    }
}
