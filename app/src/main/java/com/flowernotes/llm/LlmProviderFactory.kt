package com.flowernotes.llm

import com.flowernotes.data.Settings
import com.flowernotes.i18n.I18n

object LlmProviderFactory {

    /** Crea il provider dalle impostazioni; lancia LlmException se manca la key */
    fun create(settings: Settings): LlmProvider {
        if (settings.geminiKey.isBlank()) {
            throw LlmException(I18n.strings.llmNoKey)
        }
        return GeminiProvider(
            apiKey = settings.geminiKey,
            model = settings.geminiModel,
            durataDefault = settings.defaultDurationMinutes,
            reminderDefault = settings.defaultReminderMinutes,
        )
    }
}
