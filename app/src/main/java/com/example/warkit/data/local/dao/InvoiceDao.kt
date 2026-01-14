package com.example.warkit.data.local.dao

import androidx.room.*
import com.example.warkit.data.local.entity.InvoiceEntity
import com.example.warkit.data.local.entity.InvoiceItemEntity
import com.example.warkit.data.local.entity.InvoiceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    
    // Invoice queries
    @Query("SELECT * FROM invoices ORDER BY date DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>
    
    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceById(id: Long): InvoiceEntity?
    
    @Query("SELECT * FROM invoices WHERE customerId = :customerId ORDER BY date DESC")
    fun getInvoicesByCustomer(customerId: Long): Flow<List<InvoiceEntity>>
    
    @Query("SELECT * FROM invoices WHERE status = :status ORDER BY date DESC")
    fun getInvoicesByStatus(status: InvoiceStatus): Flow<List<InvoiceEntity>>
    
    @Query("SELECT * FROM invoices WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getInvoicesByDateRange(startDate: Long, endDate: Long): Flow<List<InvoiceEntity>>
    
    @Query("SELECT * FROM invoices WHERE invoiceNumber LIKE :datePrefix || '%' ORDER BY invoiceNumber DESC LIMIT 1")
    suspend fun getLastInvoiceByDatePrefix(datePrefix: String): InvoiceEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long
    
    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)
    
    @Query("UPDATE invoices SET status = :status WHERE id = :invoiceId")
    suspend fun updateInvoiceStatus(invoiceId: Long, status: InvoiceStatus)
    
    @Delete
    suspend fun deleteInvoice(invoice: InvoiceEntity)
    
    // Invoice Items queries
    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getInvoiceItems(invoiceId: Long): List<InvoiceItemEntity>
    
    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    fun getInvoiceItemsFlow(invoiceId: Long): Flow<List<InvoiceItemEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItem(item: InvoiceItemEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>)
    
    @Delete
    suspend fun deleteInvoiceItem(item: InvoiceItemEntity)
    
    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteInvoiceItemsByInvoiceId(invoiceId: Long)
}
