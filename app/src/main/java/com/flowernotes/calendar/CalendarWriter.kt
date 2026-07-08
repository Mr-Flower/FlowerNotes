package com.flowernotes.calendar

import android.accounts.Account
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import com.flowernotes.data.EventoData
import com.flowernotes.i18n.I18n
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

    /** Calendario di destinazione: id + account per richiedere la sync */
    private data class CalendarTarget(val id: Long, val account: Account?)

    /** Inserisce l'evento e il relativo promemoria; ritorna l'id dell'evento */
    fun insert(evento: EventoData): Long {
        val target = findWritableCalendar()
            ?: throw CalendarException(I18n.strings.calendarNoneWritable)
        val calendarId = target.id

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
            ?: throw CalendarException(I18n.strings.calendarInsertFailed)
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

        requestSync(target.account)
        return eventId
    }

    /** Elimina un evento creato in precedenza (best effort) */
    fun delete(eventId: Long) {
        // L'account va letto prima della cancellazione, dopo la riga non c'è più
        val account = accountOfEvent(eventId)
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        context.contentResolver.delete(uri, null, null)
        requestSync(account)
    }

    /**
     * Chiede ad Android di sincronizzare subito il calendario dell'account,
     * invece di aspettare il giro naturale del sync adapter (che può
     * impiegare minuti). Best effort: l'ultima parola resta al sistema.
     */
    private fun requestSync(account: Account?) {
        if (account == null) return
        val extras = Bundle().apply {
            putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
            putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
        }
        ContentResolver.requestSync(account, CalendarContract.AUTHORITY, extras)
    }

    /** Account a cui appartiene un evento (la vista Events espone le colonne del calendario) */
    private fun accountOfEvent(eventId: Long): Account? {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val projection = arrayOf(
            CalendarContract.Events.ACCOUNT_NAME,
            CalendarContract.Events.ACCOUNT_TYPE,
        )
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToNext()) {
                val name = cursor.getString(0)
                val type = cursor.getString(1)
                if (!name.isNullOrBlank() && !type.isNullOrBlank()) {
                    return Account(name, type)
                }
            }
        }
        return null
    }

    /** Intent per aprire l'evento nell'app calendario */
    fun viewIntent(eventId: Long): Intent =
        Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId))

    /**
     * Sceglie il calendario di destinazione: preferisce il primario dell'account
     * Google, poi un qualsiasi calendario con permessi di scrittura.
     */
    private fun findWritableCalendar(): CalendarTarget? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.ACCOUNT_NAME,
        )
        data class Candidate(val target: CalendarTarget, val primary: Boolean, val google: Boolean)
        val candidates = mutableListOf<Candidate>()
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI, projection, null, null, null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val access = cursor.getInt(1)
                val primary = cursor.getInt(2) == 1
                val accountType = cursor.getString(3)
                val accountName = cursor.getString(4)
                val account = if (!accountName.isNullOrBlank() && !accountType.isNullOrBlank()) {
                    Account(accountName, accountType)
                } else null
                if (access >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) {
                    candidates.add(Candidate(CalendarTarget(id, account), primary, accountType == "com.google"))
                }
            }
        }
        return (candidates.firstOrNull { it.primary && it.google }
            ?: candidates.firstOrNull { it.google }
            ?: candidates.firstOrNull { it.primary }
            ?: candidates.firstOrNull())?.target
    }
}
