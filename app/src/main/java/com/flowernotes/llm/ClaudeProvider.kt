package com.flowernotes.llm

import com.flowernotes.data.EventoData
import org.json.JSONArray
import org.json.JSONObject

class ClaudeProvider(private val apiKey: String) : LlmProvider {

    override suspend fun estraiEvento(testo: String): EventoData {
        val body = JSONObject().apply {
            put("model", MODEL)
            put("max_tokens", 512)
            put("messages", JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", ExtractionPrompt.build(testo))
                }
            ))
        }

        val response = HttpClient.postJson(
            url = "https://api.anthropic.com/v1/messages",
            body = body.toString(),
            headers = mapOf(
                "x-api-key" to apiKey,
                "anthropic-version" to "2023-06-01",
            ),
        )

        val text = try {
            JSONObject(response)
                .getJSONArray("content").getJSONObject(0)
                .getString("text")
        } catch (e: Exception) {
            throw LlmException("Risposta Claude in formato inatteso", e)
        }
        return ExtractionPrompt.parseResponse(text)
    }

    companion object {
        const val MODEL = "claude-haiku-4-5-20251001"
    }
}
