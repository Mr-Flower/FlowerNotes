package com.flowernotes.llm

import com.flowernotes.data.EventoData
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Prompt condiviso da tutti i provider e parsing della risposta JSON */
object ExtractionPrompt {

    fun build(testo: String, durataDefault: Int = 60, reminderDefault: Int = 60): String {
        val oggi = LocalDate.now()
        val giorno = oggi.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ITALIAN)
        return """
            Sei un assistente che estrae i dati di un evento di calendario da testo in linguaggio naturale in italiano.
            Oggi è $giorno ${oggi} .
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
    }

    /** Estrae l'oggetto JSON dalla risposta, tollerando eventuali code fence */
    fun parseResponse(raw: String): EventoData {
        val cleaned = raw.trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start < 0 || end <= start) {
            throw LlmException("Risposta del modello non valida: JSON non trovato")
        }
        return try {
            EventoData.fromJson(cleaned.substring(start, end + 1))
        } catch (e: Exception) {
            throw LlmException("Impossibile interpretare la risposta del modello", e)
        }
    }
}
