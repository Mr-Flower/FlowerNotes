package com.flowernotes.llm

import com.flowernotes.data.EventoData
import org.json.JSONArray
import org.json.JSONObject

class OpenAiProvider(private val apiKey: String) : LlmProvider {

    override suspend fun estraiEvento(testo: String): EventoData {
        val body = JSONObject().apply {
            put("model", MODEL)
            put("messages", JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", ExtractionPrompt.build(testo))
                }
            ))
            // Forza output JSON
            put("response_format", JSONObject().put("type", "json_object"))
        }

        val response = HttpClient.postJson(
            url = "https://api.openai.com/v1/chat/completions",
            body = body.toString(),
            headers = mapOf("Authorization" to "Bearer $apiKey"),
        )

        val text = try {
            JSONObject(response)
                .getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } catch (e: Exception) {
            throw LlmException("Risposta OpenAI in formato inatteso", e)
        }
        return ExtractionPrompt.parseResponse(text)
    }

    companion object {
        const val MODEL = "gpt-4o-mini"
    }
}
