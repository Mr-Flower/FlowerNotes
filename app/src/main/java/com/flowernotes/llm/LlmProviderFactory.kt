package com.flowernotes.llm

import com.flowernotes.data.Settings
import com.flowernotes.i18n.I18n

object LlmProviderFactory {

    /** Crea il provider dalle impostazioni; lancia LlmException se la configurazione è incompleta */
    fun create(settings: Settings): LlmProvider = when (settings.llmProvider) {
        LlmProviderType.GEMINI -> {
            if (settings.geminiKey.isBlank()) {
                throw LlmException(I18n.strings.llmNoKey)
            }
            GeminiProvider(
                apiKey = settings.geminiKey,
                model = settings.geminiModel,
                durataDefault = settings.defaultDurationMinutes,
                reminderDefault = settings.defaultReminderMinutes,
            )
        }
        LlmProviderType.OLLAMA -> {
            if (settings.ollamaUrl.isBlank()) {
                throw LlmException(I18n.strings.llmNoOllamaUrl)
            }
            OllamaProvider(
                baseUrl = settings.ollamaUrl,
                model = settings.ollamaModel.ifBlank { OllamaProvider.DEFAULT_MODEL },
                durataDefault = settings.defaultDurationMinutes,
                reminderDefault = settings.defaultReminderMinutes,
            )
        }
    }
}
