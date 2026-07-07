package com.flowernotes.i18n

import java.util.Locale

/** Lingue selezionabili nelle impostazioni */
enum class AppLanguage(val id: String) {
    SYSTEM("system"),
    ITALIAN("it"),
    ENGLISH("en");

    companion object {
        fun fromId(id: String?): AppLanguage = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

/**
 * Punto di accesso alla lingua corrente per il codice NON Compose
 * (riconoscimento vocale, provider LLM, messaggi di errore).
 * La UI Compose usa invece LocalStrings; MainActivity tiene i due allineati.
 */
object I18n {
    @Volatile
    var language: AppLanguage = AppLanguage.ENGLISH

    private val isItalian: Boolean
        get() = when (language) {
            AppLanguage.ITALIAN -> true
            AppLanguage.ENGLISH -> false
            AppLanguage.SYSTEM -> Locale.getDefault().language == "it"
        }

    val strings: Strings
        get() = if (isItalian) ItalianStrings else EnglishStrings

    val locale: Locale
        get() = if (isItalian) Locale.ITALIAN else Locale.ENGLISH

    /** Tag lingua per lo SpeechRecognizer */
    val speechLanguageTag: String
        get() = when (language) {
            AppLanguage.ITALIAN -> "it-IT"
            AppLanguage.ENGLISH -> "en-US"
            AppLanguage.SYSTEM -> Locale.getDefault().toLanguageTag()
        }
}
