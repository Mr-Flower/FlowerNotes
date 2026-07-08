package com.flowernotes.llm

import com.flowernotes.i18n.I18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Client HTTP minimale basato su HttpURLConnection:
 * evita di introdurre OkHttp/Retrofit per tre semplici chiamate POST JSON.
 *
 * Gli errori transitori (HTTP 429/5xx, tipico il 503 "traffico intenso" di
 * Gemini, e gli errori di rete) vengono ritentati con backoff prima di
 * arrendersi e mostrare l'errore all'utente.
 */
object HttpClient {

    private const val MAX_ATTEMPTS = 3
    private const val RETRY_DELAY_MS = 1_500L

    /** Errore transitorio: vale la pena riprovare */
    private class TransientHttpException(message: String) : LlmException(message)

    suspend fun postJson(
        url: String,
        body: String,
        headers: Map<String, String>,
    ): String = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            if (attempt > 0) delay(RETRY_DELAY_MS * attempt)
            try {
                return@withContext doPost(url, body, headers)
            } catch (e: TransientHttpException) {
                lastError = e
            } catch (e: IOException) {
                // Timeout o connessione caduta: riprova
                lastError = e
            }
        }
        // Le IOException vengono avvolte in un messaggio leggibile da ExtractEventUseCase
        throw lastError ?: IllegalStateException("unreachable")
    }

    private fun doPost(
        url: String,
        body: String,
        headers: Map<String, String>,
    ): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 20_000
            conn.readTimeout = 120_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            if (code in 200..299) {
                return conn.inputStream.bufferedReader().use { it.readText() }
            }
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            val message = httpErrorMessage(code, err)
            if (code == 429 || code in 500..599) {
                throw TransientHttpException(message)
            }
            throw LlmException(message)
        } finally {
            conn.disconnect()
        }
    }

    private fun httpErrorMessage(code: Int, body: String): String = when (code) {
        401, 403 -> "${I18n.strings.httpInvalidKey} (HTTP $code)"
        429 -> I18n.strings.httpRateLimit
        503 -> I18n.strings.httpOverloaded
        else -> "${I18n.strings.httpProviderError} (HTTP $code): ${body.take(200)}"
    }
}
