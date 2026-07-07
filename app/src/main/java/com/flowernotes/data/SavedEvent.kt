package com.flowernotes.data

import org.json.JSONObject

/** Evento creato dall'app e salvato nella lista locale */
data class SavedEvent(
    val id: Long,            // id dell'evento nel Calendar Provider
    val titolo: String,
    val data: String,        // yyyy-MM-dd
    val ora: String,         // HH:mm
    val luogo: String,
    val createdAtMillis: Long,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("titolo", titolo)
        put("data", data)
        put("ora", ora)
        put("luogo", luogo)
        put("createdAtMillis", createdAtMillis)
    }

    companion object {
        fun fromJson(o: JSONObject): SavedEvent = SavedEvent(
            id = o.getLong("id"),
            titolo = o.optString("titolo", ""),
            data = o.optString("data", ""),
            ora = o.optString("ora", ""),
            luogo = o.optString("luogo", ""),
            createdAtMillis = o.optLong("createdAtMillis", 0L),
        )
    }
}
