package com.flowernotes.ui.manual

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowernotes.data.EventoData
import com.flowernotes.data.SettingsRepository
import com.flowernotes.i18n.I18n
import com.flowernotes.llm.ExtractEventUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ManualUiState {
    data object Idle : ManualUiState
    data object Processing : ManualUiState
    data class Extracted(val eventi: List<EventoData>) : ManualUiState
    data class Error(val message: String) : ManualUiState
}

class ManualViewModel(application: Application) : AndroidViewModel(application) {

    private val extractEvent = ExtractEventUseCase(SettingsRepository(application))

    private val _uiState = MutableStateFlow<ManualUiState>(ManualUiState.Idle)
    val uiState: StateFlow<ManualUiState> = _uiState

    fun extract(text: String) {
        if (text.isBlank()) {
            _uiState.value = ManualUiState.Error(I18n.strings.manualEmptyError)
            return
        }
        _uiState.value = ManualUiState.Processing
        viewModelScope.launch {
            extractEvent(text).fold(
                onSuccess = { eventi -> _uiState.value = ManualUiState.Extracted(eventi) },
                onFailure = { e -> _uiState.value = ManualUiState.Error(e.message.orEmpty()) },
            )
        }
    }

    fun reset() {
        _uiState.value = ManualUiState.Idle
    }
}
