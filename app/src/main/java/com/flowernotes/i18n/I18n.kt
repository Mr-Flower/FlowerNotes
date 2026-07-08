package com.flowernotes.i18n

import java.util.Locale

/** Lingue selezionabili nelle impostazioni */
enum class AppLanguage(val id: String) {
    SYSTEM("system"),
    ITALIAN("it"),
    ENGLISH("en"),
    FRENCH("fr"),
    GERMAN("de"),
    SPANISH("es");

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
    var language: AppLanguage = AppLanguage.SYSTEM

    /** SYSTEM risolto sulla lingua del dispositivo (fallback inglese) */
    private fun resolve(language: AppLanguage): AppLanguage =
        if (language == AppLanguage.SYSTEM) {
            AppLanguage.entries.firstOrNull { it.id == Locale.getDefault().language }
                ?: AppLanguage.ENGLISH
        } else {
            language
        }

    private val effective: AppLanguage
        get() = resolve(language)

    val strings: Strings
        get() = stringsFor(effective)

    val locale: Locale
        get() = when (effective) {
            AppLanguage.ITALIAN -> Locale.ITALIAN
            AppLanguage.FRENCH -> Locale.FRENCH
            AppLanguage.GERMAN -> Locale.GERMAN
            AppLanguage.SPANISH -> Locale("es")
            else -> Locale.ENGLISH
        }

    /** Tag lingua per lo SpeechRecognizer */
    val speechLanguageTag: String
        get() = when (language) {
            AppLanguage.SYSTEM -> Locale.getDefault().toLanguageTag()
            AppLanguage.ITALIAN -> "it-IT"
            AppLanguage.ENGLISH -> "en-US"
            AppLanguage.FRENCH -> "fr-FR"
            AppLanguage.GERMAN -> "de-DE"
            AppLanguage.SPANISH -> "es-ES"
        }

    /** Mappa lingua → oggetto stringhe (usata anche dalla UI Compose) */
    fun stringsFor(language: AppLanguage): Strings = when (resolve(language)) {
        AppLanguage.ITALIAN -> ItalianStrings
        AppLanguage.FRENCH -> FrenchStrings
        AppLanguage.GERMAN -> GermanStrings
        AppLanguage.SPANISH -> SpanishStrings
        else -> EnglishStrings
    }
}
