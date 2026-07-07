package com.flowernotes.ui.manual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowernotes.data.EventoData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualScreen(
    onEventExtracted: (EventoData) -> Unit,
    onBack: () -> Unit,
    viewModel: ManualViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var text by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is ManualUiState.Extracted) {
            viewModel.reset()
            onEventExtracted(state.evento)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inserimento manuale") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Scrivi l'evento come lo detteresti a voce, ci pensa l'assistente a compilare i campi.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                label = { Text("Testo dell'evento") },
                placeholder = { Text("es. pranzo con Anna sabato all'una da Gino") },
                enabled = uiState !is ManualUiState.Processing,
            )

            when (val state = uiState) {
                is ManualUiState.Processing -> CircularProgressIndicator()
                is ManualUiState.Error -> Text(
                    state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                else -> {}
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.extract(text) },
                enabled = uiState !is ManualUiState.Processing && text.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Analizza")
            }
        }
    }
}
