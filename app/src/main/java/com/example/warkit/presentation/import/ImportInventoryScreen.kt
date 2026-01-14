package com.example.warkit.presentation.`import`

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.warkit.domain.model.Product
import com.example.warkit.util.ExcelHelper
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportInventoryScreen(
    state: ImportState,
    onFileSelected: (Uri) -> Unit,
    onConfirmImport: (updateDuplicateStock: Boolean) -> Unit,
    onDismissDuplicateDialog: () -> Unit = {},
    onDownloadTemplate: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onFileSelected(it) }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Inventory") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.importComplete) {
                // Import Complete Screen
                ImportCompleteView(
                    importedCount = state.importedCount,
                    stockUpdatedCount = state.stockUpdatedCount,
                    errors = state.errors,
                    onNavigateBack = onNavigateBack
                )
            } else if (state.showPreview) {
                // Preview Screen
                PreviewImportView(
                    newProducts = state.newProducts,
                    duplicateProducts = state.duplicateProducts,
                    successCount = state.successCount,
                    failedCount = state.failedCount,
                    errors = state.errors,
                    isLoading = state.isLoading,
                    onConfirm = { onConfirmImport(false) },
                    onCancel = onNavigateBack
                )
            } else {
                // Initial Screen - File Selection
                FileSelectionView(
                    isLoading = state.isLoading,
                    errors = state.errors,
                    onSelectFile = {
                        filePickerLauncher.launch(arrayOf(
                            "text/csv",
                            "text/comma-separated-values",
                            "application/csv",
                            "*/*" // Fallback for all files
                        ))
                    },
                    onDownloadTemplate = onDownloadTemplate
                )
            }
        }
    }
    
    // Duplicate SKU Dialog
    if (state.showDuplicateDialog && state.duplicateProducts.isNotEmpty()) {
        DuplicateSkuDialog(
            duplicates = state.duplicateProducts,
            onConfirmAddStock = { onConfirmImport(true) },
            onSkipDuplicates = { onConfirmImport(false) },
            onDismiss = onDismissDuplicateDialog
        )
    }
}

@Composable
private fun DuplicateSkuDialog(
    duplicates: List<DuplicateProduct>,
    onConfirmAddStock: () -> Unit,
    onSkipDuplicates: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("SKU Duplikat Ditemukan") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${duplicates.size} produk memiliki SKU yang sudah ada di inventory.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Apakah Anda ingin menambahkan stok ke produk yang sudah ada?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(duplicates.take(5)) { duplicate ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = duplicate.existingProduct.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "SKU: ${duplicate.existingProduct.sku}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Stok saat ini: ${duplicate.existingProduct.stock} → +${duplicate.addStock}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    if (duplicates.size > 5) {
                        item {
                            Text(
                                text = "... dan ${duplicates.size - 5} produk lainnya",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirmAddStock) {
                Text("Ya, Tambah Stok")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onSkipDuplicates) {
                Text("Lewati Duplikat")
            }
        }
    )
}

@Composable
private fun FileSelectionView(
    isLoading: Boolean,
    errors: List<String>,
    onSelectFile: () -> Unit,
    onDownloadTemplate: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Icon(
            imageVector = Icons.Default.FileUpload,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Import Data Inventory",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Upload file CSV untuk menambahkan produk secara massal.\nFile CSV dapat dibuat dan diedit menggunakan Excel.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Instructions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Format File CSV:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• Kolom: Name, SKU, Price, Stock, MinStock, Category, Description",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• Kolom wajib: Name, Price, Stock",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• Baris pertama adalah header (akan di-skip)",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• Simpan file dengan format .csv",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Error messages
        if (errors.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Error:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    errors.forEach { error ->
                        Text(
                            text = "• $error",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Buttons
        OutlinedButton(
            onClick = onDownloadTemplate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Download Template")
        }
        
        Button(
            onClick = onSelectFile,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isLoading) "Memproses..." else "Pilih File CSV")
        }
    }
}

@Composable
private fun PreviewImportView(
    newProducts: List<Product>,
    duplicateProducts: List<DuplicateProduct>,
    successCount: Int,
    failedCount: Int,
    errors: List<String>,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = {},
                label = { Text("Baru: ${newProducts.size}") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Add,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
            
            if (duplicateProducts.isNotEmpty()) {
                AssistChip(
                    onClick = {},
                    label = { Text("Duplikat: ${duplicateProducts.size}") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
            }
            
            if (failedCount > 0) {
                AssistChip(
                    onClick = {},
                    label = { Text("Gagal: $failedCount") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Error,
                            null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
            }
        }
        
        // Duplicate warning
        if (duplicateProducts.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${duplicateProducts.size} produk dengan SKU yang sudah ada akan ditawarkan untuk update stok",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        // Error list
        if (errors.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Baris yang gagal:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    errors.take(5).forEach { error ->
                        Text(
                            text = "• $error",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (errors.size > 5) {
                        Text(
                            text = "... dan ${errors.size - 5} lainnya",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        
        Text(
            text = "Preview Produk Baru (${newProducts.size} item)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        // Product list preview
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(newProducts) { product ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                if (product.sku.isNotBlank()) {
                                    Text(
                                        text = "SKU: ${product.sku}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (product.category.isNotBlank()) {
                                    Text(
                                        text = "Kategori: ${product.category}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = currencyFormat.format(product.price),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Stok: ${product.stock}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
            
            if (newProducts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Semua produk memiliki SKU duplikat",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Confirm buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Text("Batal")
            }
            
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                enabled = !isLoading && (newProducts.isNotEmpty() || duplicateProducts.isNotEmpty())
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mengimport...")
                } else {
                    Icon(Icons.Default.Check, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lanjutkan")
                }
            }
        }
    }
}

@Composable
private fun ImportCompleteView(
    importedCount: Int,
    stockUpdatedCount: Int,
    errors: List<String>,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Import Selesai!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (importedCount > 0) {
            Text(
                text = "$importedCount produk baru ditambahkan",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
        
        if (stockUpdatedCount > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$stockUpdatedCount produk stok diupdate",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        if (importedCount == 0 && stockUpdatedCount == 0) {
            Text(
                text = "Tidak ada perubahan yang dilakukan",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (errors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${errors.size} item gagal diproses",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = onNavigateBack) {
            Text("Kembali ke Dashboard")
        }
    }
}
