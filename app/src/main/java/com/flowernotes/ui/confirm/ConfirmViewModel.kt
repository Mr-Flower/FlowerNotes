package com.flowernotes.ui.confirm

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowernotes.calendar.CalendarWriter
import com.flowernotes.data.EventRepository
import com.flowernotes.data.EventoData
import com.flowernotes.data.SavedEvent
import com.flowernotes.i18n.I18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

sealed interface ConfirmUiState {
    data object Editing : ConfirmUiState
    data object Saving : ConfirmUiState
    data object Saved : ConfirmUiState
    data class Error(val message: String) : ConfirmUiState
}

class ConfirmViewModel(application: Application) : AndroidViewModel(application) {

    private val calendarWriter = CalendarWriter(application)
    private val eventRepository = EventRepository(application)

    var titolo by mutableStateOf("")
    var data by mutableStateOf("")
    var ora by mutableStateOf("")
    var durataMinuti by mutableStateOf("60")
    var reminderMinuti by mutableStateOf("60")
    var luogo by mutableStateOf("")

    var uiState by mutableStateOf<ConfirmUiState>(ConfirmUiState.Editing)
        private set

    private var loaded = false

    /** Precompila i campi con i dati estratti (solo alla prima composizione) */
    fun load(evento: EventoData) {
        if (loaded) return
        loaded = true
        titolo = evento.titolo
        data = evento.data
        ora = evento.ora
        durataMinuti = evento.durataMinuti.toString()
        reminderMinuti = evento.reminderMinuti.toString()
        luogo = evento.luogo
    }

    /** Valida i campi; ritorna un messaggio di errore o null se tutto ok */
    fun validate(): String? {
        val strings = I18n.strings
        if (titolo.isBlank()) return strings.validationTitleRequired
        try {
            LocalDate.parse(data.trim())
        } catch (e: DateTimeParseException) {
            return strings.validationInvalidDate
        }
        try {
            LocalTime.parse(ora.trim())
        } catch (e: DateTimeParseException) {
            return strings.validationInvalidTime
        }
        if (durataMinuti.trim().toIntOrNull() == null) return strings.validationInvalidDuration
        if (reminderMinuti.trim().toIntOrNull() == null) return strings.validationInvalidReminder
        return null
    }

    /** Da chiamare quando i permessi calendario sono già concessi */
    fun save() {
        val error = validate()
        if (error != null) {
            uiState = ConfirmUiState.Error(error)
            return
        }
        uiState = ConfirmUiState.Saving
        val evento = EventoData(
            titolo = titolo.trim(),
            data = data.trim(),
            ora = ora.trim(),
            durataMinuti = durataMinuti.trim().toInt(),
            reminderMinuti = reminderMinuti.trim().toInt(),
            luogo = luogo.trim(),
        )
        viewModelScope.launch {
            try {
                val eventId = withContext(Dispatchers.IO) { calendarWriter.insert(evento) }
                eventRepository.add(
                    SavedEvent(
                        id = eventId,
                        titolo = evento.titolo,
                        data = evento.data,
                        ora = evento.ora,
                        luogo = evento.luogo,
                        createdAtMillis = System.currentTimeMillis(),
                    )
                )
                uiState = ConfirmUiState.Saved
            } catch (e: Exception) {
                uiState = ConfirmUiState.Error(e.message ?: I18n.strings.saveGenericError)
            }
        }
    }

    fun dismissError() {
        if (uiState is ConfirmUiState.Error) {
            uiState = ConfirmUiState.Editing
        }
    }
}
