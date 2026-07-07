package com.flowernotes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowernotes.data.Settings
import com.flowernotes.data.SettingsRepository
import com.flowernotes.data.ThemeMode
import com.flowernotes.i18n.I18n
import com.flowernotes.i18n.LocalStrings
import com.flowernotes.ui.FlowerNotesApp
import com.flowernotes.ui.theme.FlowerNotesTheme

class MainActivity : ComponentActivity() {

    // Incrementato ogni volta che il Quick Tile chiede di partire in ascolto
    private var listenTrigger by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        val settingsRepository = SettingsRepository(applicationContext)
        setContent {
            val settings by settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = Settings())
            val darkTheme = when (settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            // Allinea il singleton I18n (usato dal codice non-Compose) e le
            // stringhe fornite alla UI con la lingua scelta nelle impostazioni
            val strings = remember(settings.language) {
                I18n.language = settings.language
                I18n.strings
            }
            CompositionLocalProvider(LocalStrings provides strings) {
                FlowerNotesTheme(darkTheme = darkTheme, accent = settings.accent) {
                    FlowerNotesApp(startListenTrigger = listenTrigger)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_START_LISTENING, false) == true) {
            intent.removeExtra(EXTRA_START_LISTENING)
            listenTrigger++
        }
    }

    companion object {
        const val EXTRA_START_LISTENING = "com.flowernotes.START_LISTENING"
    }
}
