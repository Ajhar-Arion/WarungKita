package com.example.warkit.domain.model

enum class InvoiceStatus {
    PENDING, PAID, CANCELLED
}

data class Invoice(
    val id: Long = 0,
    val invoiceNumber: String,
    val customerId: Long,
    val customerName: String = "",
    val totalAmount: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val status: InvoiceStatus = InvoiceStatus.PENDING,
    val notes: String = "",
    val items: List<InvoiceItem> = emptyList()
)

data class InvoiceItem(
    val id: Long = 0,
    val invoiceId: Long = 0,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double = quantity * unitPrice
)
