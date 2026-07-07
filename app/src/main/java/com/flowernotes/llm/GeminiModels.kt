package com.flowernotes.llm

/** Modelli Gemini selezionabili nelle impostazioni */
object GeminiModels {
    const val DEFAULT = "gemini-2.5-flash"

    /** id modello → descrizione mostrata all'utente */
    val AVAILABLE = listOf(
        "gemini-3-flash-preview" to "Nuova generazione, veloce (preview)",
        "gemini-3-pro-preview" to "Nuova generazione, il più capace (preview)",
        "gemini-2.5-flash" to "Veloce ed economico (consigliato)",
        "gemini-2.5-flash-lite" to "Il più veloce ed economico",
        "gemini-2.5-pro" to "Il più capace tra gli stabili",
        "gemini-2.0-flash" to "Generazione precedente",
    )
}
