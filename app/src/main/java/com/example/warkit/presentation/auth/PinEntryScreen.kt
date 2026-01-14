package com.example.warkit.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.warkit.data.security.PinManager
import com.example.warkit.presentation.components.PinInput
import com.example.warkit.presentation.components.PinKeypad
import kotlinx.coroutines.delay

/**
 * Screen for entering PIN to unlock the app
 */
@Composable
fun PinEntryScreen(
    pinManager: PinManager,
    onPinVerified: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var attempts by remember { mutableIntStateOf(0) }
    var isLocked by remember { mutableStateOf(false) }
    var lockTimeRemaining by remember { mutableIntStateOf(0) }
    
    // Hide keyboard
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    
    LaunchedEffect(Unit) {
        keyboardController?.hide()
        focusManager.clearFocus()
    }
    
    // Lock timer
    LaunchedEffect(isLocked) {
        if (isLocked) {
            while (lockTimeRemaining > 0) {
                delay(1000)
                lockTimeRemaining--
            }
            isLocked = false
            attempts = 0
        }
    }
    
    // Auto-verify when PIN is complete
    LaunchedEffect(pin) {
        if (pin.length == PinManager.PIN_LENGTH && !isLocked) {
            if (pinManager.verifyPin(pin)) {
                onPinVerified()
            } else {
                error = true
                attempts++
                pin = ""
                
                // Lock after 5 failed attempts
                if (attempts >= 5) {
                    isLocked = true
                    lockTimeRemaining = 30 // 30 seconds lock
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Masukkan PIN",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Masukkan 6 digit PIN untuk membuka aplikasi",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        PinInput(
            pin = pin,
            isError = error,
            modifier = Modifier.height(48.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when {
            isLocked -> {
                Text(
                    text = "Terlalu banyak percobaan. Coba lagi dalam ${lockTimeRemaining} detik",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
            error -> {
                Text(
                    text = "PIN salah. Sisa percobaan: ${5 - attempts}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        PinKeypad(
            onNumberClick = { number ->
                if (!isLocked && pin.length < PinManager.PIN_LENGTH) {
                    error = false
                    pin += number
                }
            },
            onDeleteClick = {
                if (!isLocked && pin.isNotEmpty()) {
                    pin = pin.dropLast(1)
                }
            },
            enabled = !isLocked
        )
    }
}
