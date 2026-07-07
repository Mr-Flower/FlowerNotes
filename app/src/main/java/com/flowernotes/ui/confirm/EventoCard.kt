package com.flowernotes.ui.confirm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Card riusabile con i campi dell'evento, condivisa tra flusso vocale e manuale
 * (entrambi convergono sulla schermata di conferma).
 */
@Composable
fun EventoCard(
    viewModel: ConfirmViewModel,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = viewModel.data,
                    onValueChange = { viewModel.data = it },
                    label = { Text("Data") },
                    placeholder = { Text("AAAA-MM-GG") },
                    singleLine = true,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = viewModel.ora,
                    onValueChange = { viewModel.ora = it },
                    label = { Text("Ora") },
                    placeholder = { Text("HH:MM") },
                    singleLine = true,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = viewModel.durataMinuti,
                    onValueChange = { viewModel.durataMinuti = it },
                    label = { Text("Durata (min)") },
                    singleLine = true,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = viewModel.reminderMinuti,
                    onValueChange = { viewModel.reminderMinuti = it },
                    label = { Text("Promemoria (min prima)") },
                    singleLine = true,
                    enabled = enabled,
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
