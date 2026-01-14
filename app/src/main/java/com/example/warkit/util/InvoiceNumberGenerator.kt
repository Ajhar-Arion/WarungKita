package com.example.warkit.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Generates invoice numbers in format: INV-YYYYMMDD-XXX
 * Example: INV-20260113-001, INV-20260113-002
 */
object InvoiceNumberGenerator {
    
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    
    fun getDatePrefix(): String {
        return LocalDate.now().format(dateFormatter)
    }
    
    fun generate(lastInvoiceNumber: String?): String {
        val dateStr = getDatePrefix()
        
        val sequence = if (lastInvoiceNumber != null) {
            // Extract sequence from last invoice number (last 3 digits)
            val lastSeq = lastInvoiceNumber.takeLast(3).toIntOrNull() ?: 0
            lastSeq + 1
        } else {
            1
        }
        
        return "INV-$dateStr-${sequence.toString().padStart(3, '0')}"
    }
}
