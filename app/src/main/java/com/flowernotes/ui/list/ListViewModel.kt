package com.flowernotes.ui.list

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowernotes.calendar.CalendarWriter
import com.flowernotes.data.EventRepository
import com.flowernotes.data.SavedEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListViewModel(application: Application) : AndroidViewModel(application) {

    private val eventRepository = EventRepository(application)
    private val calendarWriter = CalendarWriter(application)

    val events: StateFlow<List<SavedEvent>> = eventRepository.events
        .map { list -> list.sortedByDescending { it.createdAtMillis } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Rimuove l'evento sia dal calendario che dalla lista locale */
    fun delete(event: SavedEvent) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    calendarWriter.delete(event.id)
                } catch (e: Exception) {
                    // best effort: l'evento potrebbe essere già stato rimosso dal calendario
                }
            }
            eventRepository.remove(event.id)
        }
    }

    /** Intent per aprire l'evento nell'app calendario */
    fun viewIntent(event: SavedEvent): Intent = calendarWriter.viewIntent(event.id)
}
