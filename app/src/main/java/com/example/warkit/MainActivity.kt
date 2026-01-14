package com.example.warkit

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.warkit.data.security.PinManager
import com.example.warkit.presentation.auth.AuthGate
import com.example.warkit.presentation.navigation.WarkitNavGraph
import com.example.warkit.ui.theme.WarkitTheme

class MainActivity : ComponentActivity() {
    
    private lateinit var pinManager: PinManager
    private var isAuthenticated by mutableStateOf(false)
    
    // Handler for delayed lock mechanism
    private val lockHandler = Handler(Looper.getMainLooper())
    private var lockRunnable: Runnable? = null
    
    // Lock timeout in milliseconds (30 seconds)
    // This allows enough time for camera, file picker, share dialogs
    private val lockTimeoutMs = 30_000L
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        pinManager = PinManager(applicationContext)
        
        enableEdgeToEdge()
        setContent {
            WarkitTheme {
                // Hide keyboard when not authenticated (PIN screens)
                LaunchedEffect(isAuthenticated) {
                    if (!isAuthenticated) {
                        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
                    } else {
                        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED)
                    }
                }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuthGate(
                        pinManager = pinManager,
                        isAuthenticated = isAuthenticated,
                        onAuthStateChange = { isAuthenticated = it }
                    ) {
                        // Main app content after authentication
                        WarkitNavGraph()
                    }
                }
            }
        }
    }
    
    // Auto-lock when app goes to background (with delay)
    override fun onPause() {
        super.onPause()
        // Start delayed lock - allows time for camera/file picker/share to complete
        scheduleLock()
    }
    
    override fun onResume() {
        super.onResume()
        // Cancel any pending lock when user returns to app
        cancelScheduledLock()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up handler
        cancelScheduledLock()
    }
    
    private fun scheduleLock() {
        // Cancel any existing scheduled lock first
        cancelScheduledLock()
        
        lockRunnable = Runnable {
            isAuthenticated = false
        }
        lockHandler.postDelayed(lockRunnable!!, lockTimeoutMs)
    }
    
    private fun cancelScheduledLock() {
        lockRunnable?.let {
            lockHandler.removeCallbacks(it)
            lockRunnable = null
        }
    }
}