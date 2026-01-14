package com.example.warkit.presentation.customer

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warkit.domain.model.Customer
import com.example.warkit.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
