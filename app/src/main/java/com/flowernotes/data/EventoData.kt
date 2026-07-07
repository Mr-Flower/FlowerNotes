package com.flowernotes.data

import android.net.Uri
import org.json.JSONObject

/**
 * Dati di un evento estratti dal testo (vocale o manuale).
 * data in formato ISO yyyy-MM-dd, ora in formato HH:mm.
 */
data class EventoData(
    val titolo: String = "",
    val data: String = "",
    val ora: String = "09:00",
    val durataMinuti: Int = 60,
    val reminderMinuti: Int = 60,
    val luogo: String = "",
) {
    fun toJson(): String = JSONObject().apply {
        put("titolo", titolo)
        put("data", data)
        put("ora", ora)
        put("durata_minuti", durataMinuti)
        put("reminder_minuti", reminderMinuti)
        put("luogo", luogo)
    }.toString()

    /** Codifica per passare l'oggetto come argomento di navigazione */
    fun toNavArg(): String = Uri.encode(toJson())

    companion object {
        fun fromJson(json: String): EventoData {
            val o = JSONObject(json)
            return EventoData(
                titolo = o.optString("titolo", ""),
                data = o.optString("data", ""),
                ora = o.optString("ora", "09:00").ifBlank { "09:00" },
                durataMinuti = o.optInt("durata_minuti", 60),
                reminderMinuti = o.optInt("reminder_minuti", 60),
                luogo = if (o.isNull("luogo")) "" else o.optString("luogo", ""),
            )
        }
    }
}
