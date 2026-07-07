package com.flowernotes.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flowernotes.i18n.AppLanguage
import com.flowernotes.llm.GeminiModels
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
 * Impostazioni dell'app. Provider unico: Google Gemini (BYOK, la key la mette
 * l'utente e resta sul dispositivo). L'interfaccia LlmProvider resta astratta
 * per eventuali provider futuri.
 */
data class Settings(
    val geminiKey: String = "",
    val geminiModel: String = GeminiModels.DEFAULT,
    val accent: String = Accents.DYNAMIC,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    // Inglese di default, su scelta dell'utente
    val language: AppLanguage = AppLanguage.ENGLISH,
    val defaultDurationMinutes: Int = 60,
    val defaultReminderMinutes: Int = 60,
)

class SettingsRepository(private val context: Context) {

    private val geminiKeyPref = stringPreferencesKey("gemini_api_key")
    private val geminiModelPref = stringPreferencesKey("gemini_model")
    private val accentPref = stringPreferencesKey("accent_color")
    private val themeModePref = stringPreferencesKey("theme_mode")
    private val languagePref = stringPreferencesKey("app_language")
    private val defaultDurationPref = intPreferencesKey("default_duration_minutes")
    private val defaultReminderPref = intPreferencesKey("default_reminder_minutes")

    val settings: Flow<Settings> = context.settingsDataStore.data.map { prefs ->
        Settings(
            geminiKey = prefs[geminiKeyPref] ?: "",
            geminiModel = prefs[geminiModelPref] ?: GeminiModels.DEFAULT,
            accent = prefs[accentPref]
                ?: if (Accents.dynamicAvailable()) Accents.DYNAMIC else Accents.OPTIONS.first().id,
            themeMode = ThemeMode.fromId(prefs[themeModePref]),
            language = prefs[languagePref]?.let { AppLanguage.fromId(it) } ?: AppLanguage.ENGLISH,
            defaultDurationMinutes = prefs[defaultDurationPref] ?: 60,
            defaultReminderMinutes = prefs[defaultReminderPref] ?: 60,
        )
    }

    suspend fun setGeminiKey(key: String) {
        context.settingsDataStore.edit { it[geminiKeyPref] = key }
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
