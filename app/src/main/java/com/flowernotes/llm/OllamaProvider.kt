package com.flowernotes.llm

import com.flowernotes.data.EventoData
import com.flowernotes.i18n.I18n
import org.json.JSONArray
import org.json.JSONObject

/**
 * Provider per un server Ollama self-hosted (es. container sulla rete locale).
 * Usa l'endpoint nativo /api/chat senza streaming e con output JSON forzato,
 * riusando lo stesso prompt di estrazione di Gemini.
 */
class OllamaProvider(
    baseUrl: String,
    private val model: String = DEFAULT_MODEL,
    private val durataDefault: Int = 60,
    private val reminderDefault: Int = 60,
) : LlmProvider {

    companion object {
        const val DEFAULT_MODEL = "llama3.2"
    }

    // Tollera indirizzi senza schema ("192.168.1.10:11434") e slash finali
    private val normalizedUrl = baseUrl.trim().trimEnd('/').let {
        if (it.startsWith("http://") || it.startsWith("https://")) it else "http://$it"
    }

    override suspend fun estraiEvento(testo: String): EventoData {
        val body = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().put(
                JSONObject()
                    .put("role", "user")
                    .put("content", ExtractionPrompt.build(testo, durataDefault, reminderDefault))
            ))
            put("stream", false)
            // Equivalente del responseMimeType JSON di Gemini
            put("format", "json")
            put("options", JSONObject().put("temperature", 0))
        }

        val response = HttpClient.postJson(
            url = "$normalizedUrl/api/chat",
            body = body.toString(),
            headers = emptyMap(),
        )

        val text = try {
            JSONObject(response).getJSONObject("message").getString("content")
        } catch (e: Exception) {
            throw LlmException(I18n.strings.ollamaUnexpectedFormat, e)
        }
        return ExtractionPrompt.parseResponse(text)
    }
}
