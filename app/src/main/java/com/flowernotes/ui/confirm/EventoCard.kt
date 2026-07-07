package com.flowernotes.ui.confirm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Card riusabile con i campi dell'evento, condivisa tra flusso vocale e manuale
 * (entrambi convergono sulla schermata di conferma).
 * Data e ora si modificano con i picker Material 3 oppure a mano.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventoCard(
    viewModel: ConfirmViewModel,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    if (showDatePicker) {
        val initialMillis = try {
            LocalDate.parse(viewModel.data.trim())
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
                        viewModel.data = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annulla") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val initialTime = try {
            LocalTime.parse(viewModel.ora.trim())
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
                shape = androidx.compose.material3.MaterialTheme.shapes.extraLarge,
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
                        TextButton(onClick = { showTimePicker = false }) { Text("Annulla") }
                        TextButton(onClick = {
                            viewModel.ora = "%02d:%02d".format(
                                timePickerState.hour, timePickerState.minute,
                            )
                            showTimePicker = false
                        }) { Text("OK") }
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
            OutlinedTextField(
                value = viewModel.titolo,
                onValueChange = { viewModel.titolo = it },
                label = { Text("Titolo") },
                singleLine = true,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            // Data e ora a piena larghezza: affiancate + icona il testo veniva tagliato
            OutlinedTextField(
                value = viewModel.data,
                onValueChange = { viewModel.data = it },
                label = { Text("Data") },
                placeholder = { Text("AAAA-MM-GG") },
                singleLine = true,
                enabled = enabled,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }, enabled = enabled) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Scegli data")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = viewModel.ora,
                onValueChange = { viewModel.ora = it },
                label = { Text("Ora") },
                placeholder = { Text("HH:MM") },
                singleLine = true,
                enabled = enabled,
                trailingIcon = {
                    IconButton(onClick = { showTimePicker = true }, enabled = enabled) {
                        Icon(Icons.Default.Schedule, contentDescription = "Scegli ora")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = viewModel.durataMinuti,
                    onValueChange = { viewModel.durataMinuti = it },
                    label = { Text("Durata") },
                    supportingText = { Text("minuti") },
                    singleLine = true,
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = viewModel.reminderMinuti,
                    onValueChange = { viewModel.reminderMinuti = it },
                    label = { Text("Avviso") },
                    supportingText = { Text("minuti prima") },
                    singleLine = true,
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = viewModel.luogo,
                onValueChange = { viewModel.luogo = it },
                label = { Text("Luogo (opzionale)") },
                singleLine = true,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
