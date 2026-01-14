package com.example.warkit.data.repository

import com.example.warkit.data.local.entity.CustomerEntity
import com.example.warkit.data.local.entity.ProductEntity
import com.example.warkit.data.local.entity.InvoiceEntity
import com.example.warkit.data.local.entity.InvoiceItemEntity
import com.example.warkit.data.local.entity.InvoiceStatus as EntityInvoiceStatus
import com.example.warkit.domain.model.Customer
import com.example.warkit.domain.model.Product
import com.example.warkit.domain.model.Invoice
import com.example.warkit.domain.model.InvoiceItem
import com.example.warkit.domain.model.InvoiceStatus

// Customer Mappers
fun CustomerEntity.toDomain() = Customer(
    id = id,
    name = name,
    phone = phone,
    email = email,
    address = address,
    photoPath = photoPath,
    createdAt = createdAt
)

fun Customer.toEntity() = CustomerEntity(
    id = id,
    name = name,
    phone = phone,
    email = email,
    address = address,
    photoPath = photoPath,
    createdAt = createdAt
)

// Product Mappers
fun ProductEntity.toDomain() = Product(
    id = id,
    name = name,
    sku = sku,
    price = price,
    stock = stock,
    minStock = minStock,
    category = category,
    description = description,
    createdAt = createdAt
)

fun Product.toEntity() = ProductEntity(
    id = id,
    name = name,
    sku = sku,
    price = price,
    stock = stock,
    minStock = minStock,
    category = category,
    description = description,
    createdAt = createdAt
)

// Invoice Status Mappers
fun EntityInvoiceStatus.toDomain() = when (this) {
    EntityInvoiceStatus.PENDING -> InvoiceStatus.PENDING
    EntityInvoiceStatus.PAID -> InvoiceStatus.PAID
    EntityInvoiceStatus.CANCELLED -> InvoiceStatus.CANCELLED
}

fun InvoiceStatus.toEntity() = when (this) {
    InvoiceStatus.PENDING -> EntityInvoiceStatus.PENDING
    InvoiceStatus.PAID -> EntityInvoiceStatus.PAID
    InvoiceStatus.CANCELLED -> EntityInvoiceStatus.CANCELLED
}

// Invoice Mappers
fun InvoiceEntity.toDomain(items: List<InvoiceItem> = emptyList(), customerName: String = "") = Invoice(
    id = id,
    invoiceNumber = invoiceNumber,
    customerId = customerId,
    customerName = customerName,
    totalAmount = totalAmount,
    date = date,
    status = status.toDomain(),
    notes = notes,
    items = items
)

fun Invoice.toEntity() = InvoiceEntity(
    id = id,
    invoiceNumber = invoiceNumber,
    customerId = customerId,
    totalAmount = totalAmount,
    date = date,
    status = status.toEntity(),
    notes = notes
)

// Invoice Item Mappers
fun InvoiceItemEntity.toDomain() = InvoiceItem(
    id = id,
    invoiceId = invoiceId,
    productId = productId,
    productName = productName,
    quantity = quantity,
    unitPrice = unitPrice,
    subtotal = subtotal
)

fun InvoiceItem.toEntity() = InvoiceItemEntity(
    id = id,
    invoiceId = invoiceId,
    productId = productId,
    productName = productName,
    quantity = quantity,
    unitPrice = unitPrice,
    subtotal = subtotal
)
