package com.example.warkit.presentation.customer

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warkit.domain.model.Customer
import com.example.warkit.domain.repository.CustomerRepository
import com.example.warkit.util.ExcelHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

data class CustomerListState(
    val customers: List<Customer> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

class CustomerListViewModel(
    private val customerRepository: CustomerRepository
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    
    val state: StateFlow<CustomerListState> = combine(
        customerRepository.getAllCustomers(),
        _searchQuery
    ) { customers, query ->
        val filtered = if (query.isBlank()) {
            customers
        } else {
            customers.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.phone.contains(query, ignoreCase = true)
            }
        }
        CustomerListState(
            customers = filtered,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CustomerListState()
    )
    
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
    
    fun deleteCustomer(customerId: Long) {
        viewModelScope.launch {
            customerRepository.deleteCustomer(customerId)
        }
    }
}

data class ImportCustomerState(
    val isLoading: Boolean = false,
    val previewCustomers: List<Customer> = emptyList(),
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val errors: List<String> = emptyList(),
    val showPreview: Boolean = false,
    val importComplete: Boolean = false,
    val importedCount: Int = 0,
    val skippedDuplicateCount: Int = 0,
    val message: String = ""
)

class ImportCustomerViewModel(
    private val customerRepository: CustomerRepository
) : ViewModel() {
    
    var state by mutableStateOf(ImportCustomerState())
        private set
    
    fun parseFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, errors = emptyList(), importComplete = false)
            
            try {
                val result = ExcelHelper.importCustomersFromExcel(context, uri)
                state = state.copy(
                    isLoading = false,
                    previewCustomers = result.customers,
                    successCount = result.successCount,
                    failedCount = result.failedCount,
                    errors = result.errors,
                    showPreview = result.customers.isNotEmpty(),
                    message = if (result.customers.isEmpty()) "Tidak ada customer valid untuk diimport" else ""
                )
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    errors = listOf("Error: ${e.message}"),
                    showPreview = false
                )
            }
        }
    }
    
    fun confirmImport() {
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            
            val existingKeys = customerRepository.getAllCustomers()
                .first()
                .map { it.importKey() }
                .toMutableSet()
            val fileKeys = mutableSetOf<String>()
            var imported = 0
            var skipped = 0
            val newErrors = mutableListOf<String>()
            
            state.previewCustomers.forEach { customer ->
                val key = customer.importKey()
                if (key in existingKeys || key in fileKeys) {
                    skipped++
                    return@forEach
                }
                
                try {
                    customerRepository.insertCustomer(customer)
                    imported++
                    existingKeys.add(key)
                    fileKeys.add(key)
                } catch (e: Exception) {
                    newErrors.add("Customer '${customer.name}': ${e.message}")
                }
            }
            
            state = state.copy(
                isLoading = false,
                showPreview = false,
                importComplete = true,
                importedCount = imported,
                skippedDuplicateCount = skipped,
                errors = state.errors + newErrors,
                message = "$imported customer berhasil diimport"
            )
        }
    }
    
    fun resetState() {
        state = ImportCustomerState()
    }
    
    private fun Customer.importKey(): String {
        val normalizedPhone = phone.trim()
        return if (normalizedPhone.isNotBlank()) {
            "phone:${normalizedPhone.lowercase(Locale.ROOT)}"
        } else {
            "name:${name.trim().lowercase(Locale.ROOT)}"
        }
    }
}

// Add Customer State
data class AddCustomerState(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val photoUri: Uri? = null,
    val photoPath: String? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class AddCustomerViewModel(
    private val customerRepository: CustomerRepository
) : ViewModel() {
    
    var state by mutableStateOf(AddCustomerState())
        private set
    
    fun onNameChange(name: String) {
        state = state.copy(name = name, errorMessage = null)
    }
    
    fun onPhoneChange(phone: String) {
        state = state.copy(phone = phone)
    }
    
    fun onEmailChange(email: String) {
        state = state.copy(email = email)
    }
    
    fun onAddressChange(address: String) {
        state = state.copy(address = address)
    }
    
    fun onPhotoSelected(uri: Uri?, savedPath: String?) {
        state = state.copy(photoUri = uri, photoPath = savedPath)
    }
    
    fun saveCustomer() {
        if (state.name.isBlank()) {
            state = state.copy(errorMessage = "Nama customer tidak boleh kosong")
            return
        }
        
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            try {
                val customer = Customer(
                    name = state.name.trim(),
                    phone = state.phone.trim(),
                    email = state.email.trim(),
                    address = state.address.trim(),
                    photoPath = state.photoPath
                )
                customerRepository.insertCustomer(customer)
                state = state.copy(isLoading = false, isSaved = true)
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false, 
                    errorMessage = "Gagal menyimpan customer: ${e.message}"
                )
            }
        }
    }
}

// Edit Customer State
data class EditCustomerState(
    val customerId: Long = 0,
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val photoUri: Uri? = null,
    val photoPath: String? = null,
    val originalPhotoPath: String? = null,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class EditCustomerViewModel(
    private val customerRepository: CustomerRepository,
    private val customerId: Long
) : ViewModel() {
    
    var state by mutableStateOf(EditCustomerState(customerId = customerId))
        private set
    
    init {
        loadCustomer()
    }
    
    private fun loadCustomer() {
        viewModelScope.launch {
            val customer = customerRepository.getCustomerById(customerId)
            if (customer != null) {
                state = state.copy(
                    name = customer.name,
                    phone = customer.phone,
                    email = customer.email,
                    address = customer.address,
                    photoPath = customer.photoPath,
                    originalPhotoPath = customer.photoPath,
                    isLoading = false
                )
            } else {
                state = state.copy(
                    isLoading = false,
                    errorMessage = "Customer tidak ditemukan"
                )
            }
        }
    }
    
    fun onNameChange(name: String) {
        state = state.copy(name = name, errorMessage = null)
    }
    
    fun onPhoneChange(phone: String) {
        state = state.copy(phone = phone)
    }
    
    fun onEmailChange(email: String) {
        state = state.copy(email = email)
    }
    
    fun onAddressChange(address: String) {
        state = state.copy(address = address)
    }
    
    fun onPhotoSelected(uri: Uri?, savedPath: String?) {
        state = state.copy(photoUri = uri, photoPath = savedPath)
    }
    
    fun saveCustomer() {
        if (state.name.isBlank()) {
            state = state.copy(errorMessage = "Nama customer tidak boleh kosong")
            return
        }
        
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            try {
                val customer = Customer(
                    id = state.customerId,
                    name = state.name.trim(),
                    phone = state.phone.trim(),
                    email = state.email.trim(),
                    address = state.address.trim(),
                    photoPath = state.photoPath
                )
                customerRepository.updateCustomer(customer)
                state = state.copy(isLoading = false, isSaved = true)
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    errorMessage = "Gagal menyimpan customer: ${e.message}"
                )
            }
        }
    }
}
