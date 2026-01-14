package com.example.warkit.presentation.customer

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.warkit.domain.model.Customer
import com.example.warkit.util.PhotoHelper
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    state: CustomerListState,
    onSearchQueryChange: (String) -> Unit,
    onAddCustomerClick: () -> Unit,
    onCustomerClick: (Long) -> Unit,
    onDeleteCustomer: (Long) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf<Customer?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Customer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCustomerClick) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Customer")
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
                placeholder = { Text("Cari customer...") },
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
            
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.customers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (state.searchQuery.isEmpty()) 
                                "Belum ada customer" 
                            else 
                                "Customer tidak ditemukan",
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
                        items = state.customers,
                        key = { it.id }
                    ) { customer ->
                        CustomerListItem(
                            customer = customer,
                            onClick = { onCustomerClick(customer.id) },
                            onDelete = { showDeleteDialog = customer }
                        )
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    showDeleteDialog?.let { customer ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Hapus Customer?") },
            text = { Text("Apakah Anda yakin ingin menghapus ${customer.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCustomer(customer.id)
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
fun CustomerListItem(
    customer: Customer,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    
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
            // Customer photo
            CustomerPhoto(
                photoPath = customer.photoPath,
                name = customer.name,
                size = 50
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Customer info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (customer.phone.isNotEmpty()) {
                    Text(
                        text = customer.phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
fun CustomerPhoto(
    photoPath: String?,
    name: String,
    size: Int = 50
) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (photoPath != null) {
            val photoFile = PhotoHelper.getAbsolutePath(context, photoPath)
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photoFile)
                    .crossfade(true)
                    .build(),
                contentDescription = "Foto $name",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerScreen(
    state: AddCustomerState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onPhotoSelected: (Uri?, String?) -> Unit,
    onSave: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var showPhotoOptions by remember { mutableStateOf(false) }
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    
    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val savedPath = PhotoHelper.savePhoto(context, it)
            onPhotoSelected(it, savedPath)
        }
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoFile != null) {
            val savedPath = PhotoHelper.savePhotoFromFile(context, tempPhotoFile!!)
            if (savedPath != null) {
                onPhotoSelected(tempPhotoUri, savedPath)
            }
        }
        tempPhotoFile = null
        tempPhotoUri = null
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Launch camera after permission granted
            tempPhotoFile = PhotoHelper.createTempPhotoFile(context)
            tempPhotoUri = PhotoHelper.getTempPhotoUri(context, tempPhotoFile!!)
            cameraLauncher.launch(tempPhotoUri!!)
        }
    }
    
    fun launchCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        if (hasPermission) {
            tempPhotoFile = PhotoHelper.createTempPhotoFile(context)
            tempPhotoUri = PhotoHelper.getTempPhotoUri(context, tempPhotoFile!!)
            cameraLauncher.launch(tempPhotoUri!!)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    // Handle save success
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onNavigateBack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tambah Customer") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Photo picker
            PhotoPicker(
                photoUri = state.photoUri,
                photoPath = state.photoPath,
                onClick = { showPhotoOptions = true }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Form fields
            CustomerFormFields(
                name = state.name,
                phone = state.phone,
                email = state.email,
                address = state.address,
                errorMessage = state.errorMessage,
                onNameChange = onNameChange,
                onPhoneChange = onPhoneChange,
                onEmailChange = onEmailChange,
                onAddressChange = onAddressChange
            )
            
            if (state.isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
        }
    }
    
    // Photo options bottom sheet
    if (showPhotoOptions) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoOptions = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Pilih Foto",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                ListItem(
                    headlineContent = { Text("Ambil dari Kamera") },
                    leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                    modifier = Modifier.clickable {
                        launchCamera()
                        showPhotoOptions = false
                    }
                )
                
                ListItem(
                    headlineContent = { Text("Pilih dari Galeri") },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    modifier = Modifier.clickable {
                        galleryLauncher.launch("image/*")
                        showPhotoOptions = false
                    }
                )
                
                if (state.photoUri != null || state.photoPath != null) {
                    ListItem(
                        headlineContent = { Text("Hapus Foto") },
                        leadingContent = { 
                            Icon(
                                Icons.Default.Delete, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            ) 
                        },
                        modifier = Modifier.clickable {
                            onPhotoSelected(null, null)
                            showPhotoOptions = false
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun PhotoPicker(
    photoUri: Uri?,
    photoPath: String?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            photoPath != null -> {
                // Prioritize photoPath (saved file) - use timestamp in key to force reload
                val file = PhotoHelper.getAbsolutePath(context, photoPath)
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(file)
                        .memoryCacheKey(photoPath) // Use path as cache key
                        .diskCacheKey(photoPath)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto Customer",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            photoUri != null -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(photoUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto Customer",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tambah Foto",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerFormFields(
    name: String,
    phone: String,
    email: String,
    address: String,
    errorMessage: String?,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onAddressChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Name field (required)
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Nama *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = errorMessage != null,
            supportingText = errorMessage?.let { { Text(it) } },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
        )
        
        // Phone field
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Nomor Telepon") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
        )
        
        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
        )
        
        // Address field
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text("Alamat") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCustomerScreen(
    state: EditCustomerState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onPhotoSelected: (Uri?, String?) -> Unit,
    onSave: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var showPhotoOptions by remember { mutableStateOf(false) }
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    
    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val savedPath = PhotoHelper.savePhoto(context, it)
            onPhotoSelected(it, savedPath)
        }
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoFile != null) {
            val savedPath = PhotoHelper.savePhotoFromFile(context, tempPhotoFile!!)
            if (savedPath != null) {
                onPhotoSelected(tempPhotoUri, savedPath)
            }
        }
        tempPhotoFile = null
        tempPhotoUri = null
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Launch camera after permission granted
            tempPhotoFile = PhotoHelper.createTempPhotoFile(context)
            tempPhotoUri = PhotoHelper.getTempPhotoUri(context, tempPhotoFile!!)
            cameraLauncher.launch(tempPhotoUri!!)
        }
    }
    
    fun launchCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        if (hasPermission) {
            tempPhotoFile = PhotoHelper.createTempPhotoFile(context)
            tempPhotoUri = PhotoHelper.getTempPhotoUri(context, tempPhotoFile!!)
            cameraLauncher.launch(tempPhotoUri!!)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    // Handle save success
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onNavigateBack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Customer") },
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Photo picker
                PhotoPicker(
                    photoUri = state.photoUri,
                    photoPath = state.photoPath,
                    onClick = { showPhotoOptions = true }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Form fields
                CustomerFormFields(
                    name = state.name,
                    phone = state.phone,
                    email = state.email,
                    address = state.address,
                    errorMessage = state.errorMessage,
                    onNameChange = onNameChange,
                    onPhoneChange = onPhoneChange,
                    onEmailChange = onEmailChange,
                    onAddressChange = onAddressChange
                )
                
                if (state.isLoading && state.name.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
            }
        }
    }
    
    // Photo options bottom sheet
    if (showPhotoOptions) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoOptions = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Pilih Foto",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                ListItem(
                    headlineContent = { Text("Ambil dari Kamera") },
                    leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                    modifier = Modifier.clickable {
                        launchCamera()
                        showPhotoOptions = false
                    }
                )
                
                ListItem(
                    headlineContent = { Text("Pilih dari Galeri") },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    modifier = Modifier.clickable {
                        galleryLauncher.launch("image/*")
                        showPhotoOptions = false
                    }
                )
                
                if (state.photoUri != null || state.photoPath != null) {
                    ListItem(
                        headlineContent = { Text("Hapus Foto") },
                        leadingContent = { 
                            Icon(
                                Icons.Default.Delete, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            ) 
                        },
                        modifier = Modifier.clickable {
                            onPhotoSelected(null, null)
                            showPhotoOptions = false
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
