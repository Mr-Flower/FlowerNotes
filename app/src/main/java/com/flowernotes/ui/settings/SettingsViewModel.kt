package com.flowernotes.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowernotes.data.SettingsRepository
import com.flowernotes.data.ThemeMode
import com.flowernotes.i18n.AppLanguage
import com.flowernotes.i18n.I18n
import com.flowernotes.llm.GeminiModels
import com.flowernotes.ui.theme.Accents
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * I campi di testo usano stato locale e salvataggio esplicito: il binding
 * diretto col DataStore (asincrono) causava conflitti tra il testo digitato
 * e quello ri-emesso dal Flow (cursore che salta, cancellazioni che
 * "tornano indietro").
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    var apiKeyInput by mutableStateOf("")
    var savedKeyExists by mutableStateOf(false)
        private set
    var model by mutableStateOf(GeminiModels.DEFAULT)
        private set
    var accent by mutableStateOf(Accents.DYNAMIC)
        private set
    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set
    var language by mutableStateOf(AppLanguage.ENGLISH)
        private set
    var durationInput by mutableStateOf("60")
    var reminderInput by mutableStateOf("60")
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
            accent = s.accent
            themeMode = s.themeMode
            language = s.language
            durationInput = s.defaultDurationMinutes.toString()
            reminderInput = s.defaultReminderMinutes.toString()
            loaded = true
        }
    }

    fun saveKey() {
        val key = apiKeyInput.trim()
        viewModelScope.launch {
            settingsRepository.setGeminiKey(key)
            apiKeyInput = key
            savedKeyExists = key.isNotBlank()
            feedback = if (key.isBlank()) {
                I18n.strings.keyRemovedFeedback
            } else {
                I18n.strings.keySavedFeedback
            }
        }
    }

    fun clearKey() {
        apiKeyInput = ""
        viewModelScope.launch {
            settingsRepository.setGeminiKey("")
            savedKeyExists = false
            feedback = I18n.strings.keyRemovedFeedback
        }
    }

    fun selectModel(newModel: String) {
        model = newModel
        viewModelScope.launch { settingsRepository.setGeminiModel(newModel) }
    }

    fun selectAccent(newAccent: String) {
        accent = newAccent
        viewModelScope.launch { settingsRepository.setAccent(newAccent) }
    }

    fun selectThemeMode(mode: ThemeMode) {
        themeMode = mode
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun selectLanguage(newLanguage: AppLanguage) {
        language = newLanguage
        viewModelScope.launch { settingsRepository.setLanguage(newLanguage) }
    }

    fun onDurationChange(value: String) {
        durationInput = value
        value.trim().toIntOrNull()?.let { minutes ->
            if (minutes > 0) {
                viewModelScope.launch { settingsRepository.setDefaultDuration(minutes) }
            }
        }
    }

    fun onReminderChange(value: String) {
        reminderInput = value
        value.trim().toIntOrNull()?.let { minutes ->
            if (minutes >= 0) {
                viewModelScope.launch { settingsRepository.setDefaultReminder(minutes) }
            }
        }
    }

    fun consumeFeedback(): String? = feedback.also { feedback = null }
}
