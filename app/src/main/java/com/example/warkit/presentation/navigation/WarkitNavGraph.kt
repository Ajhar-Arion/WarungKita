package com.example.warkit.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.warkit.data.local.WarkitDatabase
import com.example.warkit.data.repository.CustomerRepositoryImpl
import com.example.warkit.data.repository.InvoiceRepositoryImpl
import com.example.warkit.data.repository.ProductRepositoryImpl
import com.example.warkit.presentation.customer.*
import com.example.warkit.presentation.inventory.*
import com.example.warkit.presentation.invoice.*
import com.example.warkit.presentation.purchase.*
import com.example.warkit.presentation.`import`.*
import com.example.warkit.presentation.export.*
import com.example.warkit.presentation.settlement.*
import com.example.warkit.presentation.components.WarkitTab
import com.example.warkit.util.ExcelHelper
import java.util.Calendar

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object CustomerList : Screen("customer_list")
    object AddCustomer : Screen("add_customer")
    object ImportCustomer : Screen("import_customer")
    object EditCustomer : Screen("edit_customer/{customerId}") {
        fun createRoute(customerId: Long) = "edit_customer/$customerId"
    }
    object InventoryList : Screen("inventory_list")
    object AddProduct : Screen("add_product")
    object EditProduct : Screen("edit_product/{productId}") {
        fun createRoute(productId: Long) = "edit_product/$productId"
    }
    object Purchase : Screen("purchase")
    object SelectCustomer : Screen("select_customer")
    object SelectProduct : Screen("select_product")
    object InvoiceList : Screen("invoice_list")
    object InvoiceDetail : Screen("invoice_detail/{invoiceId}") {
        fun createRoute(invoiceId: Long) = "invoice_detail/$invoiceId"
    }
    object ImportInventory : Screen("import_inventory")
    object ExportTransactions : Screen("export_transactions")
    object Settlement : Screen("settlement")
}

@Composable
fun WarkitNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Purchase.route
) {
    val context = LocalContext.current
    val database = remember { WarkitDatabase.getInstance(context) }
    val customerRepository = remember { CustomerRepositoryImpl(database.customerDao()) }
    val productRepository = remember { ProductRepositoryImpl(database.productDao()) }
    val invoiceRepository = remember { InvoiceRepositoryImpl(database.invoiceDao(), database.customerDao()) }
    
    // Shared purchase viewmodel
    val purchaseViewModel = remember { 
        PurchaseViewModel(customerRepository, productRepository, invoiceRepository) 
    }
    val allInvoices by invoiceRepository.getAllInvoices().collectAsState(initial = emptyList())
    val allCustomers by customerRepository.getAllCustomers().collectAsState(initial = emptyList())
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayInvoices = allInvoices.filter { it.date >= todayStart }
    val customerTotals = allInvoices
        .groupBy { it.customerId }
        .mapValues { entry -> entry.value.sumOf { it.totalAmount } }
    
    fun navigateMain(tab: WarkitTab) {
        val route = when (tab) {
            WarkitTab.Home -> Screen.Purchase.route
            WarkitTab.Products -> Screen.InventoryList.route
            WarkitTab.Customers -> Screen.CustomerList.route
            WarkitTab.History -> Screen.InvoiceList.route
            WarkitTab.Update -> Screen.ImportInventory.route
        }
        navController.navigate(route) {
            popUpTo(Screen.Purchase.route) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onCustomerClick = { navController.navigate(Screen.CustomerList.route) },
                onInventoryClick = { navController.navigate(Screen.InventoryList.route) },
                onPurchaseClick = { navController.navigate(Screen.Purchase.route) },
                onInvoiceClick = { navController.navigate(Screen.InvoiceList.route) },
                onImportClick = { navController.navigate(Screen.ImportInventory.route) },
                onExportClick = { navController.navigate(Screen.ExportTransactions.route) },
                onSettlementClick = { navController.navigate(Screen.Settlement.route) }
            )
        }
        
        // Customer screens
        composable(Screen.CustomerList.route) {
            val viewModel = remember { CustomerListViewModel(customerRepository) }
            val state by viewModel.state.collectAsState()
            
            CustomerListScreen(
                state = state,
                customerTotals = customerTotals,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onAddCustomerClick = { navController.navigate(Screen.AddCustomer.route) },
                onImportCustomerClick = { navController.navigate(Screen.ImportCustomer.route) },
                onCustomerClick = { customerId ->
                    navController.navigate(Screen.EditCustomer.createRoute(customerId))
                },
                onDeleteCustomer = viewModel::deleteCustomer,
                onNavigateBack = { navController.popBackStack() },
                onTabSelected = ::navigateMain
            )
        }
        
        composable(Screen.ImportCustomer.route) {
            val viewModel = remember { ImportCustomerViewModel(customerRepository) }
            
            ImportCustomerScreen(
                state = viewModel.state,
                onFileSelected = { uri -> viewModel.parseFile(context, uri) },
                onConfirmImport = viewModel::confirmImport,
                onDownloadTemplate = {
                    val templateFile = ExcelHelper.generateCustomerTemplate(context)
                    ExcelHelper.openCustomerTemplate(context, templateFile)
                },
                onSaveTemplate = { uri ->
                    runCatching {
                        ExcelHelper.saveCustomerTemplate(context, uri)
                    }.isSuccess
                },
                onNavigateBack = {
                    viewModel.resetState()
                    navController.popBackStack()
                },
                onTabSelected = ::navigateMain
            )
        }
        
        composable(Screen.AddCustomer.route) {
            val viewModel = remember { AddCustomerViewModel(customerRepository) }
            
            AddCustomerScreen(
                state = viewModel.state,
                onNameChange = viewModel::onNameChange,
                onPhoneChange = viewModel::onPhoneChange,
                onEmailChange = viewModel::onEmailChange,
                onAddressChange = viewModel::onAddressChange,
                onPhotoSelected = viewModel::onPhotoSelected,
                onSave = viewModel::saveCustomer,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.EditCustomer.route,
            arguments = listOf(navArgument("customerId") { type = NavType.LongType })
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
            val viewModel = remember { EditCustomerViewModel(customerRepository, customerId) }
            
            EditCustomerScreen(
                state = viewModel.state,
                onNameChange = viewModel::onNameChange,
                onPhoneChange = viewModel::onPhoneChange,
                onEmailChange = viewModel::onEmailChange,
                onAddressChange = viewModel::onAddressChange,
                onPhotoSelected = viewModel::onPhotoSelected,
                onSave = viewModel::saveCustomer,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Inventory screens
        composable(Screen.InventoryList.route) {
            val viewModel = remember { InventoryListViewModel(productRepository) }
            val state by viewModel.state.collectAsState()
            
            InventoryListScreen(
                state = state,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onCategorySelected = viewModel::onCategorySelected,
                onFilterChange = viewModel::onFilterChange,
                onAddProductClick = { navController.navigate(Screen.AddProduct.route) },
                onProductClick = { productId ->
                    navController.navigate(Screen.EditProduct.createRoute(productId))
                },
                onDeleteProduct = viewModel::deleteProduct,
                onNavigateBack = { navController.popBackStack() },
                onTabSelected = ::navigateMain
            )
        }
        
        composable(Screen.AddProduct.route) {
            val viewModel = remember { AddProductViewModel(productRepository) }
            
            AddProductScreen(
                state = viewModel.state,
                onNameChange = viewModel::onNameChange,
                onSkuChange = viewModel::onSkuChange,
                onPriceChange = viewModel::onPriceChange,
                onStockChange = viewModel::onStockChange,
                onMinStockChange = viewModel::onMinStockChange,
                onCategoryChange = viewModel::onCategoryChange,
                onDescriptionChange = viewModel::onDescriptionChange,
                onSave = viewModel::saveProduct,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.EditProduct.route,
            arguments = listOf(navArgument("productId") { type = NavType.LongType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getLong("productId") ?: 0L
            val viewModel = remember { EditProductViewModel(productRepository, productId) }
            
            EditProductScreen(
                state = viewModel.state,
                onNameChange = viewModel::onNameChange,
                onSkuChange = viewModel::onSkuChange,
                onPriceChange = viewModel::onPriceChange,
                onStockChange = viewModel::onStockChange,
                onMinStockChange = viewModel::onMinStockChange,
                onCategoryChange = viewModel::onCategoryChange,
                onDescriptionChange = viewModel::onDescriptionChange,
                onSave = viewModel::saveProduct,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Purchase screens
        composable(Screen.Purchase.route) {
            PurchaseScreen(
                state = purchaseViewModel.state,
                todayTotal = todayInvoices.sumOf { it.totalAmount },
                todayTransactionCount = todayInvoices.size,
                customerCount = allCustomers.size,
                onSelectCustomerClick = { navController.navigate(Screen.SelectCustomer.route) },
                onSelectProductClick = { navController.navigate(Screen.SelectProduct.route) },
                onRemoveFromCart = purchaseViewModel::removeFromCart,
                onIncreaseQuantity = purchaseViewModel::increaseQuantity,
                onDecreaseQuantity = purchaseViewModel::decreaseQuantity,
                onNotesChange = purchaseViewModel::onNotesChange,
                onCheckout = purchaseViewModel::checkout,
                onNavigateBack = { 
                    purchaseViewModel.resetPurchase()
                    navController.popBackStack() 
                },
                onViewInvoice = { invoiceId ->
                    purchaseViewModel.resetPurchase()
                    navController.navigate(Screen.InvoiceDetail.createRoute(invoiceId)) {
                        popUpTo(Screen.Purchase.route)
                    }
                },
                onTabSelected = ::navigateMain
            )
        }
        
        composable(Screen.SelectCustomer.route) {
            val state by purchaseViewModel.customersState.collectAsState()
            
            SelectCustomerScreen(
                state = state,
                onCustomerSelected = { customer ->
                    purchaseViewModel.selectCustomer(customer)
                    navController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.SelectProduct.route) {
            val state by purchaseViewModel.productsState.collectAsState()
            
            SelectProductScreen(
                state = state,
                cartItems = purchaseViewModel.state.cartItems,
                onProductSelected = { product ->
                    purchaseViewModel.addToCart(product)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Invoice screens
        composable(Screen.InvoiceList.route) {
            val viewModel = remember { InvoiceListViewModel(invoiceRepository) }
            val state by viewModel.state.collectAsState()
            
            InvoiceListScreen(
                state = state,
                onStatusFilterChange = viewModel::onStatusFilterChange,
                onInvoiceClick = { invoiceId ->
                    navController.navigate(Screen.InvoiceDetail.createRoute(invoiceId))
                },
                onDownloadHistory = { period ->
                    viewModel.exportHistory(context, period, allCustomers)
                },
                onClearExportMessage = viewModel::clearExportMessage,
                onNavigateBack = { navController.popBackStack() },
                onTabSelected = ::navigateMain
            )
        }
        
        composable(
            route = Screen.InvoiceDetail.route,
            arguments = listOf(navArgument("invoiceId") { type = NavType.LongType })
        ) { backStackEntry ->
            val invoiceId = backStackEntry.arguments?.getLong("invoiceId") ?: 0L
            val viewModel = remember { InvoiceDetailViewModel(invoiceRepository, invoiceId) }
            
            InvoiceDetailScreen(
                state = viewModel.state,
                onUpdateStatus = viewModel::updateStatus,
                onSharePdf = {
                    viewModel.state.invoice?.let { invoice ->
                        val pdfFile = PdfGenerator.generateInvoicePdf(context, invoice)
                        PdfGenerator.shareInvoicePdf(context, pdfFile)
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Import Inventory screen
        composable(Screen.ImportInventory.route) {
            val viewModel = remember { ImportInventoryViewModel(productRepository) }
            val products by productRepository.getAllProducts().collectAsState(initial = emptyList())
            
            ImportInventoryScreen(
                state = viewModel.state,
                onFileSelected = { uri -> viewModel.parseFile(context, uri) },
                onConfirmImport = { updateStock -> viewModel.confirmImport(updateStock) },
                onDismissDuplicateDialog = viewModel::dismissDuplicateDialog,
                onDownloadTemplate = {
                    val templateFile = ExcelHelper.generateTemplate(context)
                    ExcelHelper.openTemplate(context, templateFile)
                },
                onExportProducts = {
                    val exportFile = ExcelHelper.exportInventoryToExcel(context, products)
                    ExcelHelper.openExportFile(context, exportFile)
                },
                onNavigateBack = { 
                    viewModel.resetState()
                    navController.popBackStack() 
                },
                onTabSelected = ::navigateMain
            )
        }
        
        // Export Transactions screen
        composable(Screen.ExportTransactions.route) {
            val viewModel = remember { ExportViewModel(invoiceRepository) }
            
            ExportScreen(
                state = viewModel.state,
                onStartDateChange = viewModel::onStartDateChange,
                onEndDateChange = viewModel::onEndDateChange,
                onStatusFilterChange = viewModel::onStatusFilterChange,
                onIncludeItemsChange = viewModel::onIncludeItemsChange,
                onExport = { viewModel.exportToFile(context) },
                onOpenFile = { viewModel.openExportedFile(context) },
                onResetExport = viewModel::resetExport,
                onNavigateBack = { 
                    viewModel.resetExport()
                    navController.popBackStack() 
                }
            )
        }
        
        // Settlement screen
        composable(Screen.Settlement.route) {
            val viewModel = remember { SettlementViewModel(invoiceRepository, customerRepository) }
            val state by viewModel.state.collectAsState()
            
            SettlementScreen(
                state = state,
                onCustomerFilterChange = viewModel::onCustomerFilterChange,
                onToggleSelection = viewModel::toggleInvoiceSelection,
                onSelectAll = viewModel::selectAll,
                onClearSelection = viewModel::clearSelection,
                onSettleSelected = viewModel::settleSelected,
                onSettleSingle = viewModel::settleSingle,
                onClearMessages = viewModel::clearMessages,
                onNavigateBack = { navController.popBackStack() },
                onViewInvoice = { invoiceId ->
                    navController.navigate(Screen.InvoiceDetail.createRoute(invoiceId))
                }
            )
        }
    }
}
