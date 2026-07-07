package com.flowernotes.llm

/** Modelli Gemini selezionabili nelle impostazioni (descrizioni in i18n/Strings) */
object GeminiModels {
    const val DEFAULT = "gemini-2.5-flash"

    val AVAILABLE = listOf(
        "gemini-3-flash-preview",
        "gemini-3-pro-preview",
        "gemini-2.5-flash",
        "gemini-2.5-flash-lite",
        "gemini-2.5-pro",
        "gemini-2.0-flash",
    )
}
