package com.example.warkit.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

/**
 * Manages PIN storage and verification using EncryptedSharedPreferences.
 * PIN is hashed with SHA-256 before storage for additional security.
 */
class PinManager(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    /**
     * Check if PIN has been set up
     */
    fun hasPin(): Boolean {
        return sharedPreferences.contains(KEY_PIN_HASH)
    }
    
    /**
     * Save a new PIN (hashed)
     */
    fun savePin(pin: String) {
        val hashedPin = hashPin(pin)
        sharedPreferences.edit()
            .putString(KEY_PIN_HASH, hashedPin)
            .apply()
    }
    
    /**
     * Verify entered PIN against stored PIN
     */
    fun verifyPin(pin: String): Boolean {
        val storedHash = sharedPreferences.getString(KEY_PIN_HASH, null) ?: return false
        return hashPin(pin) == storedHash
    }
    
    /**
     * Change PIN (requires old PIN verification)
     */
    fun changePin(oldPin: String, newPin: String): Boolean {
        if (!verifyPin(oldPin)) return false
        savePin(newPin)
        return true
    }
    
    /**
     * Clear PIN (for reset functionality if needed)
     */
    fun clearPin() {
        sharedPreferences.edit()
            .remove(KEY_PIN_HASH)
            .apply()
    }
    
    /**
     * Check if credit popup has been shown
     */
    fun hasCreditBeenShown(): Boolean {
        return sharedPreferences.getBoolean(KEY_CREDIT_SHOWN, false)
    }
    
    /**
     * Mark credit popup as shown
     */
    fun setCreditShown() {
        sharedPreferences.edit()
            .putBoolean(KEY_CREDIT_SHOWN, true)
            .apply()
    }
    
    /**
     * Hash PIN using SHA-256
     */
    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    companion object {
        private const val PREFS_FILE_NAME = "warkit_secure_prefs"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_CREDIT_SHOWN = "credit_shown"
        
        const val PIN_LENGTH = 6
    }
}
