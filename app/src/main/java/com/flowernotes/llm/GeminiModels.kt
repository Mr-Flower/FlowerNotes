package com.flowernotes.llm

/** Modelli Gemini selezionabili nelle impostazioni */
object GeminiModels {
    const val DEFAULT = "gemini-2.5-flash"

    /** id modello → descrizione mostrata all'utente */
    val AVAILABLE = listOf(
        "gemini-2.5-flash" to "Veloce ed economico (consigliato)",
        "gemini-2.5-flash-lite" to "Il più veloce ed economico",
        "gemini-2.5-pro" to "Il più capace, più lento",
        "gemini-2.0-flash" to "Generazione precedente",
    )
}
