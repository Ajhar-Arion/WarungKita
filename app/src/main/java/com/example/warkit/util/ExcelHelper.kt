package com.example.warkit.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.warkit.domain.model.Product
import com.opencsv.CSVReaderBuilder
import com.opencsv.CSVWriterBuilder
import java.io.File
import java.io.FileWriter
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Helper class for importing/exporting inventory data in CSV format
 * CSV files can be opened and edited in Excel, Google Sheets, etc.
 */
object ExcelHelper {
    
    // CSV Header columns
    private val HEADER = arrayOf(
        "Name",           // Required
        "SKU",            // Optional
        "Price",          // Required
        "Stock",          // Required
        "MinStock",       // Optional (default: 5)
        "Category",       // Optional
        "Description"     // Optional
    )
    
    data class ImportResult(
        val successCount: Int,
        val failedCount: Int,
        val errors: List<String>,
        val products: List<Product>
    )
    
    /**
     * Generate a template CSV file that can be edited in Excel
     */
    fun generateTemplate(context: Context): File {
        val templatesDir = File(context.filesDir, "templates")
        if (!templatesDir.exists()) {
            templatesDir.mkdirs()
        }
        
        val templateFile = File(templatesDir, "inventory_template.csv")
        
        FileWriter(templateFile).use { fileWriter ->
            val csvWriter = CSVWriterBuilder(fileWriter)
                .withSeparator(',')
                .build()
            
            // Write header
            csvWriter.writeNext(HEADER)
            
            // Write example rows
            csvWriter.writeNext(arrayOf(
                "Contoh Produk 1",
                "SKU001",
                "50000",
                "100",
                "10",
                "Elektronik",
                "Deskripsi produk pertama"
            ))
            csvWriter.writeNext(arrayOf(
                "Contoh Produk 2",
                "SKU002",
                "75000",
                "50",
                "5",
                "Makanan",
                "Deskripsi produk kedua"
            ))
            csvWriter.writeNext(arrayOf(
                "Contoh Produk 3",
                "",
                "25000",
                "200",
                "",
                "",
                ""
            ))
            
            csvWriter.flush()
        }
        
        return templateFile
    }
    
    /**
     * Share the template file via Intent
     */
    fun shareTemplate(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Template Import Inventory Warkit")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Simpan Template"))
    }
    
    /**
     * Parse CSV file from Uri and return list of products
     */
    fun importFromCsv(context: Context, uri: Uri): ImportResult {
        val products = mutableListOf<Product>()
        val errors = mutableListOf<String>()
        var successCount = 0
        var failedCount = 0
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val result = parseInputStream(inputStream)
                products.addAll(result.products)
                errors.addAll(result.errors)
                successCount = result.successCount
                failedCount = result.failedCount
            }
        } catch (e: Exception) {
            errors.add("Error membuka file: ${e.message}")
            failedCount = 1
        }
        
        return ImportResult(successCount, failedCount, errors, products)
    }
    
    private fun parseInputStream(inputStream: InputStream): ImportResult {
        val products = mutableListOf<Product>()
        val errors = mutableListOf<String>()
        var successCount = 0
        var failedCount = 0
        
        val reader = InputStreamReader(inputStream)
        val csvReader = CSVReaderBuilder(reader)
            .withSkipLines(1) // Skip header
            .build()
        
        var lineNumber = 2 // Start from 2 (header is 1)
        
        csvReader.use { csv ->
            var line: Array<String>?
            while (csv.readNext().also { line = it } != null) {
                val row = line!!
                
                try {
                    // Skip empty rows
                    if (row.all { it.isBlank() }) {
                        lineNumber++
                        continue
                    }
                    
                    val product = parseRow(row, lineNumber)
                    if (product != null) {
                        products.add(product)
                        successCount++
                    } else {
                        errors.add("Baris $lineNumber: Data tidak valid")
                        failedCount++
                    }
                } catch (e: Exception) {
                    errors.add("Baris $lineNumber: ${e.message}")
                    failedCount++
                }
                
                lineNumber++
            }
        }
        
        return ImportResult(successCount, failedCount, errors, products)
    }
    
    private fun parseRow(row: Array<String>, lineNumber: Int): Product? {
        // Minimum columns: Name, SKU, Price, Stock
        if (row.size < 4) {
            throw IllegalArgumentException("Kolom tidak lengkap (minimal 4 kolom)")
        }
        
        val name = row.getOrNull(0)?.trim() ?: ""
        val sku = row.getOrNull(1)?.trim() ?: ""
        val priceStr = row.getOrNull(2)?.trim() ?: ""
        val stockStr = row.getOrNull(3)?.trim() ?: ""
        val minStockStr = row.getOrNull(4)?.trim() ?: "5"
        val category = row.getOrNull(5)?.trim() ?: ""
        val description = row.getOrNull(6)?.trim() ?: ""
        
        // Validate required fields
        if (name.isBlank()) {
            throw IllegalArgumentException("Nama produk tidak boleh kosong")
        }
        
        if (priceStr.isBlank()) {
            throw IllegalArgumentException("Harga tidak boleh kosong")
        }
        
        if (stockStr.isBlank()) {
            throw IllegalArgumentException("Stok tidak boleh kosong")
        }
        
        // Parse numeric values
        val price = priceStr.replace(",", "").replace(".", "").toDoubleOrNull()
            ?: throw IllegalArgumentException("Format harga tidak valid: $priceStr")
        
        val stock = stockStr.toIntOrNull()
            ?: throw IllegalArgumentException("Format stok tidak valid: $stockStr")
        
        val minStock = if (minStockStr.isBlank()) 5 else {
            minStockStr.toIntOrNull()
                ?: throw IllegalArgumentException("Format minimum stok tidak valid: $minStockStr")
        }
        
        return Product(
            name = name,
            sku = sku,
            price = price,
            stock = stock,
            minStock = minStock,
            category = category,
            description = description
        )
    }
    
    /**
     * Export products to CSV file
     */
    fun exportToCsv(context: Context, products: List<Product>): File {
        val exportDir = File(context.filesDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale("id", "ID"))
            .format(java.util.Date())
        val exportFile = File(exportDir, "inventory_export_$timestamp.csv")
        
        FileWriter(exportFile).use { fileWriter ->
            val csvWriter = CSVWriterBuilder(fileWriter)
                .withSeparator(',')
                .build()
            
            // Write header
            csvWriter.writeNext(HEADER)
            
            // Write data rows
            products.forEach { product ->
                csvWriter.writeNext(arrayOf(
                    product.name,
                    product.sku,
                    product.price.toLong().toString(),
                    product.stock.toString(),
                    product.minStock.toString(),
                    product.category,
                    product.description
                ))
            }
            
            csvWriter.flush()
        }
        
        return exportFile
    }
    
    /**
     * Share exported file
     */
    fun shareExportFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Export Inventory Warkit")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Bagikan File"))
    }
    
    // ========== INVOICE EXPORT ==========
    
    private val INVOICE_HEADER = arrayOf(
        "No Invoice",
        "Tanggal",
        "Customer",
        "Total",
        "Status",
        "Notes"
    )
    
    private val INVOICE_ITEMS_HEADER = arrayOf(
        "No Invoice",
        "Nama Produk",
        "Qty",
        "Harga Satuan",
        "Subtotal"
    )
    
    /**
     * Export invoices to CSV file (can be opened in Excel)
     */
    fun exportInvoicesToCsv(
        context: Context,
        invoices: List<com.example.warkit.domain.model.Invoice>,
        includeItems: Boolean = true
    ): File {
        val exportDir = File(context.filesDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        
        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("id", "ID"))
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale("id", "ID"))
            .format(java.util.Date())
        val exportFile = File(exportDir, "transaksi_export_$timestamp.csv")
        
        FileWriter(exportFile).use { fileWriter ->
            val csvWriter = CSVWriterBuilder(fileWriter)
                .withSeparator(',')
                .build()
            
            // Write invoice header
            csvWriter.writeNext(INVOICE_HEADER)
            
            // Write invoice data
            invoices.forEach { invoice ->
                csvWriter.writeNext(arrayOf(
                    invoice.invoiceNumber,
                    dateFormat.format(java.util.Date(invoice.date)),
                    invoice.customerName,
                    invoice.totalAmount.toLong().toString(),
                    invoice.status.name,
                    invoice.notes
                ))
            }
            
            // Add separator and items if requested
            if (includeItems && invoices.any { it.items.isNotEmpty() }) {
                csvWriter.writeNext(arrayOf("")) // Empty row separator
                csvWriter.writeNext(arrayOf("=== DETAIL ITEMS ==="))
                csvWriter.writeNext(INVOICE_ITEMS_HEADER)
                
                invoices.forEach { invoice ->
                    invoice.items.forEach { item ->
                        csvWriter.writeNext(arrayOf(
                            invoice.invoiceNumber,
                            item.productName,
                            item.quantity.toString(),
                            item.unitPrice.toLong().toString(),
                            item.subtotal.toLong().toString()
                        ))
                    }
                }
            }
            
            csvWriter.flush()
        }
        
        return exportFile
    }
    
    /**
     * Get summary statistics for export preview
     */
    fun getExportSummary(invoices: List<com.example.warkit.domain.model.Invoice>): ExportSummary {
        val totalAmount = invoices.sumOf { it.totalAmount }
        val totalItems = invoices.sumOf { it.items.size }
        val byStatus = invoices.groupBy { it.status }.mapValues { it.value.size }
        
        return ExportSummary(
            invoiceCount = invoices.size,
            totalAmount = totalAmount,
            totalItems = totalItems,
            paidCount = byStatus[com.example.warkit.domain.model.InvoiceStatus.PAID] ?: 0,
            pendingCount = byStatus[com.example.warkit.domain.model.InvoiceStatus.PENDING] ?: 0,
            cancelledCount = byStatus[com.example.warkit.domain.model.InvoiceStatus.CANCELLED] ?: 0
        )
    }
    
    data class ExportSummary(
        val invoiceCount: Int,
        val totalAmount: Double,
        val totalItems: Int,
        val paidCount: Int,
        val pendingCount: Int,
        val cancelledCount: Int
    )
}
