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
import com.flowernotes.ui.onboarding.OnboardingScreen
import com.flowernotes.ui.theme.FlowerNotesTheme

class MainActivity : ComponentActivity() {

    // Incrementato ogni volta che il Quick Tile chiede di partire in ascolto
    private var listenTrigger by mutableIntStateOf(0)

    // Testo ricevuto via ACTION_SEND (condivisione da altre app)
    private var sharedTextTrigger by mutableIntStateOf(0)
    private var sharedText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        val settingsRepository = SettingsRepository(applicationContext)
        setContent {
            // initialValue null: finché DataStore non ha emesso non decidiamo
            // nulla (evita il flash dell'onboarding per chi lo ha già fatto)
            val settings by settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = null as Settings?)
            val loaded = settings ?: return@setContent
            val darkTheme = when (loaded.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            // Allinea il singleton I18n (usato dal codice non-Compose) e le
            // stringhe fornite alla UI con la lingua scelta nelle impostazioni
            val strings = remember(loaded.language) {
                I18n.language = loaded.language
                I18n.strings
            }
            CompositionLocalProvider(LocalStrings provides strings) {
                FlowerNotesTheme(darkTheme = darkTheme, accent = loaded.accent) {
                    if (!loaded.onboardingDone) {
                        OnboardingScreen()
                    } else {
                        FlowerNotesApp(
                            startListenTrigger = listenTrigger,
                            sharedTextTrigger = sharedTextTrigger,
                            sharedText = sharedText,
                        )
                    }
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
        // Testo condiviso da un'altra app: apre l'inserimento manuale precompilato
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            if (text.isNotBlank()) {
                sharedText = text
                sharedTextTrigger++
            }
        }
    }

    companion object {
        const val EXTRA_START_LISTENING = "com.flowernotes.START_LISTENING"
    }
}
