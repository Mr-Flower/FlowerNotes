package com.flowernotes.data

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * Dati di un evento estratti dal testo (vocale o manuale).
 * data in formato ISO yyyy-MM-dd, ora in formato HH:mm.
 * ricorrenza: RRULE (RFC 5545, senza prefisso "RRULE:"), vuota se evento singolo.
 */
data class EventoData(
    val titolo: String = "",
    val data: String = "",
    val ora: String = "09:00",
    val durataMinuti: Int = 60,
    val reminderMinuti: Int = 60,
    val luogo: String = "",
    val ricorrenza: String = "",
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("titolo", titolo)
        put("data", data)
        put("ora", ora)
        put("durata_minuti", durataMinuti)
        put("reminder_minuti", reminderMinuti)
        put("luogo", luogo)
        put("ricorrenza", ricorrenza)
    }

    companion object {
        fun fromJson(o: JSONObject): EventoData = EventoData(
            titolo = o.optString("titolo", ""),
            data = o.optString("data", ""),
            ora = o.optString("ora", "09:00").ifBlank { "09:00" },
            durataMinuti = o.optInt("durata_minuti", 60),
            reminderMinuti = o.optInt("reminder_minuti", 60),
            luogo = o.optStringOrEmpty("luogo"),
            ricorrenza = o.optStringOrEmpty("ricorrenza"),
        )

        fun fromJson(arr: JSONArray): List<EventoData> =
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }

        /** Codifica una lista di eventi per la navigazione */
        fun listToNavArg(eventi: List<EventoData>): String {
            val arr = JSONArray()
            eventi.forEach { arr.put(it.toJson()) }
            return Uri.encode(arr.toString())
        }

        /** Decodifica la lista passata come argomento di navigazione */
        fun listFromJson(json: String): List<EventoData> = fromJson(JSONArray(json))

        /** optString che tratta null JSON come stringa vuota */
        private fun JSONObject.optStringOrEmpty(key: String): String =
            if (isNull(key)) "" else optString(key, "")
    }
}
