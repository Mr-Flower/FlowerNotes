package com.flowernotes.ui.confirm

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowernotes.calendar.CalendarWriter
import com.flowernotes.data.EventRepository
import com.flowernotes.data.EventoData
import com.flowernotes.data.SavedEvent
import com.flowernotes.data.SettingsRepository
import com.flowernotes.i18n.I18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

sealed interface ConfirmUiState {
    data object Loading : ConfirmUiState
    data object Editing : ConfirmUiState
    data object Saving : ConfirmUiState
    data object Saved : ConfirmUiState
    data class Error(val message: String) : ConfirmUiState
}

/** Stato editabile di un singolo evento (una card) */
class EventFields(evento: EventoData) {
    var titolo by mutableStateOf(evento.titolo)
    var data by mutableStateOf(evento.data)
    var ora by mutableStateOf(evento.ora)
    var durataMinuti by mutableStateOf(evento.durataMinuti.toString())
    var reminderMinuti by mutableStateOf(evento.reminderMinuti.toString())
    var luogo by mutableStateOf(evento.luogo)
    var ricorrenza by mutableStateOf(evento.ricorrenza)

    fun toEvento() = EventoData(
        titolo = titolo.trim(),
        data = data.trim(),
        ora = ora.trim(),
        durataMinuti = durataMinuti.trim().toInt(),
        reminderMinuti = reminderMinuti.trim().toInt(),
        luogo = luogo.trim(),
        ricorrenza = ricorrenza.trim(),
    )
}

/**
 * Gestisce una lista di eventi da confermare (il testo può descriverne più
 * d'uno) oppure, in modalità modifica, un singolo evento letto dal calendario.
 */
class ConfirmViewModel(application: Application) : AndroidViewModel(application) {

    private val calendarWriter = CalendarWriter(application)
    private val eventRepository = EventRepository(application)
    private val settingsRepository = SettingsRepository(application)

    val eventi = mutableStateListOf<EventFields>()

    var uiState by mutableStateOf<ConfirmUiState>(ConfirmUiState.Loading)
        private set

    /** id dell'evento in modifica; null in modalità creazione */
    private var editEventId: Long? = null
    private var loaded = false

    val isEditMode: Boolean get() = editEventId != null

    /** Precompila le card con gli eventi estratti (solo alla prima composizione) */
    fun loadNew(nuovi: List<EventoData>) {
        if (loaded) return
        loaded = true
        eventi.addAll(nuovi.map { EventFields(it) })
        uiState = ConfirmUiState.Editing
    }

    /** Carica dal Calendar Provider l'evento da modificare */
    fun loadEdit(eventId: Long) {
        if (loaded) return
        loaded = true
        editEventId = eventId
        viewModelScope.launch {
            val evento = try {
                withContext(Dispatchers.IO) { calendarWriter.read(eventId) }
            } catch (e: Exception) {
                // es. permesso calendario revocato: mostra il motivo reale
                uiState = ConfirmUiState.Error(e.message ?: I18n.strings.calendarEventNotFound)
                return@launch
            }
            if (evento == null) {
                uiState = ConfirmUiState.Error(I18n.strings.calendarEventNotFound)
            } else {
                eventi.add(EventFields(evento))
                uiState = ConfirmUiState.Editing
            }
        }
    }

    /** Scarta una delle card (solo in creazione, con più eventi) */
    fun removeAt(index: Int) {
        if (eventi.size > 1 && index in eventi.indices) {
            eventi.removeAt(index)
        }
    }

    /** Valida tutte le card; ritorna un messaggio di errore o null se tutto ok */
    fun validate(): String? {
        val strings = I18n.strings
        eventi.forEachIndexed { index, fields ->
            val prefix = if (eventi.size > 1) "${strings.eventLabel} ${index + 1}: " else ""
            if (fields.titolo.isBlank()) return prefix + strings.validationTitleRequired
            try {
                LocalDate.parse(fields.data.trim())
            } catch (e: DateTimeParseException) {
                return prefix + strings.validationInvalidDate
            }
            try {
                LocalTime.parse(fields.ora.trim())
            } catch (e: DateTimeParseException) {
                return prefix + strings.validationInvalidTime
            }
            val durata = fields.durataMinuti.trim().toIntOrNull()
            if (durata == null || durata <= 0) return prefix + strings.validationInvalidDuration
            if (fields.reminderMinuti.trim().toIntOrNull() == null) {
                return prefix + strings.validationInvalidReminder
            }
        }
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
        viewModelScope.launch {
            try {
                val editId = editEventId
                if (editId != null) {
                    saveEdit(editId, eventi.first().toEvento())
                } else {
                    saveNew(eventi.map { it.toEvento() })
                }
                uiState = ConfirmUiState.Saved
            } catch (e: Exception) {
                uiState = ConfirmUiState.Error(e.message ?: I18n.strings.saveGenericError)
            }
        }
    }

    private suspend fun saveNew(nuovi: List<EventoData>) {
        val calendarId = settingsRepository.settings.first().calendarId
        val ids = withContext(Dispatchers.IO) { calendarWriter.insertAll(nuovi, calendarId) }
        eventRepository.addAll(ids.zip(nuovi) { id, evento -> toSavedEvent(id, evento) })
    }

    private suspend fun saveEdit(eventId: Long, evento: EventoData) {
        withContext(Dispatchers.IO) { calendarWriter.update(eventId, evento) }
        eventRepository.update(toSavedEvent(eventId, evento))
    }

    private fun toSavedEvent(eventId: Long, evento: EventoData) = SavedEvent(
        id = eventId,
        titolo = evento.titolo,
        data = evento.data,
        ora = evento.ora,
        luogo = evento.luogo,
        createdAtMillis = System.currentTimeMillis(),
        ricorrenza = evento.ricorrenza,
    )

    fun dismissError() {
        if (uiState is ConfirmUiState.Error) {
            uiState = ConfirmUiState.Editing
        }
    }
}
