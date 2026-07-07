package com.flowernotes.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flowernotes.llm.GeminiModels
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * Impostazioni dell'app. Provider unico: Google Gemini (BYOK, la key la mette
 * l'utente e resta sul dispositivo). L'interfaccia LlmProvider resta astratta
 * per eventuali provider futuri.
 */
data class Settings(
    val geminiKey: String = "",
    val geminiModel: String = GeminiModels.DEFAULT,
)

class SettingsRepository(private val context: Context) {

    private val geminiKeyPref = stringPreferencesKey("gemini_api_key")
    private val geminiModelPref = stringPreferencesKey("gemini_model")

    val settings: Flow<Settings> = context.settingsDataStore.data.map { prefs ->
        Settings(
            geminiKey = prefs[geminiKeyPref] ?: "",
            geminiModel = prefs[geminiModelPref] ?: GeminiModels.DEFAULT,
        )
    }

    suspend fun setGeminiKey(key: String) {
        context.settingsDataStore.edit { it[geminiKeyPref] = key }
    }

    suspend fun setGeminiModel(model: String) {
        context.settingsDataStore.edit { it[geminiModelPref] = model }
    }
}
