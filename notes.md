# Notes: Warkit Development

## Project Structure

### Current State
- Project sudah ter-setup dengan Kotlin + Jetpack Compose
- Namespace: `com.example.warkit`
- Min SDK: 23, Target SDK: 36
- Compose BOM sudah include

### Dependencies Existing
- androidx.core.ktx
- androidx.lifecycle.runtime.ktx
- androidx.activity.compose
- Compose UI, Graphics, Tooling, Material3

### Dependencies To Add
```kotlin
// Room Database
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// Navigation Compose
implementation("androidx.navigation:navigation-compose:2.7.7")
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

// Hilt DI
implementation("com.google.dagger:hilt-android:2.50")
ksp("com.google.dagger:hilt-compiler:2.50")

// ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

// Security - PIN Encryption
implementation("androidx.security:security-crypto:1.1.0-alpha06")

// PDF Generation
implementation("com.itextpdf:itext7-core:7.2.5")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// Date/Time
implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

// CameraX (untuk foto customer)
implementation("androidx.camera:camera-camera2:1.3.1")
implementation("androidx.camera:camera-lifecycle:1.3.1")
implementation("androidx.camera:camera-view:1.3.1")

// Coil (untuk load & display foto)
implementation("io.coil-kt:coil-compose:2.5.0")

// Fastexcel (untuk import/export Excel .xlsx)
implementation("org.dhatim:fastexcel:0.20.0")
implementation("org.dhatim:fastexcel-reader:0.20.0")
coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
```

## Package Structure Plan

```
com.example.warkit/
├── WarkitApplication.kt          # Application class
├── MainActivity.kt               # Single Activity
├── data/
│   ├── local/
│   │   ├── WarkitDatabase.kt
│   │   ├── dao/
│   │   │   ├── CustomerDao.kt
│   │   │   ├── ProductDao.kt
│   │   │   ├── InvoiceDao.kt
│   │   │   └── InvoiceItemDao.kt
│   │   └── entity/
│   │       ├── CustomerEntity.kt
│   │       ├── ProductEntity.kt
│   │       ├── InvoiceEntity.kt
│   │       └── InvoiceItemEntity.kt
│   └── repository/
│       ├── CustomerRepositoryImpl.kt
│       ├── ProductRepositoryImpl.kt
│       └── InvoiceRepositoryImpl.kt
├── domain/
│   ├── model/
│   │   ├── Customer.kt
│   │   ├── Product.kt
│   │   ├── Invoice.kt
│   │   └── InvoiceItem.kt
│   ├── repository/
│   │   ├── CustomerRepository.kt
│   │   ├── ProductRepository.kt
│   │   └── InvoiceRepository.kt
│   └── usecase/
│       ├── customer/
│       ├── product/
│       ├── purchase/
│       └── invoice/
├── presentation/
│   ├── navigation/
│   │   └── WarkitNavGraph.kt
│   ├── dashboard/
│   │   ├── DashboardScreen.kt
│   │   └── DashboardViewModel.kt
│   ├── customer/
│   │   ├── list/
│   │   ├── add/
│   │   └── edit/
│   ├── inventory/
│   │   ├── list/
│   │   │   ├── InventoryScreen.kt
│   │   │   └── InventoryViewModel.kt
│   │   ├── add/                    # ← HALAMAN TERPISAH TAMBAH BARANG
│   │   │   ├── AddProductScreen.kt
│   │   │   └── AddProductViewModel.kt
│   │   └── edit/
│   ├── purchase/
│   └── invoice/
├── di/
│   ├── AppModule.kt
│   ├── DatabaseModule.kt
│   └── RepositoryModule.kt
└── ui/
    └── theme/
```

## UI Design Notes

### Inventory Screen dengan FAB
```kotlin
@Composable
fun InventoryScreen(
    onAddProductClick: () -> Unit  // Navigate to AddProductScreen
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProductClick) {
                Icon(Icons.Default.Add, "Tambah Barang")
            }
        }
    ) { padding ->
        // Product list...
    }
}
```

### AddProductScreen (Halaman Terpisah)
- Full screen form
- Fields: nama, SKU, harga, stok, min stok, kategori, deskripsi
- Validasi input
- Save button
- Back navigation

## Synthesized Findings

### Best Practices
- Use StateFlow untuk UI state
- LaunchedEffect untuk one-time events
- Sealed class untuk navigation events
- Data class untuk immutable state

---

## 📷 Fitur Foto Customer

### Implementasi Camera
```kotlin
// AddCustomerScreen dengan Camera
@Composable
fun AddCustomerScreen(
    onTakePhoto: () -> Unit,
    onSelectFromGallery: () -> Unit
) {
    // Photo picker UI
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .clickable { /* show options */ }
    ) {
        if (photoUri != null) {
            AsyncImage(model = photoUri, ...)
        } else {
            Icon(Icons.Default.CameraAlt, ...)
        }
    }
}
```

### Photo Storage
- Simpan foto di app internal storage: `filesDir/customer_photos/`
- Simpan path relatif di database (bukan full URI)
- Gunakan Coil untuk load & cache images

### Permissions Required
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
```

---

## 📊 Fitur Export/Import (Implemented)

### Keputusan Teknis
- **Fastexcel** digunakan untuk generate dan membaca file Excel native `.xlsx`
- Template, import, dan export memakai workbook `.xlsx`
- Reader streaming dipakai agar proses import/update tetap ringan

### Import Inventory
```kotlin
// ExcelHelper.kt - Import Excel .xlsx
fun importFromExcel(context: Context, uri: Uri): ImportResult {
    // Parse .xlsx dengan Fastexcel Reader
    val workbook = ReadableWorkbook(inputStream, ReadingOptions(true, true))
    // Return products list + errors
}

// Template columns:
// Name, SKU, Price, Stock, MinStock, Category, Description
```

### Export Transaksi
```kotlin
// ExcelHelper.kt - Export Excel .xlsx
fun exportInvoicesToExcel(
    context: Context,
    invoices: List<Invoice>,
    includeItems: Boolean = true
): File {
    // Write workbook sheets with Fastexcel
    // Optionally include detail items sheet
}
```

### UI Components
- **ImportInventoryScreen**: File picker, preview, confirm import
- **ExportScreen**: Date range picker, status filter, summary preview

---

## 🔐 PIN Security Implementation

### EncryptedSharedPreferences Setup
```kotlin
// PinManager.kt
class PinManager(private val context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "warkit_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun hasPin(): Boolean = sharedPreferences.contains(KEY_PIN)
    
    fun savePin(pin: String) {
        val hashedPin = hashPin(pin)
        sharedPreferences.edit().putString(KEY_PIN, hashedPin).apply()
    }
    
    fun verifyPin(pin: String): Boolean {
        val storedHash = sharedPreferences.getString(KEY_PIN, null) ?: return false
        return hashPin(pin) == storedHash
    }
    
    fun changePin(oldPin: String, newPin: String): Boolean {
        if (!verifyPin(oldPin)) return false
        savePin(newPin)
        return true
    }
    
    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    companion object {
        private const val KEY_PIN = "user_pin"
    }
}
```

### PIN Flow in MainActivity
```kotlin
// MainActivity.kt
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject lateinit var pinManager: PinManager
    
    private var isAuthenticated = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WarkitTheme {
                WarkitApp(
                    hasPin = pinManager.hasPin(),
                    isAuthenticated = isAuthenticated,
                    onPinVerified = { isAuthenticated = true }
                )
            }
        }
    }
    
    // Auto-lock saat app di-minimize
    override fun onPause() {
        super.onPause()
        isAuthenticated = false  // Lock app
    }
}
```

### SetupPinScreen & PinEntryScreen
```kotlin
@Composable
fun SetupPinScreen(onPinCreated: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) } // 1 = enter, 2 = confirm
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(if (step == 1) "Buat PIN Baru" else "Konfirmasi PIN")
        
        PinInput(
            pin = if (step == 1) pin else confirmPin,
            onPinChange = { 
                if (step == 1) pin = it else confirmPin = it
            },
            onComplete = {
                if (step == 1) {
                    step = 2
                } else if (pin == confirmPin) {
                    // Save PIN & navigate
                    onPinCreated()
                }
            }
        )
    }
}

@Composable
fun PinEntryScreen(onPinVerified: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    
    Column(...) {
        Text("Masukkan PIN")
        PinInput(
            pin = pin,
            onPinChange = { pin = it; error = false },
            isError = error,
            onComplete = { enteredPin ->
                if (pinManager.verifyPin(enteredPin)) {
                    onPinVerified()
                } else {
                    error = true
                    pin = ""
                }
            }
        )
    }
}
```

### Package Structure Addition for PIN
```
├── presentation/
│   ├── auth/
│   │   ├── SetupPinScreen.kt
│   │   ├── PinEntryScreen.kt
│   │   └── PinViewModel.kt
│   └── settings/
│       ├── SettingsScreen.kt
│       └── ChangePinScreen.kt
└── data/
    └── security/
        └── PinManager.kt
```

---

## 📄 Invoice Number Format

### Format: `INV-YYYYMMDD-XXX`

| Bagian | Contoh | Keterangan |
|--------|--------|------------|
| Prefix | `INV` | Identitas invoice |
| Separator | `-` | Pemisah |
| Tanggal | `20260113` | Format YYYYMMDD |
| Separator | `-` | Pemisah |
| Sequence | `001` | Urutan per hari (3 digit, reset tiap hari) |

**Contoh Output:**
- `INV-20260113-001` (invoice pertama tanggal 13 Jan 2026)
- `INV-20260113-002` (invoice kedua tanggal 13 Jan 2026)
- `INV-20260114-001` (invoice pertama tanggal 14 Jan 2026, reset)

### Implementation
```kotlin
// InvoiceNumberGenerator.kt
object InvoiceNumberGenerator {
    
    fun generate(existingInvoicesToday: List<Invoice>): String {
        val today = LocalDate.now()
        val dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        
        val nextSequence = if (existingInvoicesToday.isEmpty()) {
            1
        } else {
            // Ambil sequence tertinggi hari ini + 1
            existingInvoicesToday
                .mapNotNull { it.invoiceNumber.takeLast(3).toIntOrNull() }
                .maxOrNull()
                ?.plus(1) ?: 1
        }
        
        return "INV-$dateStr-${nextSequence.toString().padStart(3, '0')}"
    }
}
```
