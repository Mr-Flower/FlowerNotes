package com.flowernotes.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Provider LLM supportati (architettura BYOK: la key la mette l'utente) */
enum class LlmProviderType(val displayName: String) {
    GEMINI("Google Gemini"),
    CLAUDE("Anthropic Claude"),
    OPENAI("OpenAI"),
}

data class Settings(
    val provider: LlmProviderType = LlmProviderType.GEMINI,
    val geminiKey: String = "",
    val claudeKey: String = "",
    val openaiKey: String = "",
) {
    /** Key attiva per il provider selezionato */
    fun activeKey(): String = when (provider) {
        LlmProviderType.GEMINI -> geminiKey
        LlmProviderType.CLAUDE -> claudeKey
        LlmProviderType.OPENAI -> openaiKey
    }
}

class SettingsRepository(private val context: Context) {

    private val providerKey = stringPreferencesKey("provider")
    private val geminiKey = stringPreferencesKey("gemini_api_key")
    private val claudeKey = stringPreferencesKey("claude_api_key")
    private val openaiKey = stringPreferencesKey("openai_api_key")

    val settings: Flow<Settings> = context.settingsDataStore.data.map { prefs ->
        Settings(
            provider = prefs[providerKey]?.let { name ->
                LlmProviderType.entries.firstOrNull { it.name == name }
            } ?: LlmProviderType.GEMINI,
            geminiKey = prefs[geminiKey] ?: "",
            claudeKey = prefs[claudeKey] ?: "",
            openaiKey = prefs[openaiKey] ?: "",
        )
    }

    suspend fun setProvider(provider: LlmProviderType) {
        context.settingsDataStore.edit { it[providerKey] = provider.name }
    }

    suspend fun setApiKey(provider: LlmProviderType, key: String) {
        context.settingsDataStore.edit {
            when (provider) {
                LlmProviderType.GEMINI -> it[geminiKey] = key
                LlmProviderType.CLAUDE -> it[claudeKey] = key
                LlmProviderType.OPENAI -> it[openaiKey] = key
            }
        }
    }
}
