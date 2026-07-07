package com.flowernotes.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowernotes.data.SettingsRepository
import com.flowernotes.llm.GeminiModels
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Il campo della API key usa uno stato locale e un salvataggio esplicito:
 * il binding diretto col DataStore (asincrono) causava conflitti tra il testo
 * digitato e quello ri-emesso dal Flow (cursore che salta, cancellazioni che
 * "tornano indietro").
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    var apiKeyInput by mutableStateOf("")
    var savedKeyExists by mutableStateOf(false)
        private set
    var model by mutableStateOf(GeminiModels.DEFAULT)
        private set
    var loaded by mutableStateOf(false)
        private set
    var feedback by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            val s = settingsRepository.settings.first()
            apiKeyInput = s.geminiKey
            savedKeyExists = s.geminiKey.isNotBlank()
            model = s.geminiModel
            loaded = true
        }
    }

    fun saveKey() {
        val key = apiKeyInput.trim()
        viewModelScope.launch {
            settingsRepository.setGeminiKey(key)
            apiKeyInput = key
            savedKeyExists = key.isNotBlank()
            feedback = if (key.isBlank()) "Chiave rimossa" else "Chiave salvata"
        }
    }

    fun clearKey() {
        apiKeyInput = ""
        viewModelScope.launch {
            settingsRepository.setGeminiKey("")
            savedKeyExists = false
            feedback = "Chiave rimossa"
        }
    }

    fun selectModel(newModel: String) {
        model = newModel
        viewModelScope.launch { settingsRepository.setGeminiModel(newModel) }
    }

    fun consumeFeedback(): String? = feedback.also { feedback = null }
}
