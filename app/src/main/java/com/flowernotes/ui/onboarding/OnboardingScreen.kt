package com.flowernotes.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowernotes.llm.LlmProviderType
import com.flowernotes.i18n.LocalStrings

/**
 * Configurazione guidata al primo avvio, in quattro passi:
 * benvenuto → permessi → scelta del provider → configurazione.
 * Sempre saltabile: tutto si può fare dopo dalle impostazioni.
 */
@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = viewModel()) {
    val strings = LocalStrings.current

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { viewModel.skip() }) {
                    Text(strings.onbSkip)
                }
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = viewModel.step,
            label = "onboarding",
        ) { step ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (step) {
                    0 -> WelcomeStep(viewModel)
                    1 -> PermissionsStep(viewModel)
                    2 -> ProviderStep(viewModel)
                    else -> ConfigStep(viewModel)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun WelcomeStep(viewModel: OnboardingViewModel) {
    val strings = LocalStrings.current
    Spacer(Modifier.height(48.dp))
    Text(
        "FlowerNotes",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
    )
    Text(
        strings.homeTagline,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        strings.appDescription,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(16.dp))
    Button(onClick = { viewModel.next() }, modifier = Modifier.fillMaxWidth()) {
        Text(strings.onbStart)
    }
}

@Composable
private fun PermissionsStep(viewModel: OnboardingViewModel) {
    val strings = LocalStrings.current
    val context = LocalContext.current

    fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    var micGranted by remember { mutableStateOf(granted(Manifest.permission.RECORD_AUDIO)) }
    var calGranted by remember {
        mutableStateOf(
            granted(Manifest.permission.READ_CALENDAR) &&
                granted(Manifest.permission.WRITE_CALENDAR)
        )
    }
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { micGranted = it }
    val calLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants -> calGranted = grants.values.all { it } }

    Text(strings.onbPermTitle, style = MaterialTheme.typography.headlineSmall)
    Text(
        strings.onbPermText,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
    PermissionRow(
        icon = Icons.Default.Mic,
        label = strings.onbMicPerm,
        granted = micGranted,
        onGrant = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
    )
    PermissionRow(
        icon = Icons.Default.CalendarMonth,
        label = strings.onbCalPerm,
        granted = calGranted,
        onGrant = {
            calLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR,
                )
            )
        },
    )
    Spacer(Modifier.height(8.dp))
    Button(onClick = { viewModel.next() }, modifier = Modifier.fillMaxWidth()) {
        Text(strings.onbNext)
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    label: String,
    granted: Boolean,
    onGrant: () -> Unit,
) {
    val strings = LocalStrings.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null)
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            if (granted) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = strings.onbGranted,
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                Button(onClick = onGrant) { Text(strings.onbGrant) }
            }
        }
    }
}

@Composable
private fun ProviderStep(viewModel: OnboardingViewModel) {
    val strings = LocalStrings.current
    Text(strings.onbProviderTitle, style = MaterialTheme.typography.headlineSmall)
    Text(
        strings.providerHint,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
    val descriptions = mapOf(
        LlmProviderType.GEMINI to strings.onbGeminiDesc,
        LlmProviderType.OLLAMA to strings.onbOllamaDesc,
        LlmProviderType.LOCAL to strings.onbLocalDesc,
    )
    LlmProviderType.entries.forEach { providerType ->
        Card(
            onClick = { viewModel.selectProvider(providerType) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = viewModel.provider == providerType,
                    onClick = { viewModel.selectProvider(providerType) },
                )
                Column {
                    Text(
                        strings.providerNames[providerType.id] ?: providerType.id,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        descriptions[providerType].orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Button(onClick = { viewModel.next() }, modifier = Modifier.fillMaxWidth()) {
        Text(strings.onbNext)
    }
    TextButton(onClick = { viewModel.back() }) { Text(strings.back) }
}

@Composable
private fun ConfigStep(viewModel: OnboardingViewModel) {
    val strings = LocalStrings.current
    val uriHandler = LocalUriHandler.current

    when (viewModel.provider) {
        LlmProviderType.GEMINI -> {
            Text(strings.apiKeyTitle, style = MaterialTheme.typography.headlineSmall)
            Text(strings.onbGeminiHow, style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(
                onClick = { uriHandler.openUri("https://aistudio.google.com/apikey") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(strings.onbOpenAiStudio)
            }
            OutlinedTextField(
                value = viewModel.apiKeyInput,
                onValueChange = { viewModel.apiKeyInput = it },
                label = { Text(strings.apiKeyLabel) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        LlmProviderType.OLLAMA -> {
            Text(strings.providerNames["ollama"] ?: "Ollama", style = MaterialTheme.typography.headlineSmall)
            Text(strings.onbOllamaHow, style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = viewModel.ollamaUrlInput,
                onValueChange = { viewModel.ollamaUrlInput = it },
                label = { Text(strings.ollamaUrlLabel) },
                placeholder = { Text("http://192.168.1.10:11434") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = viewModel.ollamaModelInput,
                onValueChange = { viewModel.ollamaModelInput = it },
                label = { Text(strings.ollamaModelLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        LlmProviderType.LOCAL -> {
            val pickModelLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri -> uri?.let { viewModel.importLocalModel(it) } }

            Text(strings.localTitle, style = MaterialTheme.typography.headlineSmall)
            Text(strings.onbLocalHow, style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(
                onClick = { uriHandler.openUri("https://huggingface.co/litert-community/Gemma3-1B-IT") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(strings.localDownloadButton)
            }
            if (viewModel.importingModel) {
                LinearProgressIndicator(
                    progress = { viewModel.importProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "${(viewModel.importProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Button(
                    onClick = { pickModelLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(strings.localPickButton)
                }
                Text(
                    if (viewModel.localModelReady) strings.localModelConfigured
                    else strings.localNoModel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            viewModel.importError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { viewModel.finish() },
        enabled = !viewModel.importingModel,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(strings.onbDone)
    }
    TextButton(onClick = { viewModel.back() }) { Text(strings.back) }
}
