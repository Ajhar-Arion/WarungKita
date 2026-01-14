package com.example.warkit.data.repository

import com.example.warkit.data.local.dao.CustomerDao
import com.example.warkit.domain.model.Customer
import com.example.warkit.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CustomerRepositoryImpl(
    private val customerDao: CustomerDao
) : CustomerRepository {
    
    override fun getAllCustomers(): Flow<List<Customer>> {
        return customerDao.getAllCustomers().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun searchCustomers(query: String): Flow<List<Customer>> {
        return customerDao.searchCustomers(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getCustomerById(id: Long): Customer? {
        return customerDao.getCustomerById(id)?.toDomain()
    }
    
    override suspend fun insertCustomer(customer: Customer): Long {
        return customerDao.insertCustomer(customer.toEntity())
    }
    
    override suspend fun updateCustomer(customer: Customer) {
        customerDao.updateCustomer(customer.toEntity())
    }
    
    override suspend fun deleteCustomer(id: Long) {
        customerDao.deleteCustomerById(id)
    }
}
