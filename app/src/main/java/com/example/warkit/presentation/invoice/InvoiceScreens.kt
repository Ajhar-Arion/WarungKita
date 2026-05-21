package com.example.warkit.presentation.invoice

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.warkit.domain.model.Invoice
import com.example.warkit.domain.model.InvoiceStatus
import com.example.warkit.presentation.components.WarkitScaffold
import com.example.warkit.presentation.components.WarkitTab
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceListScreen(
    state: InvoiceListState,
    onStatusFilterChange: (InvoiceStatus?) -> Unit,
    onInvoiceClick: (Long) -> Unit,
    onDownloadHistory: (HistoryPeriod) -> Unit = {},
    onClearExportMessage: () -> Unit = {},
    onNavigateBack: () -> Unit,
    onTabSelected: (WarkitTab) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedPeriod by remember { mutableStateOf(HistoryPeriod.Today) }
    var showDownloadSheet by remember { mutableStateOf(false) }
    val visibleInvoices = state.invoices.filter { selectedPeriod.contains(it.date) }
    val periodCounts = remember(state.invoices) {
        val now = System.currentTimeMillis()
        HistoryPeriod.entries.associateWith { period ->
            state.invoices.count { invoice -> period.contains(invoice.date, now) }
        }
    }
    val priceFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
    }

    LaunchedEffect(state.exportMessage) {
        state.exportMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onClearExportMessage()
        }
    }
    
    WarkitScaffold(
        title = "History Pembelian",
        selectedTab = WarkitTab.History,
        onTabSelected = onTabSelected,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (!state.isExporting) {
                        showDownloadSheet = true
                    }
                },
                shape = RoundedCornerShape(8.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                if (state.isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Menyiapkan...")
                } else {
                    Icon(Icons.Default.FileDownload, contentDescription = "Download History")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download History")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LaunchedEffect(Unit) {
                onStatusFilterChange(null)
            }
            
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                HistoryPeriod.entries.forEachIndexed { index, period ->
                    SegmentedButton(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = HistoryPeriod.entries.size)
                    ) {
                        Text(period.label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistorySummaryTile(
                    label = "Total Pembelian",
                    value = priceFormat.format(visibleInvoices.sumOf { it.totalAmount }),
                    modifier = Modifier.weight(1f)
                )
                HistorySummaryTile(
                    label = "Jumlah Transaksi",
                    value = visibleInvoices.size.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (visibleInvoices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Belum ada transaksi",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleInvoices, key = { it.id }) { invoice ->
                        InvoiceListItem(
                            invoice = invoice,
                            onClick = { onInvoiceClick(invoice.id) }
                        )
                    }
                }
            }
        }
    }

    if (showDownloadSheet) {
        HistoryDownloadSheet(
            periodCounts = periodCounts,
            isExporting = state.isExporting,
            onDismiss = { showDownloadSheet = false },
            onPeriodSelected = { period ->
                showDownloadSheet = false
                onDownloadHistory(period)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryDownloadSheet(
    periodCounts: Map<HistoryPeriod, Int>,
    isExporting: Boolean,
    onDismiss: () -> Unit,
    onPeriodSelected: (HistoryPeriod) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 28.dp)
        ) {
            Text(
                text = "Download History Transaksi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            HistoryPeriod.entries.forEach { period ->
                val count = periodCounts[period] ?: 0
                val enabled = !isExporting && count > 0
                ListItem(
                    headlineContent = {
                        Text(
                            text = period.label,
                            color = if (enabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "$count transaksi",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = when (period) {
                                HistoryPeriod.Today -> Icons.Default.Today
                                HistoryPeriod.Week -> Icons.Default.DateRange
                                HistoryPeriod.Month -> Icons.Default.CalendarMonth
                            },
                            contentDescription = null,
                            tint = if (enabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = enabled) {
                            onPeriodSelected(period)
                        }
                )
            }
        }
    }
}

@Composable
private fun HistorySummaryTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun InvoiceListItem(
    invoice: Invoice,
    onClick: () -> Unit
) {
    val priceFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
    }
    val dayFormat = remember { SimpleDateFormat("dd", Locale("id", "ID")) }
    val monthFormat = remember { SimpleDateFormat("MMM", Locale("id", "ID")) }
    val timeFormat = remember { SimpleDateFormat("dd MMM yyyy | HH:mm", Locale("id", "ID")) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .width(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = monthFormat.format(Date(invoice.date)).uppercase(Locale("id", "ID")),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dayFormat.format(Date(invoice.date)),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(10.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timeFormat.format(Date(invoice.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = invoice.customerName.ifBlank { "Customer" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = invoice.invoiceNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = priceFormat.format(invoice.totalAmount),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusChip(status = invoice.status)
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatusChip(status: InvoiceStatus) {
    val (backgroundColor, textColor, text) = when (status) {
        InvoiceStatus.PENDING -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "Pending"
        )
        InvoiceStatus.PAID -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Lunas"
        )
        InvoiceStatus.CANCELLED -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Batal"
        )
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    state: InvoiceDetailState,
    onUpdateStatus: (InvoiceStatus) -> Unit,
    onSharePdf: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val priceFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID")) }
    var showStatusMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Invoice") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (state.invoice != null) {
                        IconButton(onClick = onSharePdf) {
                            Icon(Icons.Default.Share, contentDescription = "Share PDF")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.invoice == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.errorMessage ?: "Invoice tidak ditemukan",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                val invoice = state.invoice
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = invoice.invoiceNumber,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Box {
                                        TextButton(onClick = { showStatusMenu = true }) {
                                            StatusChip(status = invoice.status)
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        
                                        DropdownMenu(
                                            expanded = showStatusMenu,
                                            onDismissRequest = { showStatusMenu = false }
                                        ) {
                                            InvoiceStatus.entries.forEach { status ->
                                                DropdownMenuItem(
                                                    text = { 
                                                        Text(
                                                            when (status) {
                                                                InvoiceStatus.PENDING -> "Pending"
                                                                InvoiceStatus.PAID -> "Lunas"
                                                                InvoiceStatus.CANCELLED -> "Batal"
                                                            }
                                                        )
                                                    },
                                                    onClick = {
                                                        onUpdateStatus(status)
                                                        showStatusMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = dateFormat.format(Date(invoice.date)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // Customer
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Customer",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = invoice.customerName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                    
                    // Items
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Items (${invoice.items.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                invoice.items.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.productName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.quantity} x ${priceFormat.format(item.unitPrice)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = priceFormat.format(item.subtotal),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    if (item != invoice.items.last()) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    }
                                }
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Total",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = priceFormat.format(invoice.totalAmount),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    
                    // Notes
                    if (invoice.notes.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Catatan",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = invoice.notes,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    
                    // Share button
                    item {
                        Button(
                            onClick = onSharePdf,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download & Share PDF")
                        }
                    }
                }
                
                // Loading overlay
                if (state.isUpdating) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
