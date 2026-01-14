package com.example.warkit.presentation.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.warkit.domain.model.Product
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryListScreen(
    state: InventoryListState,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onFilterChange: (InventoryFilter) -> Unit,
    onAddProductClick: () -> Unit,
    onProductClick: (Long) -> Unit,
    onDeleteProduct: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf<Product?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProductClick) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Produk")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Cari produk...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )
            
            // Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // All filter
                item {
                    FilterChip(
                        selected = state.filter == InventoryFilter.ALL && state.selectedCategory == null,
                        onClick = {
                            onFilterChange(InventoryFilter.ALL)
                            onCategorySelected(null)
                        },
                        label = { Text("Semua") }
                    )
                }
                
                // Low stock filter
                item {
                    FilterChip(
                        selected = state.filter == InventoryFilter.LOW_STOCK,
                        onClick = { onFilterChange(InventoryFilter.LOW_STOCK) },
                        label = { Text("Stok Rendah") },
                        leadingIcon = if (state.filter == InventoryFilter.LOW_STOCK) {
                            { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
                
                // Category filters
                items(state.categories) { category ->
                    FilterChip(
                        selected = state.selectedCategory == category,
                        onClick = {
                            onCategorySelected(if (state.selectedCategory == category) null else category)
                        },
                        label = { Text(category) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Products list
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when {
                                state.searchQuery.isNotEmpty() -> "Produk tidak ditemukan"
                                state.filter == InventoryFilter.LOW_STOCK -> "Tidak ada produk dengan stok rendah"
                                else -> "Belum ada produk"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = state.products,
                        key = { it.id }
                    ) { product ->
                        ProductListItem(
                            product = product,
                            onClick = { onProductClick(product.id) },
                            onDelete = { showDeleteDialog = product }
                        )
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    showDeleteDialog?.let { product ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Hapus Produk?") },
            text = { Text("Apakah Anda yakin ingin menghapus ${product.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteProduct(product.id)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun ProductListItem(
    product: Product,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val priceFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (product.isLowStock) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Stok Rendah",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                if (product.sku.isNotEmpty()) {
                    Text(
                        text = "SKU: ${product.sku}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row {
                    Text(
                        text = priceFormat.format(product.price),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Stok: ${product.stock}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (product.isLowStock) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                
                if (product.category.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    AssistChip(
                        onClick = { },
                        label = { Text(product.category, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
            
            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Hapus",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    state: AddProductState,
    onNameChange: (String) -> Unit,
    onSkuChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onStockChange: (String) -> Unit,
    onMinStockChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    onNavigateBack: () -> Unit
) {
    // Handle save success
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onNavigateBack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tambah Produk") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    TextButton(
                        onClick = onSave,
                        enabled = !state.isLoading
                    ) {
                        Text("Simpan")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProductFormFields(
                name = state.name,
                sku = state.sku,
                price = state.price,
                stock = state.stock,
                minStock = state.minStock,
                category = state.category,
                description = state.description,
                availableCategories = state.availableCategories,
                errorMessage = state.errorMessage,
                onNameChange = onNameChange,
                onSkuChange = onSkuChange,
                onPriceChange = onPriceChange,
                onStockChange = onStockChange,
                onMinStockChange = onMinStockChange,
                onCategoryChange = onCategoryChange,
                onDescriptionChange = onDescriptionChange
            )
            
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    state: EditProductState,
    onNameChange: (String) -> Unit,
    onSkuChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onStockChange: (String) -> Unit,
    onMinStockChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    onNavigateBack: () -> Unit
) {
    // Handle save success
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onNavigateBack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Produk") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    TextButton(
                        onClick = onSave,
                        enabled = !state.isLoading
                    ) {
                        Text("Simpan")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading && state.name.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProductFormFields(
                    name = state.name,
                    sku = state.sku,
                    price = state.price,
                    stock = state.stock,
                    minStock = state.minStock,
                    category = state.category,
                    description = state.description,
                    availableCategories = state.availableCategories,
                    errorMessage = state.errorMessage,
                    onNameChange = onNameChange,
                    onSkuChange = onSkuChange,
                    onPriceChange = onPriceChange,
                    onStockChange = onStockChange,
                    onMinStockChange = onMinStockChange,
                    onCategoryChange = onCategoryChange,
                    onDescriptionChange = onDescriptionChange
                )
                
                if (state.isLoading && state.name.isNotEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormFields(
    name: String,
    sku: String,
    price: String,
    stock: String,
    minStock: String,
    category: String,
    description: String,
    availableCategories: List<String>,
    errorMessage: String?,
    onNameChange: (String) -> Unit,
    onSkuChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onStockChange: (String) -> Unit,
    onMinStockChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit
) {
    var showCategoryDropdown by remember { mutableStateOf(false) }
    
    // Name field (required)
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Nama Produk *") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = errorMessage != null,
        supportingText = errorMessage?.let { { Text(it) } },
        leadingIcon = { Icon(Icons.Default.Inventory, contentDescription = null) }
    )
    
    // SKU field
    OutlinedTextField(
        value = sku,
        onValueChange = onSkuChange,
        label = { Text("SKU") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) }
    )
    
    // Price field
    OutlinedTextField(
        value = price,
        onValueChange = onPriceChange,
        label = { Text("Harga") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        leadingIcon = { Text("Rp", modifier = Modifier.padding(start = 12.dp)) }
    )
    
    // Stock fields in a row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = stock,
            onValueChange = onStockChange,
            label = { Text("Stok") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        
        OutlinedTextField(
            value = minStock,
            onValueChange = onMinStockChange,
            label = { Text("Stok Minimum") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
    
    // Category field with dropdown
    ExposedDropdownMenuBox(
        expanded = showCategoryDropdown,
        onExpandedChange = { showCategoryDropdown = it }
    ) {
        OutlinedTextField(
            value = category,
            onValueChange = onCategoryChange,
            label = { Text("Kategori") },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable),
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) }
        )
        
        if (availableCategories.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = showCategoryDropdown,
                onDismissRequest = { showCategoryDropdown = false }
            ) {
                availableCategories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = {
                            onCategoryChange(cat)
                            showCategoryDropdown = false
                        }
                    )
                }
            }
        }
    }
    
    // Description field
    OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChange,
        label = { Text("Deskripsi") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        maxLines = 5,
        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) }
    )
}
