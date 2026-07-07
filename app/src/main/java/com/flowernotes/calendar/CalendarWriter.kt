package com.flowernotes.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.flowernotes.data.EventoData
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Scrive gli eventi tramite il Calendar Provider di sistema (CalendarContract).
 * Gli eventi inseriti nel calendario dell'account Google vengono sincronizzati
 * automaticamente con Google Calendar, senza bisogno di OAuth lato app.
 * (In futuro potrà essere affiancato da un'implementazione Google Calendar API v3.)
 */
class CalendarWriter(private val context: Context) {

    class CalendarException(message: String) : Exception(message)

    /** Inserisce l'evento e il relativo promemoria; ritorna l'id dell'evento */
    fun insert(evento: EventoData): Long {
        val calendarId = findWritableCalendarId()
            ?: throw CalendarException("Nessun calendario scrivibile trovato sul dispositivo")

        val zone = ZoneId.systemDefault()
        val startDateTime = LocalDateTime.of(
            LocalDate.parse(evento.data),
            LocalTime.parse(evento.ora),
        )
        val startMillis = startDateTime.atZone(zone).toInstant().toEpochMilli()
        val endMillis = startDateTime.plusMinutes(evento.durataMinuti.toLong())
            .atZone(zone).toInstant().toEpochMilli()

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, evento.titolo)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, zone.id)
            if (evento.luogo.isNotBlank()) {
                put(CalendarContract.Events.EVENT_LOCATION, evento.luogo)
            }
            put(CalendarContract.Events.HAS_ALARM, 1)
        }

        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            ?: throw CalendarException("Inserimento evento fallito")
        val eventId = ContentUris.parseId(uri)

        // Promemoria
        if (evento.reminderMinuti > 0) {
            val reminderValues = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.MINUTES, evento.reminderMinuti)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
        }

        return eventId
    }

    /** Elimina un evento creato in precedenza (best effort) */
    fun delete(eventId: Long) {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        context.contentResolver.delete(uri, null, null)
    }

    /** Intent per aprire l'evento nell'app calendario */
    fun viewIntent(eventId: Long): Intent =
        Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId))

    /**
     * Sceglie il calendario di destinazione: preferisce il primario dell'account
     * Google, poi un qualsiasi calendario con permessi di scrittura.
     */
    private fun findWritableCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.ACCOUNT_TYPE,
        )
        val candidates = mutableListOf<Triple<Long, Boolean, Boolean>>() // id, primario, google
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI, projection, null, null, null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val access = cursor.getInt(1)
                val primary = cursor.getInt(2) == 1
                val isGoogle = cursor.getString(3) == "com.google"
                if (access >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) {
                    candidates.add(Triple(id, primary, isGoogle))
                }
            }
        }
        return (candidates.firstOrNull { it.second && it.third }
            ?: candidates.firstOrNull { it.third }
            ?: candidates.firstOrNull { it.second }
            ?: candidates.firstOrNull())?.first
    }
}
