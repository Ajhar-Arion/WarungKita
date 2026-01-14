package com.example.warkit.presentation.invoice

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.warkit.domain.model.Invoice

/**
 * Simple PDF generator using Android's built-in PdfDocument
 * No external library needed
 */
object PdfGenerator {
    
    private val priceFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))
    
    fun generateInvoicePdf(context: Context, invoice: Invoice): File {
        val document = PdfDocument()
        
        // A4 size in points (72 dpi)
        val pageWidth = 595
        val pageHeight = 842
        
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        
        // Paints
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            isFakeBoldText = true
        }
        
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            isFakeBoldText = true
        }
        
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }
        
        val smallPaint = Paint().apply {
            color = Color.GRAY
            textSize = 10f
        }
        
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        
        var y = 50f
        val marginLeft = 40f
        val marginRight = pageWidth - 40f
        
        // Header
        canvas.drawText("INVOICE", marginLeft, y, titlePaint)
        y += 30f
        
        canvas.drawText(invoice.invoiceNumber, marginLeft, y, headerPaint)
        y += 20f
        
        canvas.drawText("Tanggal: ${dateFormat.format(Date(invoice.date))}", marginLeft, y, textPaint)
        y += 15f
        
        canvas.drawText("Status: ${invoice.status.name}", marginLeft, y, textPaint)
        y += 30f
        
        // Line
        canvas.drawLine(marginLeft, y, marginRight, y, linePaint)
        y += 20f
        
        // Customer info
        canvas.drawText("CUSTOMER", marginLeft, y, headerPaint)
        y += 18f
        canvas.drawText(invoice.customerName, marginLeft, y, textPaint)
        y += 30f
        
        // Line
        canvas.drawLine(marginLeft, y, marginRight, y, linePaint)
        y += 20f
        
        // Items header
        canvas.drawText("ITEMS", marginLeft, y, headerPaint)
        y += 20f
        
        // Table header
        canvas.drawText("Produk", marginLeft, y, smallPaint)
        canvas.drawText("Qty", 350f, y, smallPaint)
        canvas.drawText("Harga", 400f, y, smallPaint)
        canvas.drawText("Subtotal", 480f, y, smallPaint)
        y += 15f
        
        canvas.drawLine(marginLeft, y, marginRight, y, linePaint)
        y += 15f
        
        // Items
        for (item in invoice.items) {
            // Truncate long product names
            val productName = if (item.productName.length > 40) {
                item.productName.take(37) + "..."
            } else {
                item.productName
            }
            
            canvas.drawText(productName, marginLeft, y, textPaint)
            canvas.drawText(item.quantity.toString(), 350f, y, textPaint)
            canvas.drawText(formatPrice(item.unitPrice), 400f, y, textPaint)
            canvas.drawText(formatPrice(item.subtotal), 480f, y, textPaint)
            y += 18f
        }
        
        y += 10f
        canvas.drawLine(marginLeft, y, marginRight, y, linePaint)
        y += 25f
        
        // Total
        canvas.drawText("TOTAL", 400f, y, headerPaint)
        canvas.drawText(priceFormat.format(invoice.totalAmount), 480f, y, headerPaint)
        y += 30f
        
        // Notes
        if (invoice.notes.isNotEmpty()) {
            y += 10f
            canvas.drawText("Catatan:", marginLeft, y, smallPaint)
            y += 15f
            canvas.drawText(invoice.notes, marginLeft, y, textPaint)
        }
        
        // Footer
        y = pageHeight - 50f
        canvas.drawText("Terima kasih atas pembelian Anda", marginLeft, y, smallPaint)
        
        document.finishPage(page)
        
        // Save to file
        val fileName = "Invoice_${invoice.invoiceNumber.replace("-", "_")}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        
        FileOutputStream(file).use { outputStream ->
            document.writeTo(outputStream)
        }
        
        document.close()
        
        return file
    }
    
    private fun formatPrice(price: Double): String {
        return priceFormat.format(price).replace("Rp", "").trim()
    }
    
    fun shareInvoicePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Share Invoice"))
    }
}
