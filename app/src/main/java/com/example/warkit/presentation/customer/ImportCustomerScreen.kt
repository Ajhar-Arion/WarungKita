package com.example.warkit.presentation.customer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.warkit.domain.model.Customer
import com.example.warkit.presentation.components.WarkitScaffold
import com.example.warkit.presentation.components.WarkitTab
import com.example.warkit.util.ExcelHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportCustomerScreen(
    state: ImportCustomerState,
    onFileSelected: (Uri) -> Unit,
    onConfirmImport: () -> Unit,
    onDownloadTemplate: () -> Unit,
    onSaveTemplate: (Uri) -> Boolean,
    onNavigateBack: () -> Unit,
    onTabSelected: (WarkitTab) -> Unit = {}
) {
    var templateMessage by remember { mutableStateOf<String?>(null) }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onFileSelected(it) }
    }
    
    val saveTemplateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ExcelHelper.XLSX_MIME_TYPE)
    ) { uri ->
        uri?.let {
            templateMessage = if (onSaveTemplate(it)) {
                "Template customer berhasil disimpan"
            } else {
                "Gagal menyimpan template customer"
            }
        }
    }
    
    fun openCustomerFilePicker() {
        filePickerLauncher.launch(
            arrayOf(
                ExcelHelper.XLSX_MIME_TYPE,
                "*/*"
            )
        )
    }
    
    WarkitScaffold(
        title = "Import Customer",
        selectedTab = WarkitTab.Customers,
        onTabSelected = onTabSelected,
        showBack = true,
        onBackClick = onNavigateBack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.importComplete) {
                item {
                    ImportCustomerResultCard(
                        importedCount = state.importedCount,
                        skippedDuplicateCount = state.skippedDuplicateCount
                    )
                }
            }
            
            item {
                CustomerImportHintCard()
            }
            
            item {
                CustomerImportActionRow(
                    icon = Icons.Default.Description,
                    title = "Download Template Excel",
                    trailingIcon = Icons.Default.Download,
                    onClick = onDownloadTemplate
                )
            }
            
            item {
                CustomerImportActionRow(
                    icon = Icons.Default.Save,
                    title = "Simpan Template",
                    trailingIcon = Icons.Default.ChevronRight,
                    onClick = { saveTemplateLauncher.launch("customer_template.xlsx") }
                )
            }
            
            item {
                CustomerImportActionRow(
                    icon = Icons.Default.FileUpload,
                    title = if (state.isLoading) "Memproses file..." else "Import Excel",
                    trailingIcon = Icons.Default.ChevronRight,
                    enabled = !state.isLoading,
                    onClick = ::openCustomerFilePicker
                )
            }
            
            item {
                CustomerDropZone(
                    isLoading = state.isLoading,
                    importComplete = state.importComplete,
                    onClick = ::openCustomerFilePicker
                )
            }
            
            templateMessage?.let { message ->
                item {
                    CustomerImportMessageCard(message = message)
                }
            }
            
            if (state.errors.isNotEmpty()) {
                item {
                    CustomerImportErrorCard(errors = state.errors)
                }
            }
            
            if (state.showPreview) {
                item {
                    CustomerPreviewCard(
                        customers = state.previewCustomers,
                        successCount = state.successCount,
                        failedCount = state.failedCount,
                        isLoading = state.isLoading,
                        onConfirmImport = onConfirmImport,
                        onCancel = onNavigateBack
                    )
                }
            } else {
                item {
                    CustomerTemplatePreviewCard()
                }
            }
        }
    }
}

@Composable
private fun CustomerImportHintCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Import data customer lewat template Excel. Isi nama, nomor HP, email, dan alamat, lalu import kembali ke aplikasi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun CustomerImportActionRow(
    icon: ImageVector,
    title: String,
    trailingIcon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(23.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CustomerDropZone(
    isLoading: Boolean,
    importComplete: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (importComplete) Icons.Default.CheckCircle else Icons.Default.People,
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    isLoading -> "Memproses file..."
                    importComplete -> "Import Customer Berhasil"
                    else -> "Tap untuk memilih file customer"
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Gunakan file .xlsx dari template Excel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ImportCustomerResultCard(
    importedCount: Int,
    skippedDuplicateCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Import Customer Berhasil",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$importedCount customer ditambahkan, $skippedDuplicateCount duplikat dilewati",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun CustomerImportMessageCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f))
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CustomerImportErrorCard(errors: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Catatan Import",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            errors.take(3).forEach { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CustomerPreviewCard(
    customers: List<Customer>,
    successCount: Int,
    failedCount: Int,
    isLoading: Boolean,
    onConfirmImport: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Preview Customer",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("Valid: $successCount") })
                if (failedCount > 0) {
                    AssistChip(onClick = {}, label = { Text("Gagal: $failedCount") })
                }
            }
            
            CustomerTemplateTableHeader()
            customers.take(5).forEachIndexed { index, customer ->
                CustomerTemplateRow(
                    code = "CUST${(index + 1).toString().padStart(3, '0')}",
                    name = customer.name,
                    phone = customer.phone.ifBlank { "-" },
                    address = customer.address.ifBlank { customer.email.ifBlank { "-" } }
                )
            }
            if (customers.size > 5) {
                Text(
                    text = "... dan ${customers.size - 5} customer lainnya",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Batal")
                }
                Button(
                    onClick = onConfirmImport,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && customers.isNotEmpty(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import")
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerTemplatePreviewCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Preview Template",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            CustomerTemplateTableHeader()
            CustomerTemplateRow("CUST001", "Budi", "081234567890", "Jl. Melati No. 10")
            CustomerTemplateRow("CUST002", "Siti", "081322223333", "Jl. Mawar No. 5")
            CustomerTemplateRow("...", "...", "...", "...")
        }
    }
}

@Composable
private fun CustomerTemplateTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        CustomerTemplateCell("Kode", 0.9f, true)
        CustomerTemplateCell("Nama", 1.15f, true)
        CustomerTemplateCell("No. HP", 1.2f, true)
        CustomerTemplateCell("Alamat", 1.35f, true)
    }
}

@Composable
private fun CustomerTemplateRow(
    code: String,
    name: String,
    phone: String,
    address: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        CustomerTemplateCell(code, 0.9f)
        CustomerTemplateCell(name, 1.15f)
        CustomerTemplateCell(phone, 1.2f)
        CustomerTemplateCell(address, 1.35f)
    }
    HorizontalDivider()
}

@Composable
private fun RowScope.CustomerTemplateCell(
    value: String,
    weight: Float,
    header: Boolean = false
) {
    Text(
        text = value,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
