package com.example.warkit.presentation.auth

import androidx.compose.runtime.*
import com.example.warkit.data.security.PinManager

/**
 * Auth state for the application
 */
sealed class AuthState {
    /** PIN belum di-setup, perlu buat PIN baru */
    object NeedSetup : AuthState()
    
    /** PIN sudah ada, perlu unlock */
    object Locked : AuthState()
    
    /** Sudah authenticated */
    object Authenticated : AuthState()
}

/**
 * Composable that manages authentication flow
 */
@Composable
fun AuthGate(
    pinManager: PinManager,
    isAuthenticated: Boolean,
    onAuthStateChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    val authState = remember(isAuthenticated) {
        when {
            isAuthenticated -> AuthState.Authenticated
            !pinManager.hasPin() -> AuthState.NeedSetup
            else -> AuthState.Locked
        }
    }
    
    when (authState) {
        AuthState.NeedSetup -> {
            SetupPinScreen(
                pinManager = pinManager,
                onPinCreated = { onAuthStateChange(true) }
            )
        }
        
        AuthState.Locked -> {
            PinEntryScreen(
                pinManager = pinManager,
                onPinVerified = { onAuthStateChange(true) }
            )
        }
        
        AuthState.Authenticated -> {
            content()
        }
    }
}
