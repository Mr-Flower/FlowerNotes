package com.flowernotes.llm

import android.content.Context
import com.flowernotes.data.EventoData
import com.flowernotes.i18n.I18n
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Provider on-device basato su MediaPipe LLM Inference: nessun dato lascia
 * il telefono. L'utente fornisce un modello .task (consigliato Gemma 3 1B
 * int4, ~550 MB) importato nelle impostazioni o durante l'onboarding.
 *
 * L'engine viene creato una sola volta per percorso modello e riusato:
 * l'inizializzazione richiede diversi secondi.
 */
class LocalProvider(
    private val context: Context,
    private val modelPath: String,
    private val durataDefault: Int = 60,
    private val reminderDefault: Int = 60,
) : LlmProvider {

    companion object {
        /** Nome del modello consigliato, mostrato nelle istruzioni */
        const val SUGGESTED_MODEL = "Gemma 3 1B (gemma3-1b-it-int4.task)"

        @Volatile
        private var cachedEngine: LlmInference? = null

        @Volatile
        private var cachedPath: String? = null

        private fun engineFor(context: Context, path: String): LlmInference {
            cachedEngine?.let { if (cachedPath == path) return it }
            synchronized(this) {
                cachedEngine?.let { if (cachedPath == path) return it }
                cachedEngine?.close()
                cachedEngine = null
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(path)
                    .setMaxTokens(1024)
                    .build()
                return LlmInference.createFromOptions(context.applicationContext, options)
                    .also {
                        cachedEngine = it
                        cachedPath = path
                    }
            }
        }
    }

    override suspend fun estraiEventi(testo: String): List<EventoData> {
        if (!File(modelPath).exists()) {
            throw LlmException(I18n.strings.llmNoLocalModel)
        }
        val prompt = ExtractionPrompt.build(testo, durataDefault, reminderDefault)
        // L'inferenza è CPU-bound e può durare decine di secondi sui telefoni lenti
        val response = withContext(Dispatchers.Default) {
            try {
                engineFor(context, modelPath).generateResponse(prompt)
            } catch (e: LlmException) {
                throw e
            } catch (e: Exception) {
                throw LlmException("${I18n.strings.localInferenceError}: ${e.message}", e)
            }
        }
        return ExtractionPrompt.parseResponse(response)
    }
}
