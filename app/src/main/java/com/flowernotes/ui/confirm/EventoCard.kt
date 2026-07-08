package com.flowernotes.ui.confirm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.flowernotes.i18n.LocalStrings
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Card riusabile con i campi di un evento, condivisa tra flusso vocale e
 * manuale (entrambi convergono sulla schermata di conferma, che può
 * mostrarne più di una). Data e ora si modificano con i picker Material 3
 * oppure a mano; la ripetizione con un menu a tendina.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventoCard(
    fields: EventFields,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null,
) {
    val strings = LocalStrings.current
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    if (showDatePicker) {
        val initialMillis = try {
            LocalDate.parse(fields.data.trim())
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } catch (e: Exception) {
            null
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // Il DatePicker lavora in UTC
                        fields.data = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text(strings.ok) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(strings.cancel) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val initialTime = try {
            LocalTime.parse(fields.ora.trim())
        } catch (e: Exception) {
            LocalTime.of(9, 0)
        }
        val timePickerState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = true,
        )
        // Material 3 non ha (ancora) un TimePickerDialog pronto: piccolo wrapper
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text(strings.cancel) }
                        TextButton(onClick = {
                            fields.ora = "%02d:%02d".format(
                                timePickerState.hour, timePickerState.minute,
                            )
                            showTimePicker = false
                        }) { Text(strings.ok) }
                    }
                }
            }
        }
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (onRemove != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(onClick = onRemove, enabled = enabled) {
                        Icon(Icons.Default.Close, contentDescription = strings.removeEventCd)
                    }
                }
            }
            OutlinedTextField(
                value = fields.titolo,
                onValueChange = { fields.titolo = it },
                label = { Text(strings.fieldTitle) },
                singleLine = true,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            // Data e ora a piena larghezza: affiancate + icona il testo veniva tagliato
            OutlinedTextField(
                value = fields.data,
                onValueChange = { fields.data = it },
                label = { Text(strings.fieldDate) },
                placeholder = { Text(strings.datePlaceholder) },
                singleLine = true,
                enabled = enabled,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }, enabled = enabled) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = strings.pickDateCd)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = fields.ora,
                onValueChange = { fields.ora = it },
                label = { Text(strings.fieldTime) },
                placeholder = { Text("HH:MM") },
                singleLine = true,
                enabled = enabled,
                trailingIcon = {
                    IconButton(onClick = { showTimePicker = true }, enabled = enabled) {
                        Icon(Icons.Default.Schedule, contentDescription = strings.pickTimeCd)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = fields.durataMinuti,
                    onValueChange = { fields.durataMinuti = it },
                    label = { Text(strings.fieldDuration) },
                    supportingText = { Text(strings.minutes) },
                    singleLine = true,
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = fields.reminderMinuti,
                    onValueChange = { fields.reminderMinuti = it },
                    label = { Text(strings.fieldReminder) },
                    supportingText = { Text(strings.minutesBefore) },
                    singleLine = true,
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            RecurrenceField(fields = fields, enabled = enabled)
            OutlinedTextField(
                value = fields.luogo,
                onValueChange = { fields.luogo = it },
                label = { Text(strings.fieldLocation) },
                singleLine = true,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Menu a tendina della ripetizione. Se l'LLM ha prodotto una RRULE non tra
 * quelle proposte (es. FREQ=WEEKLY;BYDAY=MO), viene mostrata come
 * "personalizzata" e mantenuta finché non si sceglie un preset.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurrenceField(fields: EventFields, enabled: Boolean) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    // I preset sono le chiavi della mappa i18n: un'unica fonte, niente drift
    val label = strings.recurrenceNames[fields.ricorrenza] ?: strings.recurrenceCustom
    val isPreset = fields.ricorrenza in strings.recurrenceNames

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text(strings.fieldRecurrence) },
            singleLine = true,
            enabled = enabled,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            supportingText = if (!isPreset) {
                { Text(fields.ricorrenza) }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (!isPreset) {
                DropdownMenuItem(
                    text = { Text("${strings.recurrenceCustom} (${fields.ricorrenza})") },
                    onClick = { expanded = false },
                )
            }
            strings.recurrenceNames.forEach { (preset, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        fields.ricorrenza = preset
                        expanded = false
                    },
                )
            }
        }
    }
}
