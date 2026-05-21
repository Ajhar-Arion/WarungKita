package com.example.warkit.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = WarungGreenMuted,
    onPrimary = WarungDarkSurface,
    primaryContainer = WarungGreenDark,
    onPrimaryContainer = Color.White,
    secondary = WarungOrange,
    tertiary = WarungBlue,
    background = WarungDarkBackground,
    surface = WarungDarkSurface,
    surfaceVariant = Color(0xFF1B271F),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFC5CEC7),
    outline = Color(0xFF4D5C51),
    error = WarungRed
)

private val LightColorScheme = lightColorScheme(
    primary = WarungGreen,
    onPrimary = Color.White,
    primaryContainer = WarungGreenSoft,
    onPrimaryContainer = WarungGreenDark,
    secondary = WarungOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF0DD),
    onSecondaryContainer = Color(0xFF5F3700),
    tertiary = WarungBlue,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8F0FF),
    onTertiaryContainer = Color(0xFF0E3A7E),
    background = WarungBackground,
    onBackground = WarungText,
    surface = WarungSurface,
    onSurface = WarungText,
    surfaceVariant = Color(0xFFF1F4F1),
    onSurfaceVariant = WarungTextMuted,
    outline = WarungOutline,
    error = WarungRed,
    errorContainer = Color(0xFFFFEDEC),
    onErrorContainer = Color(0xFF7A1512)

    /*
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    */
)

@Composable
fun WarkitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
