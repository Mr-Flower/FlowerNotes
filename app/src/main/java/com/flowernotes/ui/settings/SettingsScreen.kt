package com.flowernotes.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowernotes.llm.GeminiModels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showKey by rememberSaveable { mutableStateOf(false) }

    // Mostra il feedback (chiave salvata/rimossa) come snackbar
    LaunchedEffect(viewModel.feedback) {
        viewModel.consumeFeedback()?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Impostazioni") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!viewModel.loaded) return@Column

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("API key di Gemini", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Crea la tua chiave gratuita su aistudio.google.com/apikey. " +
                            "Resta salvata solo su questo dispositivo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = viewModel.apiKeyInput,
                        onValueChange = { viewModel.apiKeyInput = it },
                        label = { Text("API key") },
                        singleLine = true,
                        visualTransformation = if (showKey) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showKey) "Nascondi chiave" else "Mostra chiave",
                                )
                            }
                        },
                        supportingText = {
                            Text(
                                if (viewModel.savedKeyExists) "Chiave configurata ✓"
                                else "Nessuna chiave salvata"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { viewModel.saveKey() },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Text(" Salva")
                        }
                        OutlinedButton(
                            onClick = { viewModel.clearKey() },
                            modifier = Modifier.weight(1f),
                            enabled = viewModel.savedKeyExists || viewModel.apiKeyInput.isNotEmpty(),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Text(" Rimuovi")
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Colori Material You", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Usa l'accento del telefono (Android 12+). Disattivalo per la palette rosa dell'app.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = viewModel.dynamicColor,
                        onCheckedChange = { viewModel.onDynamicColorChange(it) },
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Modello", style = MaterialTheme.typography.titleMedium)
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                    ) {
                        OutlinedTextField(
                            value = viewModel.model,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Modello Gemini") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            supportingText = {
                                Text(
                                    GeminiModels.AVAILABLE
                                        .firstOrNull { it.first == viewModel.model }?.second
                                        ?: ""
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            GeminiModels.AVAILABLE.forEach { (id, description) ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(id, style = MaterialTheme.typography.bodyLarge)
                                            Text(
                                                description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectModel(id)
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
