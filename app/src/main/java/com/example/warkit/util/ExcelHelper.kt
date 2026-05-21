package com.example.warkit.util

import android.content.ClipData
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.warkit.domain.model.Customer
import com.example.warkit.domain.model.Invoice
import com.example.warkit.domain.model.InvoiceStatus
import com.example.warkit.domain.model.Product
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet
import org.dhatim.fastexcel.reader.ReadableWorkbook
import org.dhatim.fastexcel.reader.ReadingOptions
import org.dhatim.fastexcel.reader.Row
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper class for importing/exporting native XLSX workbooks.
 */
object ExcelHelper {

    const val XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    private val INDONESIAN_LOCALE: Locale = Locale.forLanguageTag("id-ID")

    private val HEADER = arrayOf(
        "Name",
        "SKU",
        "Price",
        "Stock",
        "MinStock",
        "Category",
        "Description"
    )

    private val CUSTOMER_HEADER = arrayOf(
        "Kode Customer",
        "Nama Customer",
        "No. HP",
        "Email",
        "Alamat"
    )

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

    private val CUSTOMER_TOTAL_HEADER = arrayOf(
        "ID Customer",
        "Customer",
        "No. HP",
        "Email",
        "Alamat",
        "Jumlah Transaksi",
        "Jumlah Barang",
        "Total Pembelian"
    )

    private val CUSTOMER_ITEM_RECAP_HEADER = arrayOf(
        "ID Customer",
        "Customer",
        "No. HP",
        "Email",
        "Alamat",
        "Nama Barang",
        "Total Qty",
        "Total Pembelian"
    )

    private val CUSTOMER_PURCHASE_DETAIL_HEADER = arrayOf(
        "No Invoice",
        "Tanggal",
        "Status",
        "Nama Barang",
        "Qty",
        "Harga Satuan",
        "Subtotal Barang",
        "Total Invoice",
        "Catatan"
    )

    data class ImportResult(
        val successCount: Int,
        val failedCount: Int,
        val errors: List<String>,
        val products: List<Product>
    )

    data class CustomerImportResult(
        val successCount: Int,
        val failedCount: Int,
        val errors: List<String>,
        val customers: List<Customer>
    )

    data class ExportSummary(
        val invoiceCount: Int,
        val totalAmount: Double,
        val totalItems: Int,
        val paidCount: Int,
        val pendingCount: Int,
        val cancelledCount: Int
    )

    fun generateTemplate(context: Context): File {
        val templatesDir = ensureDir(context.filesDir, "templates")
        val templateFile = File(templatesDir, "inventory_template.xlsx")

        writeWorkbook(templateFile) { workbook ->
            val worksheet = workbook.newWorksheet("Inventory")
            writeHeader(worksheet, HEADER)
            writeRow(
                worksheet,
                1,
                listOf(
                    "Contoh Produk 1",
                    "SKU001",
                    50000,
                    100,
                    10,
                    "Elektronik",
                    "Deskripsi produk pertama"
                )
            )
            writeRow(
                worksheet,
                2,
                listOf(
                    "Contoh Produk 2",
                    "SKU002",
                    75000,
                    50,
                    5,
                    "Makanan",
                    "Deskripsi produk kedua"
                )
            )
            writeRow(
                worksheet,
                3,
                listOf(
                    "Contoh Produk 3",
                    "",
                    25000,
                    200,
                    "",
                    "",
                    ""
                )
            )
            formatMoneyColumn(worksheet, 2, 3)
        }

        return templateFile
    }

    fun openTemplate(context: Context, file: File) {
        openExcelFile(context, file, "Buka atau Edit Template Inventory")
    }

    fun generateCustomerTemplate(context: Context): File {
        val templatesDir = ensureDir(context.filesDir, "templates")
        val templateFile = File(templatesDir, "customer_template.xlsx")

        writeWorkbook(templateFile) { workbook ->
            val worksheet = workbook.newWorksheet("Customer")
            writeHeader(worksheet, CUSTOMER_HEADER)
            writeRow(
                worksheet,
                1,
                listOf("CUST001", "Budi", "081234567890", "budi@example.com", "Jl. Melati No. 10")
            )
            writeRow(
                worksheet,
                2,
                listOf("CUST002", "Siti", "081322223333", "siti@example.com", "Jl. Mawar No. 5")
            )
            writeRow(
                worksheet,
                3,
                listOf("CUST003", "Andi", "082111112222", "", "Jl. Kenanga No. 7")
            )
        }

        return templateFile
    }

    fun openCustomerTemplate(context: Context, file: File) {
        openExcelFile(context, file, "Buka atau Edit Template Customer")
    }

    fun saveCustomerTemplate(context: Context, targetUri: Uri) {
        val templateFile = generateCustomerTemplate(context)
        val outputStream = context.contentResolver.openOutputStream(targetUri)
            ?: throw IllegalStateException("Gagal membuka lokasi penyimpanan")

        outputStream.use { output ->
            templateFile.inputStream().use { input ->
                input.copyTo(output)
            }
        }

        openExcelUri(
            context = context,
            uri = targetUri,
            mimeType = XLSX_MIME_TYPE,
            chooserTitle = "Buka atau Edit Template Customer"
        )
    }

    fun importFromExcel(context: Context, uri: Uri): ImportResult {
        val products = mutableListOf<Product>()
        val errors = mutableListOf<String>()
        var successCount = 0
        var failedCount = 0

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val result = parseProductWorkbook(inputStream)
                products.addAll(result.products)
                errors.addAll(result.errors)
                successCount = result.successCount
                failedCount = result.failedCount
            } ?: run {
                errors.add("Error membuka file: stream kosong")
                failedCount = 1
            }
        } catch (e: Exception) {
            errors.add("Error membuka file Excel: ${e.message ?: "Pastikan file berformat .xlsx"}")
            failedCount = 1
        }

        return ImportResult(successCount, failedCount, errors, products)
    }

    fun importCustomersFromExcel(context: Context, uri: Uri): CustomerImportResult {
        val customers = mutableListOf<Customer>()
        val errors = mutableListOf<String>()
        var successCount = 0
        var failedCount = 0

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val result = parseCustomerWorkbook(inputStream)
                customers.addAll(result.customers)
                errors.addAll(result.errors)
                successCount = result.successCount
                failedCount = result.failedCount
            } ?: run {
                errors.add("Error membuka file: stream kosong")
                failedCount = 1
            }
        } catch (e: Exception) {
            errors.add("Error membuka file Excel: ${e.message ?: "Pastikan file berformat .xlsx"}")
            failedCount = 1
        }

        return CustomerImportResult(successCount, failedCount, errors, customers)
    }

    fun exportInventoryToExcel(context: Context, products: List<Product>): File {
        val exportDir = ensureDir(context.filesDir, "exports")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", INDONESIAN_LOCALE).format(Date())
        val exportFile = File(exportDir, "inventory_export_$timestamp.xlsx")

        writeWorkbook(exportFile) { workbook ->
            val worksheet = workbook.newWorksheet("Inventory")
            writeHeader(worksheet, HEADER)
            products.forEachIndexed { index, product ->
                writeRow(
                    worksheet,
                    index + 1,
                    listOf(
                        product.name,
                        product.sku,
                        product.price,
                        product.stock,
                        product.minStock,
                        product.category,
                        product.description
                    )
                )
            }
            formatMoneyColumn(worksheet, 2, products.size)
        }

        return exportFile
    }

    fun openExportFile(context: Context, file: File) {
        openExcelFile(context, file, "Buka atau Edit File Excel")
    }

    fun exportTransactionHistoryToExcel(
        context: Context,
        invoices: List<Invoice>,
        periodLabel: String,
        customers: List<Customer> = emptyList(),
        startDateMillis: Long? = null,
        endDateMillis: Long? = null
    ): File {
        val exportDir = ensureDir(context.filesDir, "exports")
        val locale = INDONESIAN_LOCALE
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", locale).format(Date())
        val exportFile = File(
            exportDir,
            "history_transaksi_${fileSafeLabel(periodLabel)}_$timestamp.xlsx"
        )

        writeWorkbook(exportFile) { workbook ->
            val summary = workbook.newWorksheet("Ringkasan")
            val customerMap = customers.associateBy { it.id }
            writeHistorySummary(summary, invoices, periodLabel, startDateMillis, endDateMillis)
            writeCustomerTotalsSheet(workbook, invoices, customerMap)
            writeCustomerItemRecapSheet(workbook, invoices, customerMap)
            writeCustomerPurchaseDetailSheet(workbook, invoices, customerMap, periodLabel, startDateMillis, endDateMillis)
        }

        return exportFile
    }

    fun openTransactionHistoryFile(context: Context, file: File) {
        openExcelFile(context, file, "Buka atau Edit History Transaksi")
    }

    fun exportInvoicesToExcel(
        context: Context,
        invoices: List<Invoice>,
        includeItems: Boolean = true
    ): File {
        val exportDir = ensureDir(context.filesDir, "exports")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", INDONESIAN_LOCALE).format(Date())
        val exportFile = File(exportDir, "transaksi_export_$timestamp.xlsx")

        writeWorkbook(exportFile) { workbook ->
            writeInvoiceSheet(workbook, invoices)
            if (includeItems) {
                writeInvoiceItemSheet(workbook, invoices)
            }
        }

        return exportFile
    }

    fun getExportSummary(invoices: List<Invoice>): ExportSummary {
        val totalAmount = invoices.sumOf { it.totalAmount }
        val totalItems = invoices.sumOf { it.items.size }
        val byStatus = invoices.groupBy { it.status }.mapValues { it.value.size }

        return ExportSummary(
            invoiceCount = invoices.size,
            totalAmount = totalAmount,
            totalItems = totalItems,
            paidCount = byStatus[InvoiceStatus.PAID] ?: 0,
            pendingCount = byStatus[InvoiceStatus.PENDING] ?: 0,
            cancelledCount = byStatus[InvoiceStatus.CANCELLED] ?: 0
        )
    }

    private fun parseProductWorkbook(inputStream: InputStream): ImportResult {
        val products = mutableListOf<Product>()
        val errors = mutableListOf<String>()
        var successCount = 0
        var failedCount = 0

        readDataRows(inputStream) { row ->
            val lineNumber = row.getRowNum()
            try {
                if (isBlankRow(row, HEADER.size)) {
                    return@readDataRows
                }

                products.add(parseProductRow(row))
                successCount++
            } catch (e: Exception) {
                errors.add("Baris $lineNumber: ${e.message}")
                failedCount++
            }
        }

        return ImportResult(successCount, failedCount, errors, products)
    }

    private fun parseCustomerWorkbook(inputStream: InputStream): CustomerImportResult {
        val customers = mutableListOf<Customer>()
        val errors = mutableListOf<String>()
        var successCount = 0
        var failedCount = 0

        readDataRows(inputStream) { row ->
            val lineNumber = row.getRowNum()
            try {
                if (isBlankRow(row, CUSTOMER_HEADER.size)) {
                    return@readDataRows
                }

                customers.add(parseCustomerRow(row))
                successCount++
            } catch (e: Exception) {
                errors.add("Baris $lineNumber: ${e.message}")
                failedCount++
            }
        }

        return CustomerImportResult(successCount, failedCount, errors, customers)
    }

    private fun readDataRows(inputStream: InputStream, onRow: (Row) -> Unit) {
        val workbook = ReadableWorkbook(inputStream, ReadingOptions(true, true))
        try {
            val sheet = workbook.getFirstSheet()
            val rows = sheet.openStream()
            try {
                val iterator = rows.iterator()
                var headerSkipped = false
                while (iterator.hasNext()) {
                    val row = iterator.next()
                    if (!headerSkipped) {
                        headerSkipped = true
                        continue
                    }
                    onRow(row)
                }
            } finally {
                rows.close()
            }
        } finally {
            workbook.close()
        }
    }

    private fun parseProductRow(row: Row): Product {
        val name = textCell(row, 0)
        val sku = textCell(row, 1)
        val priceText = textCell(row, 2)
        val stockText = textCell(row, 3)
        val minStockText = textCell(row, 4)
        val category = textCell(row, 5)
        val description = textCell(row, 6)

        if (name.isBlank()) {
            throw IllegalArgumentException("Nama produk tidak boleh kosong")
        }

        val price = numberCell(row, 2)
            ?: parseDecimal(priceText)
            ?: throw IllegalArgumentException("Format harga tidak valid: $priceText")

        val stock = intCell(row, 3, stockText, "stok")
        val minStock = if (minStockText.isBlank() && numberCell(row, 4) == null) {
            5
        } else {
            intCell(row, 4, minStockText, "minimum stok")
        }

        if (stock < 0) {
            throw IllegalArgumentException("Stok tidak boleh negatif")
        }
        if (minStock < 0) {
            throw IllegalArgumentException("Minimum stok tidak boleh negatif")
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

    private fun parseCustomerRow(row: Row): Customer {
        val name = textCell(row, 1)
        val phone = textCell(row, 2)
        val email = textCell(row, 3)
        val address = textCell(row, 4)

        if (name.isBlank()) {
            throw IllegalArgumentException("Nama customer tidak boleh kosong")
        }

        return Customer(
            name = name,
            phone = phone,
            email = email,
            address = address
        )
    }

    private fun writeHistorySummary(
        worksheet: Worksheet,
        invoices: List<Invoice>,
        periodLabel: String,
        startDateMillis: Long?,
        endDateMillis: Long?
    ) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", INDONESIAN_LOCALE)
        val totalAmount = invoices.sumOf { it.totalAmount }
        val customerCount = customerHistoryGroups(invoices).size
        writeHeader(worksheet, arrayOf("Metrik", "Nilai"))
        writeRow(worksheet, 1, listOf("Periode", periodLabel))
        writeRow(
            worksheet,
            2,
            listOf("Tanggal Mulai", startDateMillis?.let { dateFormat.format(Date(it)) }.orEmpty())
        )
        writeRow(
            worksheet,
            3,
            listOf("Tanggal Selesai", endDateMillis?.let { dateFormat.format(Date(it)) }.orEmpty())
        )
        writeRow(worksheet, 4, listOf("Jumlah Transaksi", invoices.size))
        writeRow(worksheet, 5, listOf("Jumlah Customer", customerCount))
        writeRow(worksheet, 6, listOf("Total Pembelian", totalAmount))
        worksheet.style(6, 1).format("#,##0").set()
    }

    private fun writeCustomerTotalsSheet(
        workbook: Workbook,
        invoices: List<Invoice>,
        customerMap: Map<Long, Customer>
    ) {
        val worksheet = workbook.newWorksheet("Total Customer")
        writeHeader(worksheet, CUSTOMER_TOTAL_HEADER)

        val groups = customerHistoryGroups(invoices)
        groups.forEachIndexed { index, group ->
            val customer = customerMap[group.customerId]
            writeRow(
                worksheet,
                index + 1,
                listOf(
                    group.customerId,
                    customerName(group, customer),
                    customer?.phone.orEmpty(),
                    customer?.email.orEmpty(),
                    customer?.address.orEmpty(),
                    group.invoices.size,
                    group.invoices.sumOf { invoice -> invoice.items.sumOf { it.quantity } },
                    group.invoices.sumOf { it.totalAmount }
                )
            )
        }

        formatMoneyColumn(worksheet, 7, groups.size)
    }

    private fun writeCustomerItemRecapSheet(
        workbook: Workbook,
        invoices: List<Invoice>,
        customerMap: Map<Long, Customer>
    ) {
        val worksheet = workbook.newWorksheet("Rekap Barang")
        writeHeader(worksheet, CUSTOMER_ITEM_RECAP_HEADER)

        var rowIndex = 1
        customerHistoryGroups(invoices).forEach { group ->
            val customer = customerMap[group.customerId]
            val displayName = customerName(group, customer)
            val itemGroups = group.invoices
                .flatMap { it.items }
                .groupBy { it.productName.ifBlank { "Barang" } }
                .toSortedMap(String.CASE_INSENSITIVE_ORDER)

            if (itemGroups.isEmpty()) {
                writeRow(
                    worksheet,
                    rowIndex,
                    listOf(
                        group.customerId,
                        displayName,
                        customer?.phone.orEmpty(),
                        customer?.email.orEmpty(),
                        customer?.address.orEmpty(),
                        "-",
                        0,
                        0.0
                    )
                )
                rowIndex++
            } else {
                itemGroups.forEach { (productName, items) ->
                    writeRow(
                        worksheet,
                        rowIndex,
                        listOf(
                            group.customerId,
                            displayName,
                            customer?.phone.orEmpty(),
                            customer?.email.orEmpty(),
                            customer?.address.orEmpty(),
                            productName,
                            items.sumOf { it.quantity },
                            items.sumOf { it.subtotal }
                        )
                    )
                    rowIndex++
                }
            }
        }

        formatMoneyColumn(worksheet, 7, rowIndex - 1)
    }

    private fun writeCustomerPurchaseDetailSheet(
        workbook: Workbook,
        invoices: List<Invoice>,
        customerMap: Map<Long, Customer>,
        periodLabel: String,
        startDateMillis: Long?,
        endDateMillis: Long?
    ) {
        val worksheet = workbook.newWorksheet("Detail Per Customer")
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", INDONESIAN_LOCALE)
        var rowIndex = 0
        customerHistoryGroups(invoices).forEach { group ->
            val customer = customerMap[group.customerId]
            val itemCount = group.invoices.sumOf { invoice -> invoice.items.sumOf { it.quantity } }
            val totalAmount = group.invoices.sumOf { it.totalAmount }

            writeCustomerBlockHeader(
                worksheet = worksheet,
                rowIndex = rowIndex,
                periodLabel = periodLabel,
                startDateMillis = startDateMillis,
                endDateMillis = endDateMillis,
                dateFormat = dateFormat,
                group = group,
                customer = customer,
                itemCount = itemCount,
                totalAmount = totalAmount
            )
            rowIndex += 13

            writeHeaderAt(worksheet, rowIndex, CUSTOMER_PURCHASE_DETAIL_HEADER)
            rowIndex++
            val firstDetailRow = rowIndex

            group.invoices.forEach { invoice ->
                if (invoice.items.isEmpty()) {
                    writeRow(
                        worksheet,
                        rowIndex,
                        listOf(
                            invoice.invoiceNumber,
                            dateFormat.format(Date(invoice.date)),
                            invoiceStatusLabel(invoice.status),
                            "-",
                            0,
                            0.0,
                            0.0,
                            invoice.totalAmount,
                            invoice.notes
                        )
                    )
                    rowIndex++
                } else {
                    invoice.items.forEach { item ->
                        writeRow(
                            worksheet,
                            rowIndex,
                            listOf(
                                invoice.invoiceNumber,
                                dateFormat.format(Date(invoice.date)),
                                invoiceStatusLabel(invoice.status),
                                item.productName,
                                item.quantity,
                                item.unitPrice,
                                item.subtotal,
                                invoice.totalAmount,
                                invoice.notes
                            )
                        )
                        rowIndex++
                    }
                }
            }

            val detailRowCount = rowIndex - firstDetailRow
            formatMoneyColumn(worksheet, 5, rowIndex - 1)
            formatMoneyColumn(worksheet, 6, rowIndex - 1)
            formatMoneyColumn(worksheet, 7, rowIndex - 1)

            if (detailRowCount > 0) {
                writeRow(worksheet, rowIndex, listOf("", "", "", "", "TOTAL CUSTOMER", "", totalAmount, totalAmount, ""))
                worksheet.range(rowIndex, 0, rowIndex, CUSTOMER_PURCHASE_DETAIL_HEADER.lastIndex)
                    .style()
                    .bold()
                    .fillColor("FFF2CC")
                    .set()
                worksheet.style(rowIndex, 6).format("#,##0").set()
                worksheet.style(rowIndex, 7).format("#,##0").set()
                rowIndex++
            }

            rowIndex += 2
        }
    }

    private fun writeCustomerBlockHeader(
        worksheet: Worksheet,
        rowIndex: Int,
        periodLabel: String,
        startDateMillis: Long?,
        endDateMillis: Long?,
        dateFormat: SimpleDateFormat,
        group: CustomerHistoryGroup,
        customer: Customer?,
        itemCount: Int,
        totalAmount: Double
    ) {
        val displayName = customerName(group, customer)
        val periodStart = startDateMillis?.let { dateFormat.format(Date(it)) }.orEmpty()
        val periodEnd = endDateMillis?.let { dateFormat.format(Date(it)) }.orEmpty()

        writeRow(worksheet, rowIndex, listOf("DATA CUSTOMER", displayName))
        worksheet.range(rowIndex, 0, rowIndex, CUSTOMER_PURCHASE_DETAIL_HEADER.lastIndex)
            .style()
            .bold()
            .fillColor("D9EAD3")
            .set()

        writeRow(worksheet, rowIndex + 1, listOf("ID Customer", group.customerId))
        writeRow(worksheet, rowIndex + 2, listOf("Nama Customer", displayName))
        writeRow(worksheet, rowIndex + 3, listOf("No. HP", customer?.phone.orEmpty()))
        writeRow(worksheet, rowIndex + 4, listOf("Email", customer?.email.orEmpty()))
        writeRow(worksheet, rowIndex + 5, listOf("Alamat", customer?.address.orEmpty()))
        writeRow(worksheet, rowIndex + 6, listOf("Periode", periodLabel))
        writeRow(worksheet, rowIndex + 7, listOf("Tanggal Mulai", periodStart))
        writeRow(worksheet, rowIndex + 8, listOf("Tanggal Selesai", periodEnd))
        writeRow(worksheet, rowIndex + 9, listOf("Jumlah Transaksi", group.invoices.size))
        writeRow(worksheet, rowIndex + 10, listOf("Jumlah Barang", itemCount))
        writeRow(worksheet, rowIndex + 11, listOf("Total Pembelian", totalAmount))
        worksheet.style(rowIndex + 11, 1).format("#,##0").set()
    }

    private fun writeInvoiceSheet(workbook: Workbook, invoices: List<Invoice>) {
        val worksheet = workbook.newWorksheet("Transaksi")
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", INDONESIAN_LOCALE)
        writeHeader(worksheet, INVOICE_HEADER)

        invoices.forEachIndexed { index, invoice ->
            writeRow(
                worksheet,
                index + 1,
                listOf(
                    invoice.invoiceNumber,
                    dateFormat.format(Date(invoice.date)),
                    invoice.customerName.ifBlank { "Customer" },
                    invoice.totalAmount,
                    invoiceStatusLabel(invoice.status),
                    invoice.notes
                )
            )
        }

        formatMoneyColumn(worksheet, 3, invoices.size)
    }

    private fun writeInvoiceItemSheet(workbook: Workbook, invoices: List<Invoice>) {
        val items = invoices.flatMap { invoice ->
            invoice.items.map { item -> invoice to item }
        }
        if (items.isEmpty()) {
            return
        }

        val worksheet = workbook.newWorksheet("Detail Items")
        writeHeader(worksheet, INVOICE_ITEMS_HEADER)
        items.forEachIndexed { index, (invoice, item) ->
            writeRow(
                worksheet,
                index + 1,
                listOf(
                    invoice.invoiceNumber,
                    item.productName,
                    item.quantity,
                    item.unitPrice,
                    item.subtotal
                )
            )
        }
        formatMoneyColumn(worksheet, 3, items.size)
        formatMoneyColumn(worksheet, 4, items.size)
    }

    private data class CustomerHistoryGroup(
        val customerId: Long,
        val customerName: String,
        val invoices: List<Invoice>
    )

    private data class CustomerHistoryKey(
        val customerId: Long,
        val customerName: String
    )

    private fun customerHistoryGroups(invoices: List<Invoice>): List<CustomerHistoryGroup> {
        return invoices
            .groupBy { invoice ->
                CustomerHistoryKey(
                    customerId = invoice.customerId,
                    customerName = customerDisplayName(invoice)
                )
            }
            .map { (key, customerInvoices) ->
                CustomerHistoryGroup(
                    customerId = key.customerId,
                    customerName = key.customerName,
                    invoices = customerInvoices.sortedByDescending { it.date }
                )
            }
            .sortedWith(
                Comparator { left, right ->
                    val nameComparison = String.CASE_INSENSITIVE_ORDER.compare(
                        left.customerName,
                        right.customerName
                    )
                    if (nameComparison != 0) {
                        nameComparison
                    } else {
                        left.customerId.compareTo(right.customerId)
                    }
                }
            )
    }

    private fun customerDisplayName(invoice: Invoice): String {
        return invoice.customerName.ifBlank {
            if (invoice.customerId > 0) {
                "Customer #${invoice.customerId}"
            } else {
                "Customer"
            }
        }
    }

    private fun customerName(group: CustomerHistoryGroup, customer: Customer?): String {
        return customer?.name?.takeIf { it.isNotBlank() } ?: group.customerName
    }

    private fun writeWorkbook(file: File, block: (Workbook) -> Unit) {
        FileOutputStream(file).use { output ->
            val workbook = Workbook(output, "WarungKita", "1.0")
            try {
                workbook.setGlobalDefaultFont("Calibri", 11.0)
                block(workbook)
            } finally {
                workbook.close()
            }
        }
    }

    private fun writeHeader(worksheet: Worksheet, headers: Array<String>) {
        writeHeaderAt(worksheet, 0, headers)
        worksheet.freezePane(1, 0)
        worksheet.setAutoFilter(0, 0, headers.lastIndex)
    }

    private fun writeHeaderAt(worksheet: Worksheet, rowIndex: Int, headers: Array<String>) {
        headers.forEachIndexed { column, value ->
            worksheet.value(rowIndex, column, value)
        }
        worksheet.range(rowIndex, 0, rowIndex, headers.lastIndex)
            .style()
            .bold()
            .fillColor("D9EAD3")
            .horizontalAlignment("center")
            .set()
    }

    private fun writeRow(worksheet: Worksheet, rowIndex: Int, values: List<Any?>) {
        values.forEachIndexed { column, value ->
            when (value) {
                null -> worksheet.value(rowIndex, column, "")
                is Int -> worksheet.value(rowIndex, column, value)
                is Long -> worksheet.value(rowIndex, column, value)
                is Float -> worksheet.value(rowIndex, column, value.toDouble())
                is Double -> worksheet.value(rowIndex, column, value)
                is Boolean -> worksheet.value(rowIndex, column, value)
                else -> worksheet.value(rowIndex, column, value.toString())
            }
        }
    }

    private fun formatMoneyColumn(worksheet: Worksheet, column: Int, rowCount: Int) {
        if (rowCount <= 0) return

        for (row in 1..rowCount) {
            worksheet.style(row, column).format("#,##0").set()
        }
    }

    private fun textCell(row: Row, column: Int): String {
        return row.getCellText(column)?.trim().orEmpty()
    }

    private fun numberCell(row: Row, column: Int): Double? {
        val number = row.getCellAsNumber(column)
        return if (number.isPresent) number.get().toDouble() else null
    }

    private fun intCell(row: Row, column: Int, rawText: String, label: String): Int {
        numberCell(row, column)?.let { return it.toInt() }

        val parsed = parseDecimal(rawText)
            ?: throw IllegalArgumentException("Format $label tidak valid: $rawText")
        return parsed.toInt()
    }

    private fun parseDecimal(raw: String): Double? {
        val cleaned = raw
            .trim()
            .replace(Regex("[^0-9,.-]"), "")

        if (cleaned.isBlank() || cleaned == "-" || cleaned == "." || cleaned == ",") {
            return null
        }

        val normalized = when {
            cleaned.contains(',') && cleaned.contains('.') -> {
                if (cleaned.lastIndexOf(',') > cleaned.lastIndexOf('.')) {
                    cleaned.replace(".", "").replace(",", ".")
                } else {
                    cleaned.replace(",", "")
                }
            }
            cleaned.contains(',') -> normalizeSingleSeparator(cleaned, ',')
            cleaned.contains('.') -> normalizeSingleSeparator(cleaned, '.')
            else -> cleaned
        }

        return normalized.toDoubleOrNull()
    }

    private fun normalizeSingleSeparator(value: String, separator: Char): String {
        val separatorCount = value.count { it == separator }
        if (separatorCount > 1) {
            return value.replace(separator.toString(), "")
        }

        val fraction = value.substringAfterLast(separator)
        return if (fraction.length == 3) {
            value.replace(separator.toString(), "")
        } else if (separator == ',') {
            value.replace(",", ".")
        } else {
            value
        }
    }

    private fun isBlankRow(row: Row, columnCount: Int): Boolean {
        return (0 until columnCount).all { column -> textCell(row, column).isBlank() }
    }

    private fun ensureDir(parent: File, name: String): File {
        return File(parent, name).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private fun openExcelFile(context: Context, file: File, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        openExcelUri(
            context = context,
            uri = uri,
            mimeType = excelMimeType(file),
            chooserTitle = chooserTitle,
            fileName = file.name
        )
    }

    private fun openExcelUri(
        context: Context,
        uri: Uri,
        mimeType: String,
        chooserTitle: String,
        fileName: String? = null
    ) {
        fun buildOpenIntent(): Intent {
            return Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                clipData = ClipData.newUri(context.contentResolver, fileName ?: "excel_file", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                fileName?.let { putExtra(Intent.EXTRA_TITLE, it) }
            }
        }

        fun buildShareIntent(): Intent {
            return Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(context.contentResolver, fileName ?: "excel_file", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                fileName?.let {
                    putExtra(Intent.EXTRA_TITLE, it)
                    putExtra(Intent.EXTRA_SUBJECT, it)
                }
            }
        }

        val openIntent = buildOpenIntent()
        val shareIntent = buildShareIntent()

        AlertDialog.Builder(context)
            .setTitle("File Excel Siap")
            .setMessage(fileName?.let { "Pilih tindakan untuk $it" } ?: "Pilih tindakan untuk file Excel")
            .setPositiveButton("Buka") { _, _ ->
                launchExcelIntent(
                    context = context,
                    intent = Intent.createChooser(openIntent, chooserTitle),
                    failureMessage = "Tidak ada aplikasi untuk membuka file Excel."
                )
            }
            .setNegativeButton("Bagikan") { _, _ ->
                launchExcelIntent(
                    context = context,
                    intent = Intent.createChooser(shareIntent, "Bagikan File Excel"),
                    failureMessage = "Tidak ada aplikasi untuk membagikan file Excel."
                )
            }
            .setNeutralButton("Batal", null)
            .show()
    }

    private fun launchExcelIntent(context: Context, intent: Intent, failureMessage: String) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, failureMessage, Toast.LENGTH_LONG).show()
        } catch (e: SecurityException) {
            Toast.makeText(context, "Aplikasi tidak memiliki izin untuk membuka file Excel.", Toast.LENGTH_LONG).show()
        }
    }

    private fun excelMimeType(file: File): String {
        return when (file.extension.lowercase(Locale.ROOT)) {
            "xlsx" -> XLSX_MIME_TYPE
            else -> XLSX_MIME_TYPE
        }
    }

    private fun invoiceStatusLabel(status: InvoiceStatus): String {
        return when (status) {
            InvoiceStatus.PENDING -> "Pending"
            InvoiceStatus.PAID -> "Lunas"
            InvoiceStatus.CANCELLED -> "Batal"
        }
    }

    private fun fileSafeLabel(label: String): String {
        return label
            .lowercase(INDONESIAN_LOCALE)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
    }
}
