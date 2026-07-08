package com.flowernotes.ui.onboarding

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowernotes.data.SettingsRepository
import com.flowernotes.i18n.I18n
import com.flowernotes.llm.LlmProviderType
import com.flowernotes.llm.LocalModelImporter
import com.flowernotes.llm.OllamaProvider
import kotlinx.coroutines.launch

/**
 * Configurazione guidata al primo avvio: permessi → scelta provider →
 * credenziali/modello. Tutto è facoltativo (si può saltare) e resta
 * modificabile dalle impostazioni.
 */
class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val modelImporter = LocalModelImporter(application)

    var step by mutableIntStateOf(0)
        private set

    var provider by mutableStateOf(LlmProviderType.GEMINI)
        private set

    var apiKeyInput by mutableStateOf("")
    var ollamaUrlInput by mutableStateOf("")
    var ollamaModelInput by mutableStateOf(OllamaProvider.DEFAULT_MODEL)

    var localModelReady by mutableStateOf(false)
        private set
    var importingModel by mutableStateOf(false)
        private set
    var importProgress by mutableStateOf(0f)
        private set
    var importError by mutableStateOf<String?>(null)
        private set

    fun next() {
        step++
    }

    fun back() {
        if (step > 0) step--
    }

    fun selectProvider(newProvider: LlmProviderType) {
        provider = newProvider
        viewModelScope.launch { settingsRepository.setLlmProvider(newProvider) }
    }

    fun importLocalModel(uri: Uri) {
        if (importingModel) return
        importingModel = true
        importProgress = 0f
        importError = null
        viewModelScope.launch {
            try {
                val path = modelImporter.import(uri) { progress -> importProgress = progress }
                settingsRepository.setLocalModelPath(path)
                localModelReady = true
            } catch (e: Exception) {
                importError = "${I18n.strings.localImportFailed}: ${e.message}"
            }
            importingModel = false
        }
    }

    /** Salva la configurazione del provider scelto e chiude l'onboarding */
    fun finish() {
        viewModelScope.launch {
            when (provider) {
                LlmProviderType.GEMINI -> {
                    val key = apiKeyInput.trim()
                    if (key.isNotBlank()) settingsRepository.setGeminiKey(key)
                }
                LlmProviderType.OLLAMA -> {
                    val url = ollamaUrlInput.trim()
                    if (url.isNotBlank()) {
                        settingsRepository.setOllamaUrl(url)
                        settingsRepository.setOllamaModel(
                            ollamaModelInput.trim().ifBlank { OllamaProvider.DEFAULT_MODEL }
                        )
                    }
                }
                LlmProviderType.LOCAL -> {
                    // Il percorso del modello è già stato salvato dall'import
                }
            }
            settingsRepository.setOnboardingDone(true)
        }
    }

    /** Chiude l'onboarding senza configurare nulla */
    fun skip() {
        viewModelScope.launch { settingsRepository.setOnboardingDone(true) }
    }
}
