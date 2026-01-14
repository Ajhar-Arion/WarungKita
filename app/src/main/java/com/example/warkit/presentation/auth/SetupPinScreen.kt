package com.example.warkit.presentation.auth

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.warkit.data.security.PinManager
import com.example.warkit.presentation.components.PinInput
import com.example.warkit.presentation.components.PinKeypad

/**
 * Screen for setting up PIN for the first time
 */
@Composable
fun SetupPinScreen(
    pinManager: PinManager,
    onPinCreated: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1 = create, 2 = confirm
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    val currentPin = if (step == 1) pin else confirmPin
    
    // Hide keyboard
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    
    LaunchedEffect(Unit) {
        keyboardController?.hide()
        focusManager.clearFocus()
    }
    
    // Auto-proceed when PIN is complete
    LaunchedEffect(pin) {
        if (pin.length == PinManager.PIN_LENGTH && step == 1) {
            step = 2
        }
    }
    
    LaunchedEffect(confirmPin) {
        if (confirmPin.length == PinManager.PIN_LENGTH && step == 2) {
            if (pin == confirmPin) {
                pinManager.savePin(pin)
                onPinCreated()
            } else {
                error = "PIN tidak cocok"
                confirmPin = ""
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
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = if (step == 1) "Buat PIN Baru" else "Konfirmasi PIN",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (step == 1) 
                "Masukkan 6 digit PIN untuk mengamankan aplikasi" 
            else 
                "Masukkan PIN sekali lagi untuk konfirmasi",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        PinInput(
            pin = currentPin,
            isError = error != null,
            modifier = Modifier.height(48.dp)
        )
        
        if (error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        PinKeypad(
            onNumberClick = { number ->
                error = null
                if (step == 1 && pin.length < PinManager.PIN_LENGTH) {
                    pin += number
                } else if (step == 2 && confirmPin.length < PinManager.PIN_LENGTH) {
                    confirmPin += number
                }
            },
            onDeleteClick = {
                if (step == 1 && pin.isNotEmpty()) {
                    pin = pin.dropLast(1)
                } else if (step == 2 && confirmPin.isNotEmpty()) {
                    confirmPin = confirmPin.dropLast(1)
                }
            }
        )
        
        if (step == 2) {
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = {
                step = 1
                pin = ""
                confirmPin = ""
                error = null
            }) {
                Text("Kembali")
            }
        }
    }
}
