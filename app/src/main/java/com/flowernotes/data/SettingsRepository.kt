package com.flowernotes.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flowernotes.i18n.AppLanguage
import com.flowernotes.llm.GeminiModels
import com.flowernotes.llm.LlmProviderType
import com.flowernotes.llm.OllamaProvider
import com.flowernotes.ui.theme.Accents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Modalità tema chiaro/scuro */
enum class ThemeMode(val id: String, val label: String) {
    SYSTEM("system", "Sistema"),
    LIGHT("light", "Chiaro"),
    DARK("dark", "Scuro");

    companion object {
        fun fromId(id: String?): ThemeMode = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

/**
 * Impostazioni dell'app. Due provider LLM: Google Gemini (BYOK, la key la
 * mette l'utente e resta sul dispositivo) oppure un server Ollama self-hosted
 * raggiungibile dal telefono (es. container sulla rete locale).
 */
data class Settings(
    val llmProvider: LlmProviderType = LlmProviderType.GEMINI,
    val geminiKey: String = "",
    val geminiModel: String = GeminiModels.DEFAULT,
    val ollamaUrl: String = "",
    val ollamaModel: String = OllamaProvider.DEFAULT_MODEL,
    // Calendario di destinazione; CALENDAR_AUTO = selezione automatica
    val calendarId: Long = Settings.CALENDAR_AUTO,
    val accent: String = Accents.DYNAMIC,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    // Segue la lingua di sistema, come il tema
    val language: AppLanguage = AppLanguage.SYSTEM,
    val defaultDurationMinutes: Int = 60,
    val defaultReminderMinutes: Int = 60,
) {
    companion object {
        /** Valore "automatico" per il calendario di destinazione */
        const val CALENDAR_AUTO = -1L
    }
}

class SettingsRepository(private val context: Context) {

    private val llmProviderPref = stringPreferencesKey("llm_provider")
    private val geminiKeyPref = stringPreferencesKey("gemini_api_key")
    private val geminiModelPref = stringPreferencesKey("gemini_model")
    private val ollamaUrlPref = stringPreferencesKey("ollama_url")
    private val ollamaModelPref = stringPreferencesKey("ollama_model")
    private val calendarIdPref = longPreferencesKey("calendar_id")
    private val accentPref = stringPreferencesKey("accent_color")
    private val themeModePref = stringPreferencesKey("theme_mode")
    private val languagePref = stringPreferencesKey("app_language")
    private val defaultDurationPref = intPreferencesKey("default_duration_minutes")
    private val defaultReminderPref = intPreferencesKey("default_reminder_minutes")

    val settings: Flow<Settings> = context.settingsDataStore.data.map { prefs ->
        Settings(
            llmProvider = LlmProviderType.fromId(prefs[llmProviderPref]),
            geminiKey = prefs[geminiKeyPref] ?: "",
            geminiModel = prefs[geminiModelPref] ?: GeminiModels.DEFAULT,
            ollamaUrl = prefs[ollamaUrlPref] ?: "",
            ollamaModel = prefs[ollamaModelPref] ?: OllamaProvider.DEFAULT_MODEL,
            calendarId = prefs[calendarIdPref] ?: Settings.CALENDAR_AUTO,
            accent = prefs[accentPref]
                ?: if (Accents.dynamicAvailable()) Accents.DYNAMIC else Accents.OPTIONS.first().id,
            themeMode = ThemeMode.fromId(prefs[themeModePref]),
            language = AppLanguage.fromId(prefs[languagePref]),
            defaultDurationMinutes = prefs[defaultDurationPref] ?: 60,
            defaultReminderMinutes = prefs[defaultReminderPref] ?: 60,
        )
    }

    suspend fun setLlmProvider(provider: LlmProviderType) {
        context.settingsDataStore.edit { it[llmProviderPref] = provider.id }
    }

    suspend fun setGeminiKey(key: String) {
        context.settingsDataStore.edit { it[geminiKeyPref] = key }
    }

    suspend fun setOllamaUrl(url: String) {
        context.settingsDataStore.edit { it[ollamaUrlPref] = url }
    }

    suspend fun setOllamaModel(model: String) {
        context.settingsDataStore.edit { it[ollamaModelPref] = model }
    }

    suspend fun setCalendarId(id: Long) {
        context.settingsDataStore.edit { it[calendarIdPref] = id }
    }

    suspend fun setGeminiModel(model: String) {
        context.settingsDataStore.edit { it[geminiModelPref] = model }
    }

    suspend fun setAccent(accent: String) {
        context.settingsDataStore.edit { it[accentPref] = accent }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[themeModePref] = mode.id }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { it[languagePref] = language.id }
    }

    suspend fun setDefaultDuration(minutes: Int) {
        context.settingsDataStore.edit { it[defaultDurationPref] = minutes }
    }

    suspend fun setDefaultReminder(minutes: Int) {
        context.settingsDataStore.edit { it[defaultReminderPref] = minutes }
    }
}
