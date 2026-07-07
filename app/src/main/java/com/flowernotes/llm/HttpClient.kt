package com.flowernotes.llm

import com.flowernotes.i18n.I18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Client HTTP minimale basato su HttpURLConnection:
 * evita di introdurre OkHttp/Retrofit per tre semplici chiamate POST JSON.
 */
object HttpClient {

    suspend fun postJson(
        url: String,
        body: String,
        headers: Map<String, String>,
    ): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 20_000
            conn.readTimeout = 60_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                throw LlmException(httpErrorMessage(code, err))
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun httpErrorMessage(code: Int, body: String): String = when (code) {
        401, 403 -> "${I18n.strings.httpInvalidKey} (HTTP $code)"
        429 -> I18n.strings.httpRateLimit
        else -> "${I18n.strings.httpProviderError} (HTTP $code): ${body.take(200)}"
    }
}
