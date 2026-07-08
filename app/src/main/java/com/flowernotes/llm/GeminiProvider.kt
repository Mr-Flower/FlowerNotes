package com.flowernotes.llm

import com.flowernotes.data.EventoData
import com.flowernotes.i18n.I18n
import org.json.JSONArray
import org.json.JSONObject

class GeminiProvider(
    private val apiKey: String,
    private val model: String = GeminiModels.DEFAULT,
    private val durataDefault: Int = 60,
    private val reminderDefault: Int = 60,
) : LlmProvider {

    override suspend fun estraiEventi(testo: String): List<EventoData> {
        val body = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", ExtractionPrompt.build(testo, durataDefault, reminderDefault))
                ))
            ))
            // Chiediamo direttamente output JSON
            put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
        }

        val response = HttpClient.postJson(
            url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent",
            body = body.toString(),
            headers = mapOf("x-goog-api-key" to apiKey),
        )

        val text = try {
            JSONObject(response)
                .getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts").getJSONObject(0)
                .getString("text")
        } catch (e: Exception) {
            throw LlmException(I18n.strings.llmUnexpectedFormat, e)
        }
        return ExtractionPrompt.parseResponse(text)
    }
}
