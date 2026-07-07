package com.flowernotes.llm

import com.flowernotes.data.Settings

object LlmProviderFactory {

    /** Crea il provider dalle impostazioni; lancia LlmException se manca la key */
    fun create(settings: Settings): LlmProvider {
        if (settings.geminiKey.isBlank()) {
            throw LlmException("Nessuna API key Gemini configurata: aggiungila nelle impostazioni")
        }
        return GeminiProvider(
            apiKey = settings.geminiKey,
            model = settings.geminiModel,
            durataDefault = settings.defaultDurationMinutes,
            reminderDefault = settings.defaultReminderMinutes,
        )
    }
}
