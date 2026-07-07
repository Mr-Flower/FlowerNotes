package com.flowernotes.ui.manual

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowernotes.data.EventoData
import com.flowernotes.data.SettingsRepository
import com.flowernotes.llm.LlmException
import com.flowernotes.llm.LlmProviderFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface ManualUiState {
    data object Idle : ManualUiState
    data object Processing : ManualUiState
    data class Extracted(val evento: EventoData) : ManualUiState
    data class Error(val message: String) : ManualUiState
}

class ManualViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    private val _uiState = MutableStateFlow<ManualUiState>(ManualUiState.Idle)
    val uiState: StateFlow<ManualUiState> = _uiState

    fun extract(text: String) {
        if (text.isBlank()) {
            _uiState.value = ManualUiState.Error("Scrivi prima il testo dell'evento")
            return
        }
        _uiState.value = ManualUiState.Processing
        viewModelScope.launch {
            try {
                val settings = settingsRepository.settings.first()
                val provider = LlmProviderFactory.create(settings)
                val evento = provider.estraiEvento(text)
                _uiState.value = ManualUiState.Extracted(evento)
            } catch (e: LlmException) {
                _uiState.value = ManualUiState.Error(e.message ?: "Errore sconosciuto")
            } catch (e: Exception) {
                _uiState.value = ManualUiState.Error("Errore di rete o del provider: ${e.message}")
            }
        }
    }

    fun reset() {
        _uiState.value = ManualUiState.Idle
    }
}
