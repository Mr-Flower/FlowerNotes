package com.flowernotes.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext

/** Accento colore selezionabile nelle impostazioni */
data class AccentOption(val id: String, val label: String, val seed: Color)

object Accents {
    /** Colori dinamici Material You del telefono (Android 12+) */
    const val DYNAMIC = "dynamic"

    val OPTIONS = listOf(
        AccentOption("rosa", "Rosa", Color(0xFFD81B60)),
        AccentOption("viola", "Viola", Color(0xFF6750A4)),
        AccentOption("blu", "Blu", Color(0xFF1E88E5)),
        AccentOption("verde", "Verde", Color(0xFF43A047)),
        AccentOption("ambra", "Ambra", Color(0xFFEF8F00)),
        AccentOption("rosso", "Rosso", Color(0xFFE53935)),
    )

    fun seedFor(id: String): Color =
        OPTIONS.firstOrNull { it.id == id }?.seed ?: OPTIONS.first().seed

    fun dynamicAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

/**
 * Schemi colore derivati da un colore seme con semplici interpolazioni,
 * senza portarci dietro la libreria material-color-utilities.
 */
private fun lightSchemeFrom(seed: Color): ColorScheme = lightColorScheme(
    primary = seed,
    onPrimary = Color.White,
    primaryContainer = lerp(seed, Color.White, 0.85f),
    onPrimaryContainer = lerp(seed, Color.Black, 0.55f),
    secondary = lerp(seed, Color(0xFF5F5F66), 0.45f),
    onSecondary = Color.White,
    secondaryContainer = lerp(seed, Color.White, 0.80f),
    onSecondaryContainer = lerp(seed, Color.Black, 0.60f),
    tertiary = lerp(seed, Color(0xFF7D6A52), 0.50f),
    onTertiary = Color.White,
    tertiaryContainer = lerp(lerp(seed, Color(0xFF7D6A52), 0.50f), Color.White, 0.80f),
    onTertiaryContainer = lerp(seed, Color.Black, 0.60f),
    surfaceTint = seed,
)

private fun darkSchemeFrom(seed: Color): ColorScheme {
    val primary = lerp(seed, Color.White, 0.55f)
    return darkColorScheme(
        primary = primary,
        onPrimary = lerp(seed, Color.Black, 0.50f),
        primaryContainer = lerp(seed, Color.Black, 0.45f),
        onPrimaryContainer = lerp(seed, Color.White, 0.85f),
        secondary = lerp(primary, Color(0xFFCCC2DC), 0.40f),
        onSecondary = lerp(seed, Color.Black, 0.55f),
        secondaryContainer = lerp(lerp(seed, Color(0xFF5F5F66), 0.40f), Color.Black, 0.45f),
        onSecondaryContainer = lerp(seed, Color.White, 0.85f),
        tertiary = lerp(primary, Color(0xFFEFB8C8), 0.40f),
        onTertiary = lerp(seed, Color.Black, 0.55f),
        tertiaryContainer = lerp(seed, Color.Black, 0.50f),
        onTertiaryContainer = lerp(seed, Color.White, 0.85f),
        surfaceTint = primary,
    )
}

@Composable
fun FlowerNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: String = Accents.DYNAMIC,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        accent == Accents.DYNAMIC && Accents.dynamicAvailable() -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            val seed = Accents.seedFor(accent)
            if (darkTheme) darkSchemeFrom(seed) else lightSchemeFrom(seed)
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
