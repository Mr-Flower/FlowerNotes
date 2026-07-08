package com.flowernotes.llm

import com.flowernotes.data.EventoData
import com.flowernotes.data.SettingsRepository
import com.flowernotes.i18n.I18n
import kotlinx.coroutines.flow.first

/**
 * Estrazione testo → lista di EventoData, condivisa tra flusso vocale e manuale.
 * Converte ogni errore in una LlmException con messaggio localizzato.
 */
class ExtractEventUseCase(private val settingsRepository: SettingsRepository) {

    suspend operator fun invoke(text: String): Result<List<EventoData>> = try {
        val settings = settingsRepository.settings.first()
        val provider = LlmProviderFactory.create(settings)
        Result.success(provider.estraiEventi(text))
    } catch (e: LlmException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(LlmException("${I18n.strings.networkProviderError}: ${e.message}", e))
    }
}
