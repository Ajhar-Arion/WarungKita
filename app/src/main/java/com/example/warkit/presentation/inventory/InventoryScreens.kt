package com.example.warkit.presentation.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.warkit.domain.model.Product
import com.example.warkit.presentation.components.WarkitScaffold
import com.example.warkit.presentation.components.WarkitSearchField
import com.example.warkit.presentation.components.WarkitTab
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
    onNavigateBack: () -> Unit,
    onTabSelected: (WarkitTab) -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf<Product?>(null) }
    
    WarkitScaffold(
        title = "List Barang",
        selectedTab = WarkitTab.Products,
        onTabSelected = onTabSelected,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProductClick,
                shape = RoundedCornerShape(8.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Barang")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tambah Barang")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WarkitSearchField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = "Cari barang",
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        onFilterChange(
                            if (state.filter == InventoryFilter.LOW_STOCK) InventoryFilter.ALL
                            else InventoryFilter.LOW_STOCK
                        )
                    }
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Filter stok rendah",
                        tint = if (state.filter == InventoryFilter.LOW_STOCK) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
            
            if (state.categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedCategory == null && state.filter == InventoryFilter.ALL,
                            onClick = {
                                onFilterChange(InventoryFilter.ALL)
                                onCategorySelected(null)
                            },
                            label = { Text("Semua") }
                        )
                    }
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
                Spacer(modifier = Modifier.height(4.dp))
            }
            
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
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        ProductTableHeader()
                    }
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
private fun ProductTableHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nama Barang",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(2.35f)
            )
            Text(
                text = "Kategori",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1.1f)
            )
            Text(
                text = "Stok",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.75f)
            )
            Text(
                text = "Harga",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1.2f)
            )
            Spacer(modifier = Modifier.width(52.dp))
        }
    }
}

@Composable
fun ProductListItem(
    product: Product,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val priceFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
    }
    
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
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(2.35f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = product.sku.ifBlank { "BRG${product.id.toString().padStart(3, '0')}" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Text(
                text = product.category.ifBlank { "-" },
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.1f)
            )
            Row(
                modifier = Modifier.weight(0.75f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.stock.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (product.isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                if (product.isLowStock) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Stok Rendah",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                text = priceFormat.format(product.price),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.2f)
            )
            Row(modifier = Modifier.width(52.dp)) {
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(26.dp)
                ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Hapus",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                }
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
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
