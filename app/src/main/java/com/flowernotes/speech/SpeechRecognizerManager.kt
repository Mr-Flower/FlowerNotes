package com.flowernotes.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Wrapper del SpeechRecognizer nativo di Android.
 *
 * L'istanza viene riusata tra un ascolto e l'altro: distruggere e ricreare il
 * recognizer a ogni avvio causava ERROR_SERVER_DISCONNECTED (codice 11) su
 * alcuni dispositivi. In più, se il servizio si disconnette comunque, viene
 * ricreato e riavviato una volta in automatico prima di mostrare l'errore.
 */
class SpeechRecognizerManager(
    private val context: Context,
    private val onPartial: (String) -> Unit,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
) {
    private var recognizer: SpeechRecognizer? = null
    private var retriedAfterDisconnect = false

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start() {
        retriedAfterDisconnect = false
        startInternal()
    }

    private fun startInternal() {
        if (!isAvailable()) {
            onError("Riconoscimento vocale non disponibile su questo dispositivo")
            return
        }
        val r = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
            it.setRecognitionListener(listener)
            recognizer = it
        }
        r.startListening(buildIntent())
    }

    /** Interrompe l'ascolto in corso senza distruggere il recognizer */
    fun cancel() {
        recognizer?.cancel()
    }

    /** Da chiamare solo quando il ViewModel viene distrutto */
    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.let(onPartial)
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            if (text.isNullOrBlank()) {
                onError("Non ho capito, riprova")
            } else {
                onResult(text)
            }
        }

        override fun onError(error: Int) {
            // Il servizio si è disconnesso: ricrea il recognizer e riprova una volta
            if (error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED && !retriedAfterDisconnect) {
                retriedAfterDisconnect = true
                recognizer?.destroy()
                recognizer = null
                startInternal()
                return
            }
            this@SpeechRecognizerManager.onError(errorMessage(error))
        }
    }

    private fun buildIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    private fun errorMessage(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Non ho sentito nulla, riprova"
        SpeechRecognizer.ERROR_AUDIO -> "Errore audio"
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Errore di rete del riconoscimento vocale"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permesso microfono mancante"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Riconoscimento occupato, riprova"
        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Troppe richieste, attendi un momento"
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED ->
            "Il servizio di riconoscimento si è disconnesso, riprova"
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
            "Italiano non disponibile nel riconoscimento vocale del dispositivo"
        else -> "Errore del riconoscimento vocale (codice $code)"
    }
}
