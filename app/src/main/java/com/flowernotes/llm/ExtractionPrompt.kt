package com.flowernotes.llm

import com.flowernotes.data.EventoData
import com.flowernotes.i18n.I18n
import org.json.JSONObject
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
            Sei un assistente che estrae i dati di uno o più eventi di calendario da testo in linguaggio naturale.
            Oggi è $giorno $oggi .
            Analizza il testo e rispondi SOLO con JSON puro, senza markdown, senza spiegazioni, in questo formato esatto:
            {"eventi": [{"titolo": "...", "data": "YYYY-MM-DD", "ora": "HH:MM", "durata_minuti": $durataDefault, "reminder_minuti": $reminderDefault, "luogo": "", "ricorrenza": ""}]}
            Regole:
            - Il testo può contenere PIÙ eventi (es. "dentista giovedì e cena sabato"): un oggetto per ciascuno, nell'ordine in cui compaiono.
            - Risolvi le date relative ("domani", "venerdì prossimo") rispetto a oggi.
            - Il titolo deve essere breve e descrittivo (es. "Barbiere", "Dentista", "Cena con Marco").
            - Se l'ora non è specificata usa "09:00".
            - durata_minuti: $durataDefault se non specificata.
            - reminder_minuti: $reminderDefault se non specificato; se l'utente chiede un promemoria esplicito usa quello.
            - luogo: stringa vuota se non menzionato.
            - ricorrenza: RRULE RFC 5545 senza prefisso "RRULE:" SOLO se l'evento si ripete ("ogni lunedì" → "FREQ=WEEKLY;BYDAY=MO", "tutti i giorni" → "FREQ=DAILY", "ogni mese" → "FREQ=MONTHLY"); stringa vuota se evento singolo. Per gli eventi ricorrenti, data = prima occorrenza.
            Testo: "$testo"
            """.trimIndent()
        } else {
            """
            You are an assistant that extracts the data of one or more calendar events from natural language text.
            Today is $giorno $oggi .
            Analyze the text and reply ONLY with pure JSON, no markdown, no explanations, in this exact format:
            {"eventi": [{"titolo": "...", "data": "YYYY-MM-DD", "ora": "HH:MM", "durata_minuti": $durataDefault, "reminder_minuti": $reminderDefault, "luogo": "", "ricorrenza": ""}]}
            Rules:
            - The text may contain MULTIPLE events (e.g. "dentist on Thursday and dinner on Saturday"): one object per event, in the order they appear.
            - Resolve relative dates ("tomorrow", "next Friday") against today.
            - The title must be short and descriptive (e.g. "Barber", "Dentist", "Dinner with Mark"), in the same language as the text.
            - If the time is not specified use "09:00".
            - durata_minuti: $durataDefault if not specified.
            - reminder_minuti: $reminderDefault if not specified; if the user asks for an explicit reminder use that.
            - luogo: empty string if not mentioned.
            - ricorrenza: RFC 5545 RRULE without the "RRULE:" prefix ONLY if the event repeats ("every Monday" → "FREQ=WEEKLY;BYDAY=MO", "every day" → "FREQ=DAILY", "every month" → "FREQ=MONTHLY"); empty string for one-off events. For recurring events, data = first occurrence.
            Text: "$testo"
            """.trimIndent()
        }
    }

    /**
     * Estrae la lista di eventi dalla risposta, tollerando code fence e,
     * per compatibilità, anche il vecchio formato a oggetto singolo.
     */
    fun parseResponse(raw: String): List<EventoData> {
        val cleaned = raw.trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start < 0 || end <= start) {
            throw LlmException(I18n.strings.llmInvalidResponse)
        }
        val list = try {
            val o = JSONObject(cleaned.substring(start, end + 1))
            o.optJSONArray("eventi")?.let { EventoData.fromJson(it) }
                ?: listOf(EventoData.fromJson(o))
        } catch (e: Exception) {
            throw LlmException(I18n.strings.llmParseFailed, e)
        }
        if (list.isEmpty()) throw LlmException(I18n.strings.llmParseFailed)
        return list
    }
}
