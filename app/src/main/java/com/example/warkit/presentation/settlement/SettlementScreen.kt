package com.example.warkit.presentation.settlement

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.warkit.domain.model.Customer
import com.example.warkit.domain.model.Invoice
import com.example.warkit.util.PhotoHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementScreen(
    state: SettlementState,
    onCustomerFilterChange: (Long?) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onSettleSelected: () -> Unit,
    onSettleSingle: (Long) -> Unit,
    onClearMessages: () -> Unit,
    onNavigateBack: () -> Unit,
    onViewInvoice: (Long) -> Unit
) {
    val priceFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    
    // Show snackbar for messages
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(state.successMessage, state.errorMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessages()
        }
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessages()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pelunasan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (state.filteredInvoices.isNotEmpty()) {
                        if (state.selectedInvoiceIds.isEmpty()) {
                            TextButton(onClick = onSelectAll) {
                                Text("Pilih Semua")
                            }
                        } else {
                            TextButton(onClick = onClearSelection) {
                                Text("Batal Pilih")
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (state.selectedCount > 0) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${state.selectedCount} invoice dipilih",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = priceFormat.format(state.totalSelected),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Button(
                            onClick = { showConfirmDialog = true },
                            enabled = !state.isProcessing
                        ) {
                            if (state.isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Lunasi")
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Customer Filter
            CustomerFilterCard(
                customers = state.customers,
                selectedCustomerId = state.selectedCustomerId,
                onClick = { showCustomerPicker = true },
                onClear = { onCustomerFilterChange(null) }
            )
            
            // Summary Card
            if (state.filteredInvoices.isNotEmpty()) {
                SummaryCard(
                    invoiceCount = state.filteredInvoices.size,
                    totalAmount = state.filteredInvoices.sumOf { it.totalAmount },
                    priceFormat = priceFormat
                )
            }
            
            // Invoice List
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.filteredInvoices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (state.selectedCustomerId != null) 
                                "Tidak ada tagihan untuk customer ini"
                            else 
                                "Tidak ada tagihan yang belum lunas",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = if (state.selectedCount > 0) 100.dp else 16.dp)
                ) {
                    items(state.filteredInvoices, key = { it.id }) { invoice ->
                        PendingInvoiceItem(
                            invoice = invoice,
                            isSelected = invoice.id in state.selectedInvoiceIds,
                            onToggleSelect = { onToggleSelection(invoice.id) },
                            onSettle = { onSettleSingle(invoice.id) },
                            onClick = { onViewInvoice(invoice.id) },
                            priceFormat = priceFormat,
                            dateFormat = dateFormat,
                            isProcessing = state.isProcessing
                        )
                    }
                }
            }
        }
    }
    
    // Customer Picker Dialog
    if (showCustomerPicker) {
        CustomerPickerDialog(
            customers = state.customers,
            selectedCustomerId = state.selectedCustomerId,
            onSelect = {
                onCustomerFilterChange(it)
                showCustomerPicker = false
            },
            onDismiss = { showCustomerPicker = false }
        )
    }
    
    // Confirm Settlement Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = { Icon(Icons.Default.Payment, contentDescription = null) },
            title = { Text("Konfirmasi Pelunasan") },
            text = {
                Text("Anda akan melunasi ${state.selectedCount} invoice dengan total ${priceFormat.format(state.totalSelected)}. Lanjutkan?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onSettleSelected()
                    }
                ) {
                    Text("Lunasi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun CustomerFilterCard(
    customers: List<Customer>,
    selectedCustomerId: Long?,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val selectedCustomer = customers.find { it.id == selectedCustomerId }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.FilterList,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Filter Customer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (selectedCustomer != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Customer photo
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedCustomer.photoPath != null) {
                                val photoFile = PhotoHelper.getAbsolutePath(context, selectedCustomer.photoPath)
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(photoFile)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = selectedCustomer.name.firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedCustomer.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                } else {
                    Text(
                        text = "Semua Customer",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            if (selectedCustomerId != null) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = "Hapus filter")
                }
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SummaryCard(
    invoiceCount: Int,
    totalAmount: Double,
    priceFormat: NumberFormat
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Total Tagihan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$invoiceCount invoice",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Jumlah",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = priceFormat.format(totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun PendingInvoiceItem(
    invoice: Invoice,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onSettle: () -> Unit,
    onClick: () -> Unit,
    priceFormat: NumberFormat,
    dateFormat: SimpleDateFormat,
    isProcessing: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = if (isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() }
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Invoice info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = invoice.invoiceNumber,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = invoice.customerName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateFormat.format(Date(invoice.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Amount and action
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = priceFormat.format(invoice.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                
                TextButton(
                    onClick = onSettle,
                    enabled = !isProcessing,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("Lunasi", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPickerDialog(
    customers: List<Customer>,
    selectedCustomerId: Long?,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredCustomers = if (searchQuery.isBlank()) {
        customers
    } else {
        customers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery, ignoreCase = true)
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Customer") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari customer...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // All customers option
                ListItem(
                    headlineContent = { Text("Semua Customer") },
                    leadingContent = {
                        RadioButton(
                            selected = selectedCustomerId == null,
                            onClick = { onSelect(null) }
                        )
                    },
                    modifier = Modifier.clickable { onSelect(null) }
                )
                
                HorizontalDivider()
                
                LazyColumn {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        ListItem(
                            headlineContent = { Text(customer.name) },
                            supportingContent = if (customer.phone.isNotEmpty()) {
                                { Text(customer.phone) }
                            } else null,
                            leadingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = customer.id == selectedCustomerId,
                                        onClick = { onSelect(customer.id) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (customer.photoPath != null) {
                                            val photoFile = PhotoHelper.getAbsolutePath(context, customer.photoPath)
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(photoFile)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(
                                                text = customer.name.firstOrNull()?.uppercase() ?: "?",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.clickable { onSelect(customer.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
