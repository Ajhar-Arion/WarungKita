# Task Plan: Aplikasi Warkit - Manajemen Pembelian, Inventory & Invoice

## Goal
Membangun aplikasi Android lengkap untuk manajemen pembelian customer, kontrol inventory dengan fitur tambah barang, serta pembuatan invoice. Aplikasi berjalan **single user** dengan **PIN security** dan **offline-first** (tidak sync ke cloud).

## Phases
- [x] Phase 1: Setup PIN Security & Auth Flow ✅
- [x] Phase 2: Setup Database (Room) ✅
- [x] Phase 3: Setup Repository & Use Cases ✅
- [x] Phase 4: Implementasi Modul Customer + Foto Customer ✅
- [x] Phase 5: Implementasi Modul Inventory + Fitur Tambah Barang ✅
- [x] Phase 6: Implementasi Modul Purchase ✅
- [x] Phase 7: Implementasi Modul Invoice + PDF Generation ✅
- [x] Phase 8: Implementasi Import Inventory (Excel .xlsx) ✅
- [x] Phase 9: Implementasi Export Excel (Transaksi) ✅
- [ ] Phase 10: Testing & Polish

## Fitur Utama

### 0. PIN Security 🔐 ✅
- [x] **Setup PIN saat pertama kali buka aplikasi**
- [x] **PIN Screen setiap kali aplikasi dibuka**
- [x] **Auto-lock saat aplikasi ditutup/minimize**
- [x] PIN tersimpan lokal (encrypted dengan EncryptedSharedPreferences)
- [ ] Opsi ganti PIN di settings (optional - future)
- [x] Tidak ada forgot PIN (reset = clear data)

### 1. Customer Management ✅
- [x] List semua customer
- [x] Tambah customer baru
- [x] **📷 Ambil foto customer dari Kamera**
- [x] **📷 Pilih foto dari Galeri**
- [x] Edit customer (termasuk update foto)
- [x] Hapus customer
- [x] Search customer
- [x] Tampilkan foto customer di list dan detail

### 2. Inventory Management ✅
- [x] List semua produk
- [x] **Tombol FAB untuk tambah barang → halaman terpisah (AddProductScreen)**
- [x] Edit produk
- [x] Hapus produk
- [x] Filter by kategori
- [x] Alert stok rendah (visual indicator)
- [x] Search produk
- [ ] Barcode scanner (optional - future)

### 3. Purchase Management ✅
- [x] Pilih customer
- [x] Tambah produk ke keranjang
- [x] Hitung total otomatis
- [x] Kurangi stok otomatis
- [x] Generate invoice

### 4. Invoice Management ✅
- [x] List semua invoice
- [x] Detail invoice dengan items
- [x] Generate PDF
- [x] Share/Print invoice
- [x] Filter by status (PAID/PENDING/CANCELLED)
- [x] Update status invoice
- [x] **Format nomor invoice: `INV-YYYYMMDD-XXX`** (contoh: INV-20260113-001)

### 5. Export Data (Excel) ✅
- [x] **📊 Export data transaksi ke file Excel .xlsx**
- [x] Pilih rentang tanggal export (DatePicker)
- [x] Filter by status (All/Paid/Pending/Cancelled)
- [x] Include detail items per invoice (toggle)
- [x] Preview summary sebelum export
- [x] Share file Excel .xlsx

### 6. Import Data Inventory ✅
- [x] **📥 Import data produk dari file Excel .xlsx**
- [x] Template .xlsx yang bisa diedit di Excel
- [x] Download template dari aplikasi
- [x] Preview data sebelum import
- [x] Support multiple item sekaligus
- [x] Error handling per baris
- [x] Validasi kolom wajib (Name, Price, Stock)

## Arsitektur

```
MVVM + Clean Architecture
├── data/           # Database, DAOs, Repository Impl
├── domain/         # Models, Repository Interface, Use Cases
├── presentation/   # ViewModels, Screens, Components
└── di/             # Dependency Injection (Hilt)
```

## Database Schema

### Entities
1. **Customer** - id, name, phone, email, address, **photoPath**, createdAt
2. **Product** - id, name, sku, price, stock, minStock, category, description, createdAt
3. **Invoice** - id, invoiceNumber, customerId, totalAmount, date, status, notes
4. **InvoiceItem** - id, invoiceId, productId, quantity, unitPrice, subtotal

## Screen Navigation

```
[App Start]
    │
    ▼
┌─────────────────────────────────────┐
│  PIN tidak ada? → SetupPinScreen    │
│  PIN ada? → PinEntryScreen          │
└─────────────────────────────────────┘
    │ (PIN valid)
    ▼
MainScreen (Dashboard)
├── CustomerListScreen
│   ├── AddCustomerScreen (dengan foto)
│   └── EditCustomerScreen
├── InventoryScreen
│   ├── AddProductScreen (FAB → halaman terpisah)
│   └── EditProductScreen
├── PurchaseScreen
│   └── SelectProductScreen
├── InvoiceListScreen
│   └── InvoiceDetailScreen
├── ExportScreen
└── SettingsScreen
    └── ChangePinScreen
```

## Key Questions
1. ~~Apakah perlu multi-user/login?~~ → **Single user dengan PIN**
2. ~~Apakah perlu sync ke cloud?~~ → **Tidak, offline-first (local only)**
3. ~~Format nomor invoice?~~ → **INV-YYYYMMDD-XXX**

## Decisions Made
- [Jetpack Compose]: UI modern dan deklaratif
- [Room Database]: Local storage yang reliable
- [MVVM]: Separation of concerns yang baik
- [Hilt]: DI yang recommended oleh Google
- [Single Activity]: Navigation Compose
- [EncryptedSharedPreferences]: Untuk simpan PIN dengan aman
- [Single User + PIN]: Keamanan sederhana tanpa server
- [Offline-First]: Semua data lokal, tidak sync ke cloud
- [Context7 MCP]: Akses dokumentasi terbaru untuk semua library

## 📚 Context7 - Dokumentasi Terbaru

### Cara Akses
Context7 tersedia sebagai MCP (Model Context Protocol) untuk mengakses dokumentasi terbaru dari library/framework yang digunakan.

### Library IDs untuk Project Ini (Verified)
| Library | Context7 ID | Code Snippets | Kegunaan |
|---------|-------------|---------------|----------|
| Jetpack Compose | `/websites/developer_android_develop_ui_compose` | 241,862 | UI Components, State, Effects |
| Compose Samples | `/android/compose-samples` | 83 | Code examples & patterns |
| AndroidX (Room, etc) | `/androidx/androidx` | 232,075 | Room, Navigation, Lifecycle |
| Android Jetpack | `/websites/developer_android_jetpack_androidx` | 35,068 | Official Android docs |
| Architecture Samples | `/android/architecture-samples` | 44 | MVVM + Room + Hilt patterns |
| Dagger Hilt | `/websites/dagger_dev_hilt` | 138 | Dependency injection |
| Accompanist | `/google/accompanist` | 110 | Compose utilities |

### Workflow dengan Context7
1. **Sebelum implementasi fitur** → Query Context7 untuk dokumentasi terbaru
2. **Saat menemukan error** → Cek Context7 untuk breaking changes/migration guides
3. **Update dependencies** → Verify API changes dari Context7

### Contoh Query Context7
```
# Untuk Room migrations
Context7-query-docs: libraryId="/androidx/androidx", query="Room database migration"

# Untuk Compose state management
Context7-query-docs: libraryId="/websites/developer_android_develop_ui_compose", query="state hoisting remember"

# Untuk Hilt modules
Context7-query-docs: libraryId="/websites/dagger_dev_hilt", query="hilt module viewmodel"

# Untuk Architecture patterns
Context7-query-docs: libraryId="/android/architecture-samples", query="repository pattern flow"
```

## Environment Setup

### ☕ Java Status
| Item | Status | Notes |
|------|--------|-------|
| JDK Installed | ✅ **OpenJDK 21.0.8** | Android Studio JBR |
| JDK Path | `C:\Program Files\Android\Android Studio\jbr` | JetBrains Runtime |
| Gradle | ✅ **8.13** | Working! |
| Kotlin | ✅ 2.0.21 | Via Gradle |

### JAVA_HOME untuk Build
```powershell
# Set sebelum menjalankan Gradle commands:
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

## Dependencies Yang Dibutuhkan
```kotlin
// Room, Navigation, Hilt, PDF (iText), MLKit Barcode
// CameraX (untuk foto customer)
// Fastexcel (untuk import/export Excel .xlsx)
// Security Crypto (untuk PIN encryption)
```

## Analisis Teknologi

### 🔐 PIN Security - Technology Analysis

| Opsi | Library | Pros | Cons | **Keputusan** |
|------|---------|------|------|---------------|
| EncryptedSharedPreferences | `androidx.security:security-crypto` | Official Android, AES-256, simple API | Butuh API 23+ | ✅ **DIPILIH** |
| DataStore Encrypted | `androidx.datastore` + crypto | Modern, Flow-based | Lebih kompleks | ❌ |
| SQLCipher | `net.zetetic:android-database-sqlcipher` | Enkripsi seluruh DB | Overkill untuk PIN | ❌ |
| Manual Hashing | SHA-256/BCrypt | Ringan | Kurang aman untuk storage | ❌ |

**Alasan pilih EncryptedSharedPreferences:**
- Official dari Android Jetpack Security
- AES-256-GCM encryption
- Simple key-value storage cocok untuk PIN
- Min SDK 23 sesuai dengan project (minSdk = 23)

### 📄 Invoice Format - INV-YYYYMMDD-XXX

```kotlin
// Invoice Number Generator
object InvoiceNumberGenerator {
    suspend fun generate(lastInvoiceToday: Invoice?): String {
        val today = LocalDate.now()
        val dateStr = today.format(DateTimeFormatter.BASIC_ISO_DATE) // 20260113
        
        val sequence = if (lastInvoiceToday != null) {
            val lastSeq = lastInvoiceToday.invoiceNumber.takeLast(3).toInt()
            lastSeq + 1
        } else {
            1
        }
        
        return "INV-$dateStr-${sequence.toString().padStart(3, '0')}"
        // Output: INV-20260113-001, INV-20260113-002, ...
    }
}
```

### 📊 Semua Teknologi Yang Digunakan

| Kategori | Teknologi | Versi | Fungsi |
|----------|-----------|-------|--------|
| **UI** | Jetpack Compose | BOM 2024.x | Modern declarative UI |
| **Architecture** | MVVM + Clean | - | Separation of concerns |
| **DI** | Hilt | 2.50 | Dependency Injection |
| **Database** | Room | 2.6.1 | Local SQLite wrapper |
| **Navigation** | Navigation Compose | 2.7.7 | Screen navigation |
| **Security** | EncryptedSharedPreferences | 1.1.0-alpha06 | PIN storage |
| **Camera** | CameraX | 1.3.1 | Foto customer |
| **Image Loading** | Coil | 2.5.0 | Load & cache images |
| **PDF** | iText7 | 7.2.5 | Generate invoice PDF |
| **Excel** | Fastexcel | 0.20.0 | Import/export .xlsx |
| **Barcode** | MLKit | 17.2.0 | Scan barcode (optional) |

## Errors Encountered
- (akan diupdate selama development)

## Status
**Phase 9 COMPLETED** ✅ - All Core Features Implemented!

### 📱 App Summary
| Feature | Status | Description |
|---------|--------|-------------|
| PIN Security | ✅ | Setup, Entry, Auto-lock |
| Customer Management | ✅ | CRUD + Photo + Search |
| Inventory Management | ✅ | CRUD + Filter + Low Stock Alert |
| Purchase/Cart | ✅ | Customer + Products + Checkout |
| Invoice | ✅ | List + Detail + PDF + Status |
| Import Inventory | ✅ | XLSX + Template + Preview |
| Export Transaksi | ✅ | XLSX + Date Range + Filter |

### Phase 9 Deliverables (Export Transaksi):
- `ExportViewModel.kt` - State management for export
- `ExportScreen.kt` - UI with date picker, filters, preview
- Updated `ExcelHelper.kt` - Added invoice export functions
- Updated `DashboardScreen.kt` - Added Export menu
- Updated `WarkitNavGraph.kt` - Added export route

### Phase 8 Deliverables (Import Inventory):
- `ExcelHelper.kt` - XLSX import/export utility with Fastexcel
- `ImportInventoryViewModel.kt` - State management for import
- `ImportInventoryScreen.kt` - UI for file selection, preview, confirm

**APK Size: 19.10 MB**

### 🚀 Optional Future Enhancements (Phase 10):
- [ ] Change PIN in Settings
- [ ] Barcode scanner for products
- [ ] Customer transaction history
- [ ] Dashboard statistics/charts
- [ ] Dark mode support
- [ ] Data backup/restore
