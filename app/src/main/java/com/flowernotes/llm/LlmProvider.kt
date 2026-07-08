package com.flowernotes.llm

import com.flowernotes.data.EventoData

/**
 * Interfaccia comune per i provider LLM (architettura BYOK).
 * Ogni implementazione gestisce endpoint/auth/formato propri,
 * ma l'output converge sempre su EventoData.
 */
interface LlmProvider {
    /** Un testo può descrivere più eventi: la lista non è mai vuota */
    suspend fun estraiEventi(testo: String): List<EventoData>
}

/** Errore leggibile da mostrare all'utente quando la chiamata LLM fallisce */
open class LlmException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Provider selezionabile nelle impostazioni */
enum class LlmProviderType(val id: String) {
    GEMINI("gemini"),
    OLLAMA("ollama");

    companion object {
        fun fromId(id: String?): LlmProviderType = entries.firstOrNull { it.id == id } ?: GEMINI
    }
}
