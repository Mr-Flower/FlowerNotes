package com.flowernotes.llm

import com.flowernotes.data.EventoData
import org.json.JSONArray
import org.json.JSONObject

class GeminiProvider(private val apiKey: String) : LlmProvider {

    override suspend fun estraiEvento(testo: String): EventoData {
        val body = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", ExtractionPrompt.build(testo))
                ))
            ))
            // Chiediamo direttamente output JSON
            put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
        }

        val response = HttpClient.postJson(
            url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent",
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
            throw LlmException("Risposta Gemini in formato inatteso", e)
        }
        return ExtractionPrompt.parseResponse(text)
    }

    companion object {
        const val MODEL = "gemini-2.0-flash"
    }
}
