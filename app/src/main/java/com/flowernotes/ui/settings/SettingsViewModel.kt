package com.flowernotes.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowernotes.calendar.CalendarWriter
import com.flowernotes.data.Settings
import com.flowernotes.data.SettingsRepository
import com.flowernotes.data.ThemeMode
import com.flowernotes.i18n.AppLanguage
import com.flowernotes.i18n.I18n
import com.flowernotes.llm.ExtractEventUseCase
import com.flowernotes.llm.GeminiModels
import com.flowernotes.llm.LlmProviderType
import com.flowernotes.llm.OllamaProvider
import com.flowernotes.ui.theme.Accents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * I campi di testo usano stato locale e salvataggio esplicito: il binding
 * diretto col DataStore (asincrono) causava conflitti tra il testo digitato
 * e quello ri-emesso dal Flow (cursore che salta, cancellazioni che
 * "tornano indietro").
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val calendarWriter = CalendarWriter(application)
    private val extractEvent = ExtractEventUseCase(settingsRepository)

    var provider by mutableStateOf(LlmProviderType.GEMINI)
        private set
    var apiKeyInput by mutableStateOf("")
    var savedKeyExists by mutableStateOf(false)
        private set
    var model by mutableStateOf(GeminiModels.DEFAULT)
        private set
    var ollamaUrlInput by mutableStateOf("")
    var ollamaModelInput by mutableStateOf(OllamaProvider.DEFAULT_MODEL)
    var savedOllamaUrlExists by mutableStateOf(false)
        private set
    var accent by mutableStateOf(Accents.DYNAMIC)
        private set
    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set
    var language by mutableStateOf(AppLanguage.SYSTEM)
        private set
    var durationInput by mutableStateOf("60")
    var reminderInput by mutableStateOf("60")
    var loaded by mutableStateOf(false)
        private set
    var feedback by mutableStateOf<String?>(null)
        private set

    // Prova connessione al provider LLM
    var testing by mutableStateOf(false)
        private set
    var testResult by mutableStateOf<Pair<Boolean, String>?>(null) // ok? + messaggio
        private set

    // Selettore del calendario di destinazione
    var calendars by mutableStateOf<List<CalendarWriter.CalendarInfo>>(emptyList())
        private set
    var calendarId by mutableStateOf(Settings.CALENDAR_AUTO)
        private set

    init {
        viewModelScope.launch {
            val s = settingsRepository.settings.first()
            provider = s.llmProvider
            apiKeyInput = s.geminiKey
            savedKeyExists = s.geminiKey.isNotBlank()
            model = s.geminiModel
            ollamaUrlInput = s.ollamaUrl
            ollamaModelInput = s.ollamaModel
            savedOllamaUrlExists = s.ollamaUrl.isNotBlank()
            accent = s.accent
            themeMode = s.themeMode
            language = s.language
            durationInput = s.defaultDurationMinutes.toString()
            reminderInput = s.defaultReminderMinutes.toString()
            calendarId = s.calendarId
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

    fun selectProvider(newProvider: LlmProviderType) {
        provider = newProvider
        viewModelScope.launch { settingsRepository.setLlmProvider(newProvider) }
    }

    fun saveOllama() {
        val url = ollamaUrlInput.trim()
        val ollamaModel = ollamaModelInput.trim().ifBlank { OllamaProvider.DEFAULT_MODEL }
        viewModelScope.launch {
            settingsRepository.setOllamaUrl(url)
            settingsRepository.setOllamaModel(ollamaModel)
            ollamaUrlInput = url
            ollamaModelInput = ollamaModel
            savedOllamaUrlExists = url.isNotBlank()
            feedback = I18n.strings.ollamaSavedFeedback
        }
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

    /**
     * Prova end-to-end del provider configurato (key/URL salvati): manda un
     * testo di esempio attraverso la stessa pipeline usata da Home e Manuale.
     */
    fun testConnection() {
        if (testing) return
        testing = true
        testResult = null
        viewModelScope.launch {
            val startMillis = System.currentTimeMillis()
            val sample = if (I18n.locale.language == Locale.ITALIAN.language) {
                "riunione di prova domani alle 10"
            } else {
                "test meeting tomorrow at 10"
            }
            testResult = extractEvent(sample).fold(
                onSuccess = {
                    val seconds = (System.currentTimeMillis() - startMillis) / 1000.0
                    true to "${I18n.strings.testConnectionOk} (%.1f s)".format(seconds)
                },
                onFailure = { e -> false to e.message.orEmpty() },
            )
            testing = false
        }
    }

    /** Carica i calendari scrivibili (richiede il permesso READ_CALENDAR) */
    fun loadCalendars() {
        viewModelScope.launch {
            calendars = withContext(Dispatchers.IO) { calendarWriter.listWritableCalendars() }
        }
    }

    fun selectCalendar(id: Long) {
        calendarId = id
        viewModelScope.launch { settingsRepository.setCalendarId(id) }
    }

    fun consumeFeedback(): String? = feedback.also { feedback = null }
}
