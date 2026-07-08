package com.flowernotes.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowernotes.data.ThemeMode
import com.flowernotes.i18n.AppLanguage
import com.flowernotes.i18n.LocalStrings
import com.flowernotes.llm.GeminiModels
import com.flowernotes.llm.LlmProviderType
import com.flowernotes.ui.theme.Accents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenInfo: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val strings = LocalStrings.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Mostra il feedback (chiave salvata/rimossa) come snackbar
    LaunchedEffect(viewModel.feedback) {
        viewModel.consumeFeedback()?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenInfo) {
                        Icon(Icons.Default.Info, contentDescription = strings.infoCd)
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

            ThemeCard(viewModel)
            LanguageCard(viewModel)
            ProviderCard(viewModel)
            if (viewModel.provider == LlmProviderType.GEMINI) {
                ApiKeyCard(viewModel)
                ModelCard(viewModel)
            } else {
                OllamaCard(viewModel)
            }
            EventDefaultsCard(viewModel)
        }
    }
}

/** Selettore accento colore (dinamico Material You o predefiniti) e tema chiaro/scuro */
@Composable
private fun ThemeCard(viewModel: SettingsViewModel) {
    val strings = LocalStrings.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(strings.themeTitle, style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (Accents.dynamicAvailable()) {
                    AccentSwatch(
                        label = strings.dynamicAccentLabel,
                        selected = viewModel.accent == Accents.DYNAMIC,
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0xFFE53935), Color(0xFFEF8F00), Color(0xFF43A047),
                                Color(0xFF1E88E5), Color(0xFF6750A4), Color(0xFFE53935),
                            )
                        ),
                        onClick = { viewModel.selectAccent(Accents.DYNAMIC) },
                    )
                }
                Accents.OPTIONS.forEach { option ->
                    AccentSwatch(
                        label = strings.accentNames[option.id] ?: option.label,
                        selected = viewModel.accent == option.id,
                        brush = Brush.linearGradient(listOf(option.seed, option.seed)),
                        onClick = { viewModel.selectAccent(option.id) },
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = viewModel.themeMode == mode,
                        onClick = { viewModel.selectThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ThemeMode.entries.size,
                        ),
                    ) {
                        Text(strings.themeModeLabels[mode.id] ?: mode.id)
                    }
                }
            }
        }
    }
}

/** Lingua dell'app (vale anche per riconoscimento vocale e prompt del modello) */
@Composable
private fun LanguageCard(viewModel: SettingsViewModel) {
    val strings = LocalStrings.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(strings.languageTitle, style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AppLanguage.entries.forEachIndexed { index, language ->
                    SegmentedButton(
                        selected = viewModel.language == language,
                        onClick = { viewModel.selectLanguage(language) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = AppLanguage.entries.size,
                        ),
                    ) {
                        Text(strings.languageNames[language.id] ?: language.id)
                    }
                }
            }
        }
    }
}

/** Pallino colore selezionabile, come nei selettori tema delle app Material You */
@Composable
private fun AccentSwatch(
    label: String,
    selected: Boolean,
    brush: Brush,
    onClick: () -> Unit,
) {
    val strings = LocalStrings.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(brush),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                onClick = onClick,
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (selected) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "$label ${strings.selectedCd}",
                            tint = Color.White,
                        )
                    }
                }
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/** Scelta del provider: cloud Gemini oppure server Ollama self-hosted */
@Composable
private fun ProviderCard(viewModel: SettingsViewModel) {
    val strings = LocalStrings.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(strings.providerTitle, style = MaterialTheme.typography.titleMedium)
            Text(
                strings.providerHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                LlmProviderType.entries.forEachIndexed { index, providerType ->
                    SegmentedButton(
                        selected = viewModel.provider == providerType,
                        onClick = { viewModel.selectProvider(providerType) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = LlmProviderType.entries.size,
                        ),
                    ) {
                        Text(strings.providerNames[providerType.id] ?: providerType.id)
                    }
                }
            }
        }
    }
}

/** Indirizzo e modello del server Ollama, con salvataggio esplicito */
@Composable
private fun OllamaCard(viewModel: SettingsViewModel) {
    val strings = LocalStrings.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(strings.providerNames["ollama"] ?: "Ollama", style = MaterialTheme.typography.titleMedium)
            Text(
                strings.ollamaHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = viewModel.ollamaUrlInput,
                onValueChange = { viewModel.ollamaUrlInput = it },
                label = { Text(strings.ollamaUrlLabel) },
                placeholder = { Text("http://192.168.1.10:11434") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                supportingText = {
                    Text(
                        if (viewModel.savedOllamaUrlExists) strings.ollamaUrlConfigured
                        else strings.ollamaNoUrlSaved
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = viewModel.ollamaModelInput,
                onValueChange = { viewModel.ollamaModelInput = it },
                label = { Text(strings.ollamaModelLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { viewModel.saveOllama() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Text(" ${strings.saveButton}")
            }
        }
    }
}

@Composable
private fun ApiKeyCard(viewModel: SettingsViewModel) {
    val strings = LocalStrings.current
    var showKey by rememberSaveable { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(strings.apiKeyTitle, style = MaterialTheme.typography.titleMedium)
            Text(
                strings.apiKeyHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = viewModel.apiKeyInput,
                onValueChange = { viewModel.apiKeyInput = it },
                label = { Text(strings.apiKeyLabel) },
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
                            contentDescription = if (showKey) strings.hideKeyCd else strings.showKeyCd,
                        )
                    }
                },
                supportingText = {
                    Text(
                        if (viewModel.savedKeyExists) strings.keyConfigured
                        else strings.noKeySaved
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
                    Text(" ${strings.saveButton}")
                }
                OutlinedButton(
                    onClick = { viewModel.clearKey() },
                    modifier = Modifier.weight(1f),
                    enabled = viewModel.savedKeyExists || viewModel.apiKeyInput.isNotEmpty(),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text(" ${strings.removeButton}")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelCard(viewModel: SettingsViewModel) {
    val strings = LocalStrings.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(strings.modelTitle, style = MaterialTheme.typography.titleMedium)
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                // Campo editabile: si può anche digitare l'id di un modello
                // non ancora in lista (es. versioni future)
                OutlinedTextField(
                    value = viewModel.model,
                    onValueChange = { viewModel.selectModel(it) },
                    label = { Text(strings.modelLabel) },
                    singleLine = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    supportingText = {
                        Text(strings.modelDescriptions[viewModel.model] ?: strings.customModel)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    GeminiModels.AVAILABLE.forEach { id ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(id, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        strings.modelDescriptions[id].orEmpty(),
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

/** Durata e promemoria predefiniti per i nuovi eventi */
@Composable
private fun EventDefaultsCard(viewModel: SettingsViewModel) {
    val strings = LocalStrings.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(strings.newEventsTitle, style = MaterialTheme.typography.titleMedium)
            Text(
                strings.newEventsHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = viewModel.durationInput,
                    onValueChange = { viewModel.onDurationChange(it) },
                    label = { Text(strings.fieldDuration) },
                    supportingText = { Text(strings.minutes) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = viewModel.reminderInput,
                    onValueChange = { viewModel.onReminderChange(it) },
                    label = { Text(strings.fieldReminder) },
                    supportingText = { Text(strings.minutesBefore) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
