package com.flowernotes.llm

import com.flowernotes.data.EventoData
import com.flowernotes.i18n.AppLanguage
import com.flowernotes.i18n.I18n
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Prompt di estrazione (nella lingua dell'app) e parsing della risposta JSON */
object ExtractionPrompt {

    fun build(testo: String, durataDefault: Int = 60, reminderDefault: Int = 60): String {
        val oggi = LocalDate.now()
        val locale = I18n.locale
        val giorno = oggi.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        // Le chiavi JSON restano le stesse in entrambe le lingue
        return if (locale.language == Locale.ITALIAN.language) {
            """
            Sei un assistente che estrae i dati di un evento di calendario da testo in linguaggio naturale.
            Oggi è $giorno $oggi .
            Analizza il testo e rispondi SOLO con JSON puro, senza markdown, senza spiegazioni, in questo formato esatto:
            {"titolo": "...", "data": "YYYY-MM-DD", "ora": "HH:MM", "durata_minuti": $durataDefault, "reminder_minuti": $reminderDefault, "luogo": ""}
            Regole:
            - Risolvi le date relative ("domani", "venerdì prossimo") rispetto a oggi.
            - Il titolo deve essere breve e descrittivo (es. "Barbiere", "Dentista", "Cena con Marco").
            - Se l'ora non è specificata usa "09:00".
            - durata_minuti: $durataDefault se non specificata.
            - reminder_minuti: $reminderDefault se non specificato; se l'utente chiede un promemoria esplicito usa quello.
            - luogo: stringa vuota se non menzionato.
            Testo: "$testo"
            """.trimIndent()
        } else {
            """
            You are an assistant that extracts calendar event data from natural language text.
            Today is $giorno $oggi .
            Analyze the text and reply ONLY with pure JSON, no markdown, no explanations, in this exact format:
            {"titolo": "...", "data": "YYYY-MM-DD", "ora": "HH:MM", "durata_minuti": $durataDefault, "reminder_minuti": $reminderDefault, "luogo": ""}
            Rules:
            - Resolve relative dates ("tomorrow", "next Friday") against today.
            - The title must be short and descriptive (e.g. "Barber", "Dentist", "Dinner with Mark"), in the same language as the text.
            - If the time is not specified use "09:00".
            - durata_minuti: $durataDefault if not specified.
            - reminder_minuti: $reminderDefault if not specified; if the user asks for an explicit reminder use that.
            - luogo: empty string if not mentioned.
            Text: "$testo"
            """.trimIndent()
        }
    }

    /** Estrae l'oggetto JSON dalla risposta, tollerando eventuali code fence */
    fun parseResponse(raw: String): EventoData {
        val cleaned = raw.trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start < 0 || end <= start) {
            throw LlmException(I18n.strings.llmInvalidResponse)
        }
        return try {
            EventoData.fromJson(cleaned.substring(start, end + 1))
        } catch (e: Exception) {
            throw LlmException(I18n.strings.llmParseFailed, e)
        }
    }
}
