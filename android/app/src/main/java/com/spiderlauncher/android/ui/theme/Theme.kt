package com.spiderlauncher.android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Spider brand colours
val SpiderGreen      = Color(0xFF2ECC71)
val SpiderDarkGreen  = Color(0xFF27AE60)
val SpiderDark       = Color(0xFF0D1117)
val SpiderSurface    = Color(0xFF161B22)
val SpiderCard       = Color(0xFF21262D)

private val DarkColorScheme = darkColorScheme(
    primary          = SpiderGreen,
    onPrimary        = Color(0xFF003919),
    primaryContainer = Color(0xFF005227),
    secondary        = Color(0xFF58A6FF),
    tertiary         = Color(0xFFD2A8FF),
    background       = SpiderDark,
    surface          = SpiderSurface,
    surfaceVariant   = SpiderCard,
    onBackground     = Color(0xFFE6EDF3),
    onSurface        = Color(0xFFE6EDF3),
    onSurfaceVariant = Color(0xFF8B949E),
    error            = Color(0xFFF85149),
    errorContainer   = Color(0xFF490002)
)

private val LightColorScheme = lightColorScheme(
    primary          = SpiderDarkGreen,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFB7F0CC),
    secondary        = Color(0xFF0969DA),
    tertiary         = Color(0xFF8250DF),
    background       = Color(0xFFF6F8FA),
    surface          = Color.White,
    surfaceVariant   = Color(0xFFEAECEF),
    error            = Color(0xFFCF222E)
)

@Composable
fun SpiderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else           dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = SpiderTypography,
        content     = content
    )
}
