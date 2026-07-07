package com.flowernotes.ui.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowernotes.data.EventoData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onEventExtracted: (EventoData) -> Unit,
    onOpenManual: () -> Unit,
    onOpenList: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Quando l'estrazione è completata, naviga alla schermata di conferma
    LaunchedEffect(uiState) {
        val state = uiState
        if (state is HomeUiState.Extracted) {
            viewModel.reset()
            onEventExtracted(state.evento)
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startListening()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flower Notes") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Impostazioni")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Area di stato centrale
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (val state = uiState) {
                        is HomeUiState.Idle -> {
                            Text(
                                "Tocca il microfono\ne detta l'evento",
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                "es. \"devo andare dal barbiere domani alle 15\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                        is HomeUiState.Listening -> {
                            Text(
                                "Ti ascolto…",
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Text(
                                state.partialText.ifBlank { " " },
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                        }
                        is HomeUiState.Processing -> {
                            CircularProgressIndicator()
                            Text(
                                "“${state.recognizedText}”",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                "Sto interpretando…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        is HomeUiState.Extracted -> {
                            CircularProgressIndicator()
                        }
                        is HomeUiState.Error -> {
                            Text(
                                state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                "Tocca il microfono per riprovare",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Pulsante microfono, con "respiro" mentre ascolta
            val isListening = uiState is HomeUiState.Listening
            val isProcessing = uiState is HomeUiState.Processing || uiState is HomeUiState.Extracted
            val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                initialValue = 1f,
                targetValue = if (isListening) 1.12f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "pulseScale",
            )
            FloatingActionButton(
                onClick = {
                    when {
                        isListening -> viewModel.stopListening()
                        isProcessing -> {}
                        else -> micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier
                    .size(104.dp)
                    .scale(if (isListening) pulse else 1f),
                containerColor = if (isListening) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
            ) {
                Icon(
                    if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isListening) "Ferma ascolto" else "Avvia ascolto",
                    modifier = Modifier.size(44.dp),
                )
            }

            Spacer(Modifier.height(32.dp))

            // Azioni secondarie ben raggiungibili col pollice
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalButton(
                    onClick = onOpenManual,
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing,
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Text(" Scrivi")
                }
                FilledTonalButton(
                    onClick = onOpenList,
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing,
                ) {
                    Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = null)
                    Text(" Eventi")
                }
            }
        }
    }
}
