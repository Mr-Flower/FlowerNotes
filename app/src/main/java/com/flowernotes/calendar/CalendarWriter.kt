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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Scrive gli eventi tramite il Calendar Provider di sistema (CalendarContract).
 * Gli eventi inseriti nel calendario dell'account Google vengono sincronizzati
 * automaticamente con Google Calendar, senza bisogno di OAuth lato app;
 * dopo ogni modifica viene richiesta una sync immediata (requestSync).
 */
class CalendarWriter(private val context: Context) {

    class CalendarException(message: String) : Exception(message)

    /**
     * Calendario scrivibile: id, nome e account (per il selettore nelle
     * impostazioni e per richiedere la sync dopo le scritture).
     */
    data class CalendarInfo(
        val id: Long,
        val name: String,
        val account: String,
        internal val syncAccount: Account? = null,
        internal val primary: Boolean = false,
        internal val google: Boolean = false,
    )

    /**
     * Inserisce ogni evento con il relativo promemoria e ritorna gli id,
     * nello stesso ordine. Il calendario viene risolto una volta sola e la
     * sync viene richiesta una volta sola alla fine.
     * preferredCalendarId: calendario scelto nelle impostazioni,
     * Settings.CALENDAR_AUTO per la selezione automatica
     * (primario Google → Google → primario → primo scrivibile).
     */
    fun insertAll(eventi: List<EventoData>, preferredCalendarId: Long): List<Long> {
        val target = findWritableCalendar(preferredCalendarId)
            ?: throw CalendarException(I18n.strings.calendarNoneWritable)

        val ids = eventi.map { evento ->
            val values = eventValues(evento).apply {
                put(CalendarContract.Events.CALENDAR_ID, target.id)
            }
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                ?: throw CalendarException(I18n.strings.calendarInsertFailed)
            val eventId = ContentUris.parseId(uri)
            insertReminder(eventId, evento.reminderMinuti)
            eventId
        }
        requestSync(target.syncAccount)
        return ids
    }

    /** Aggiorna un evento esistente (stessi campi dell'insert, calendario invariato) */
    fun update(eventId: Long, evento: EventoData) {
        val account = accountOfEvent(eventId)
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val updated = context.contentResolver.update(uri, eventValues(evento), null, null)
        if (updated == 0) {
            throw CalendarException(I18n.strings.calendarEventNotFound)
        }
        // Promemoria: si riparte puliti e si reinserisce quello corrente
        context.contentResolver.delete(
            CalendarContract.Reminders.CONTENT_URI,
            "${CalendarContract.Reminders.EVENT_ID} = ?",
            arrayOf(eventId.toString()),
        )
        insertReminder(eventId, evento.reminderMinuti)
        requestSync(account)
    }

    /** Legge un evento dal provider (per la modifica); null se non esiste più */
    fun read(eventId: Long): EventoData? {
        val zone = ZoneId.systemDefault()
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.DURATION,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.RRULE,
            CalendarContract.Events.DELETED,
        )
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToNext() || cursor.getInt(6) == 1) return null
            val startMillis = cursor.getLong(1)
            val start = LocalDateTime.ofInstant(Instant.ofEpochMilli(startMillis), zone)
            val durataMinuti = when {
                !cursor.isNull(2) && cursor.getLong(2) > 0 ->
                    ((cursor.getLong(2) - startMillis) / 60_000L).toInt()
                else -> parseRfc2445Duration(cursor.getString(3))
            }
            return EventoData(
                titolo = cursor.getString(0).orEmpty(),
                data = start.toLocalDate().toString(),
                ora = "%02d:%02d".format(start.hour, start.minute),
                durataMinuti = durataMinuti.coerceAtLeast(1),
                reminderMinuti = readReminderMinutes(eventId),
                luogo = cursor.getString(4).orEmpty(),
                ricorrenza = cursor.getString(5).orEmpty(),
            )
        }
        return null
    }

    /** Elimina un evento creato in precedenza (best effort) */
    fun delete(eventId: Long) {
        // L'account va letto prima della cancellazione, dopo la riga non c'è più
        val account = accountOfEvent(eventId)
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        context.contentResolver.delete(uri, null, null)
        requestSync(account)
    }

    /** Intent per aprire l'evento nell'app calendario */
    fun viewIntent(eventId: Long): Intent =
        Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId))

    /**
     * Calendari scrivibili (permessi >= CONTRIBUTOR), un'unica query
     * condivisa tra selettore delle impostazioni e scelta di destinazione.
     */
    fun listWritableCalendars(): List<CalendarInfo> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.IS_PRIMARY,
        )
        val result = mutableListOf<CalendarInfo>()
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?",
            arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val accountName = cursor.getString(2)
                val accountType = cursor.getString(3)
                result.add(
                    CalendarInfo(
                        id = cursor.getLong(0),
                        name = cursor.getString(1).orEmpty(),
                        account = accountName.orEmpty(),
                        syncAccount = if (!accountName.isNullOrBlank() && !accountType.isNullOrBlank()) {
                            Account(accountName, accountType)
                        } else null,
                        primary = cursor.getInt(4) == 1,
                        google = accountType == "com.google",
                    )
                )
            }
        }
        return result
    }

    /** Valori comuni a insert e update; per gli eventi ricorrenti serve DURATION, non DTEND */
    private fun eventValues(evento: EventoData): ContentValues {
        val zone = ZoneId.systemDefault()
        val startDateTime = LocalDateTime.of(
            LocalDate.parse(evento.data),
            LocalTime.parse(evento.ora),
        )
        val startMillis = startDateTime.atZone(zone).toInstant().toEpochMilli()
        val ricorrente = evento.ricorrenza.isNotBlank()

        return ContentValues().apply {
            put(CalendarContract.Events.TITLE, evento.titolo)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, zone.id)
            put(CalendarContract.Events.EVENT_LOCATION, evento.luogo)
            put(CalendarContract.Events.HAS_ALARM, 1)
            if (ricorrente) {
                put(CalendarContract.Events.RRULE, evento.ricorrenza)
                put(CalendarContract.Events.DURATION, "PT${evento.durataMinuti}M")
                putNull(CalendarContract.Events.DTEND)
            } else {
                val endMillis = startDateTime.plusMinutes(evento.durataMinuti.toLong())
                    .atZone(zone).toInstant().toEpochMilli()
                put(CalendarContract.Events.DTEND, endMillis)
                putNull(CalendarContract.Events.RRULE)
                putNull(CalendarContract.Events.DURATION)
            }
        }
    }

    private fun insertReminder(eventId: Long, minuti: Int) {
        if (minuti <= 0) return
        val values = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.MINUTES, minuti)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }
        context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
    }

    private fun readReminderMinutes(eventId: Long): Int {
        context.contentResolver.query(
            CalendarContract.Reminders.CONTENT_URI,
            arrayOf(CalendarContract.Reminders.MINUTES),
            "${CalendarContract.Reminders.EVENT_ID} = ?",
            arrayOf(eventId.toString()),
            null,
        )?.use { cursor ->
            if (cursor.moveToNext()) return cursor.getInt(0)
        }
        return 0
    }

    /** "PT3600S"/"PT60M"/"P1D" (RFC 2445) → minuti; 60 se non interpretabile */
    private fun parseRfc2445Duration(duration: String?): Int {
        if (duration.isNullOrBlank()) return 60
        val match = Regex("^P(?:(\\d+)D)?(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?)?$")
            .find(duration.trim()) ?: return 60
        val (d, h, m, s) = match.destructured
        val minuti = (d.toIntOrNull() ?: 0) * 24 * 60 +
            (h.toIntOrNull() ?: 0) * 60 +
            (m.toIntOrNull() ?: 0) +
            (s.toIntOrNull() ?: 0) / 60
        return if (minuti > 0) minuti else 60
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

    /**
     * Sceglie il calendario di destinazione: quello preferito dall'utente se
     * ancora scrivibile, altrimenti selezione automatica (primario dell'account
     * Google, poi un qualsiasi calendario con permessi di scrittura).
     */
    private fun findWritableCalendar(preferredCalendarId: Long): CalendarInfo? {
        val candidates = listWritableCalendars()
        return candidates.firstOrNull { it.id == preferredCalendarId }
            ?: candidates.firstOrNull { it.primary && it.google }
            ?: candidates.firstOrNull { it.google }
            ?: candidates.firstOrNull { it.primary }
            ?: candidates.firstOrNull()
    }
}
