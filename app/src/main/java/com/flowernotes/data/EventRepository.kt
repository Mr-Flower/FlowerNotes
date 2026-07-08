package com.flowernotes.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.eventsDataStore by preferencesDataStore(name = "events")

/**
 * Lista locale degli eventi creati dall'app.
 * Persistita come JSON in DataStore: per l'MVP evita di introdurre Room/KSP.
 */
class EventRepository(private val context: Context) {

    private val eventsKey = stringPreferencesKey("saved_events")

    val events: Flow<List<SavedEvent>> = context.eventsDataStore.data.map { prefs ->
        parse(prefs[eventsKey] ?: "[]")
    }

    suspend fun addAll(events: List<SavedEvent>) {
        if (events.isEmpty()) return
        context.eventsDataStore.edit { prefs ->
            val list = parse(prefs[eventsKey] ?: "[]") + events
            prefs[eventsKey] = serialize(list)
        }
    }

    /** Sostituisce l'evento con lo stesso id (usato dalla modifica),
     *  conservando la data di creazione originale per l'ordinamento */
    suspend fun update(event: SavedEvent) {
        context.eventsDataStore.edit { prefs ->
            val list = parse(prefs[eventsKey] ?: "[]").map {
                if (it.id == event.id) event.copy(createdAtMillis = it.createdAtMillis) else it
            }
            prefs[eventsKey] = serialize(list)
        }
    }

    suspend fun remove(id: Long) {
        context.eventsDataStore.edit { prefs ->
            val list = parse(prefs[eventsKey] ?: "[]").filterNot { it.id == id }
            prefs[eventsKey] = serialize(list)
        }
    }

    private fun parse(json: String): List<SavedEvent> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { SavedEvent.fromJson(arr.getJSONObject(it)) }
    } catch (e: Exception) {
        emptyList()
    }

    private fun serialize(list: List<SavedEvent>): String {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        return arr.toString()
    }
}
