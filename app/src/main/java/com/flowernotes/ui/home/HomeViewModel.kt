package com.flowernotes.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowernotes.data.EventoData
import com.flowernotes.data.SettingsRepository
import com.flowernotes.llm.ExtractEventUseCase
import com.flowernotes.speech.SpeechRecognizerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Stati del flusso vocale sulla Home */
sealed interface HomeUiState {
    data object Idle : HomeUiState
    data class Listening(val partialText: String) : HomeUiState
    data class Processing(val recognizedText: String) : HomeUiState
    data class Extracted(val evento: EventoData) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val extractEvent = ExtractEventUseCase(SettingsRepository(application))

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState

    private var errorAutoDismiss: Job? = null

    private val speech = SpeechRecognizerManager(
        context = application,
        onPartial = { partial -> _uiState.value = HomeUiState.Listening(partial) },
        onResult = { text -> extract(text) },
        onError = { message -> showError(message) },
    )

    /** Mostra l'errore e torna da solo a Idle dopo qualche secondo */
    private fun showError(message: String) {
        _uiState.value = HomeUiState.Error(message)
        errorAutoDismiss?.cancel()
        errorAutoDismiss = viewModelScope.launch {
            delay(8_000)
            if (_uiState.value is HomeUiState.Error) {
                _uiState.value = HomeUiState.Idle
            }
        }
    }

    fun startListening() {
        _uiState.value = HomeUiState.Listening("")
        speech.start()
    }

    fun stopListening() {
        speech.cancel()
        if (_uiState.value is HomeUiState.Listening) {
            _uiState.value = HomeUiState.Idle
        }
    }

    /** Chiamata LLM sul testo riconosciuto (usata anche per riprovare dopo un errore) */
    fun extract(text: String) {
        _uiState.value = HomeUiState.Processing(text)
        viewModelScope.launch {
            extractEvent(text).fold(
                onSuccess = { evento -> _uiState.value = HomeUiState.Extracted(evento) },
                onFailure = { e -> showError(e.message.orEmpty()) },
            )
        }
    }

    /** Da chiamare dopo che la UI ha consumato lo stato Extracted */
    fun reset() {
        _uiState.value = HomeUiState.Idle
    }

    override fun onCleared() {
        speech.destroy()
    }
}
