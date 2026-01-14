package com.example.warkit.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Helper for managing customer photos
 */
object PhotoHelper {
    
    private const val PHOTOS_DIR = "customer_photos"
    
    /**
     * Get the photos directory, creating it if needed
     */
    private fun getPhotosDir(context: Context): File {
        val dir = File(context.filesDir, PHOTOS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    /**
     * Save image from URI to internal storage
     * Returns the relative path to the saved file
     */
    fun savePhoto(context: Context, sourceUri: Uri): String? {
        return try {
            val fileName = "customer_${UUID.randomUUID()}.jpg"
            val destFile = File(getPhotosDir(context), fileName)
            
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Return relative path
            "$PHOTOS_DIR/$fileName"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Save photo from file (e.g., camera capture) to internal storage
     * Returns the relative path to the saved file
     */
    fun savePhotoFromFile(context: Context, sourceFile: File): String? {
        return try {
            val fileName = "customer_${UUID.randomUUID()}.jpg"
            val destFile = File(getPhotosDir(context), fileName)
            
            sourceFile.inputStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Clean up temp file
            sourceFile.delete()
            
            // Return relative path
            "$PHOTOS_DIR/$fileName"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Get absolute file path from relative path
     */
    fun getAbsolutePath(context: Context, relativePath: String): File {
        return File(context.filesDir, relativePath)
    }
    
    /**
     * Delete photo by relative path
     */
    fun deletePhoto(context: Context, relativePath: String?): Boolean {
        if (relativePath == null) return false
        return try {
            val file = File(context.filesDir, relativePath)
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Create a temporary file for camera capture
     */
    fun createTempPhotoFile(context: Context): File {
        val fileName = "temp_${System.currentTimeMillis()}.jpg"
        return File(context.cacheDir, fileName)
    }
    
    /**
     * Get Uri for camera capture using FileProvider
     */
    fun getTempPhotoUri(context: Context, tempFile: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }
}
