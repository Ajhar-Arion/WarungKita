package com.example.warkit.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.warkit.data.local.dao.CustomerDao
import com.example.warkit.data.local.dao.InvoiceDao
import com.example.warkit.data.local.dao.ProductDao
import com.example.warkit.data.local.entity.CustomerEntity
import com.example.warkit.data.local.entity.InvoiceEntity
import com.example.warkit.data.local.entity.InvoiceItemEntity
import com.example.warkit.data.local.entity.ProductEntity

@Database(
    entities = [
        CustomerEntity::class,
        ProductEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WarkitDatabase : RoomDatabase() {
    
    abstract fun customerDao(): CustomerDao
    abstract fun productDao(): ProductDao
    abstract fun invoiceDao(): InvoiceDao
    
    companion object {
        private const val DATABASE_NAME = "warkit_database"
        
        @Volatile
        private var INSTANCE: WarkitDatabase? = null
        
        fun getInstance(context: Context): WarkitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WarkitDatabase::class.java,
                    DATABASE_NAME
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
