package com.flowernotes.ui.theme

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

// Palette "flower": rosa/verde come fallback quando i colori dinamici non ci sono
private val LightColors = lightColorScheme(
    primary = Color(0xFFB0286B),
    secondary = Color(0xFF74565F),
    tertiary = Color(0xFF3E6939),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB0CB),
    secondary = Color(0xFFE2BDC6),
    tertiary = Color(0xFFA4D399),
)

@Composable
fun FlowerNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    // Con dynamicColor attivo (Android 12+) l'app usa l'accento Material You del telefono
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
