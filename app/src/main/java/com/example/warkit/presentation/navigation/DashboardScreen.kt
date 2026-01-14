package com.example.warkit.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.warkit.data.security.PinManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onCustomerClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onPurchaseClick: () -> Unit = {},
    onInvoiceClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    onSettlementClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val pinManager = remember { PinManager(context) }
    var showCreditDialog by remember { mutableStateOf(!pinManager.hasCreditBeenShown()) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Warkit") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Selamat Datang!",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Text(
                text = "Pilih menu untuk memulai:",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Menu Cards
            DashboardMenuCard(
                icon = Icons.Default.People,
                title = "Customer",
                description = "Kelola data customer",
                onClick = onCustomerClick
            )
            
            DashboardMenuCard(
                icon = Icons.Default.Inventory,
                title = "Inventory",
                description = "Kelola stok barang",
                onClick = onInventoryClick
            )
            
            DashboardMenuCard(
                icon = Icons.Default.ShoppingCart,
                title = "Transaksi",
                description = "Buat transaksi baru",
                onClick = onPurchaseClick
            )
            
            DashboardMenuCard(
                icon = Icons.Default.Receipt,
                title = "Invoice",
                description = "Lihat daftar invoice",
                onClick = onInvoiceClick
            )
            
            DashboardMenuCard(
                icon = Icons.Default.Payment,
                title = "Pelunasan",
                description = "Lunasi tagihan customer",
                onClick = onSettlementClick
            )
            
            DashboardMenuCard(
                icon = Icons.Default.FileUpload,
                title = "Import Inventory",
                description = "Import data dari file CSV/Excel",
                onClick = onImportClick
            )
            
            DashboardMenuCard(
                icon = Icons.Default.FileDownload,
                title = "Export Transaksi",
                description = "Export data ke file CSV/Excel",
                onClick = onExportClick
            )
        }
    }
    
    // Credit Dialog
    if (showCreditDialog) {
        CreditDialog(
            onDismiss = {
                pinManager.setCreditShown()
                showCreditDialog = false
            }
        )
    }
}

@Composable
fun CreditDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Selamat Datang di Warkit! 🎉",
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Aplikasi ini dengan bangga dipersembahkan oleh:",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Ajhar Haqqani Ersaputra",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "dengan bantuan luar biasa dari",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "GitHub Copilot 🤖",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "\"Karena mengelola warung tidak harus ribet, " +
                               "cukup ketik-ketik sedikit, bisnis pun jadi hemat waktu. " +
                               "Semoga aplikasi ini membantu usaha Anda berkembang!\"",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Terima kasih telah menggunakan Warkit! 🙏",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Mulai Menggunakan")
            }
        }
    )
}

@Composable
fun DashboardMenuCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = if (enabled) {
            CardDefaults.cardColors()
        } else {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    }
                )
                Text(
                    text = if (enabled) description else "$description (Coming Soon)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (enabled) 1f else 0.5f
                    )
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                }
            )
        }
    }
}
