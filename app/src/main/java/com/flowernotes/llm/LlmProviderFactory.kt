package com.flowernotes.llm

import com.flowernotes.data.LlmProviderType
import com.flowernotes.data.Settings

object LlmProviderFactory {

    /** Crea il provider dalle impostazioni; lancia LlmException se manca la key */
    fun create(settings: Settings): LlmProvider {
        val key = settings.activeKey()
        if (key.isBlank()) {
            throw LlmException("Nessuna API key configurata per ${settings.provider.displayName}: aggiungila nelle impostazioni")
        }
        return when (settings.provider) {
            LlmProviderType.GEMINI -> GeminiProvider(key)
            LlmProviderType.CLAUDE -> ClaudeProvider(key)
            LlmProviderType.OPENAI -> OpenAiProvider(key)
        }
    }
}
