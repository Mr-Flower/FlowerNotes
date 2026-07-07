package com.flowernotes.ui.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowernotes.R
import com.flowernotes.data.EventoData
import com.flowernotes.i18n.LocalStrings
import kotlinx.coroutines.delay
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onEventExtracted: (EventoData) -> Unit,
    onOpenManual: () -> Unit,
    onOpenList: () -> Unit,
    onOpenSettings: () -> Unit,
    startListenTrigger: Int = 0,
    viewModel: HomeViewModel = viewModel(),
) {
    val strings = LocalStrings.current
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

    // Avvio automatico dell'ascolto quando si arriva dal Quick Tile
    LaunchedEffect(startListenTrigger) {
        if (startListenTrigger > 0 && uiState is HomeUiState.Idle) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Frase d'esempio: casuale a ogni apertura, ruota ogni 8 secondi
    val examples = strings.homeExamples
    var exampleIndex by remember { mutableIntStateOf(Random.nextInt(examples.size)) }
    LaunchedEffect(examples) {
        while (true) {
            delay(8000)
            exampleIndex = (exampleIndex + 1) % examples.size
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = strings.settingsCd)
                    }
                },
            )
        },
    ) { padding ->
        val isListening = uiState is HomeUiState.Listening
        val isProcessing = uiState is HomeUiState.Processing || uiState is HomeUiState.Extracted

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
        ) {
            // Branding + stato nella fascia superiore, sopra il microfono
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painterResource(R.drawable.logo_artwork),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(84.dp),
                )
                Text(
                    "FlowerNotes",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    strings.homeTagline,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(12.dp))

                when (val state = uiState) {
                    is HomeUiState.Idle -> {
                        Text(
                            strings.homeInstruction,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                        AnimatedContent(
                            targetState = examples[exampleIndex],
                            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                            label = "example",
                        ) { example ->
                            Text(
                                "“$example”",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    is HomeUiState.Listening -> {
                        Text(
                            strings.listeningTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            state.partialText,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
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
                            strings.processingTitle,
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
                            strings.errorRetryHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Microfono al centro. Mentre ascolta, onde concentriche che
            // si propagano sfumando allargandosi.
            val waveColor = MaterialTheme.colorScheme.primary
            val waveTransition = rememberInfiniteTransition(label = "waves")
            val waveProgress = List(3) { index ->
                waveTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2100, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                        initialStartOffset = StartOffset(index * 700),
                    ),
                    label = "wave$index",
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(140.dp)
                    .drawBehind {
                        if (isListening) {
                            waveProgress.forEach { progress ->
                                val p = progress.value
                                drawCircle(
                                    color = waveColor,
                                    radius = size.minDimension / 2f * (1f + p * 0.9f),
                                    alpha = (1f - p) * 0.4f,
                                    style = Stroke(width = 3.dp.toPx()),
                                )
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                FloatingActionButton(
                    onClick = {
                        when {
                            isListening -> viewModel.stopListening()
                            isProcessing -> {}
                            else -> micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    containerColor = if (isListening) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                ) {
                    Icon(
                        if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isListening) {
                            strings.stopListeningCd
                        } else {
                            strings.startListeningCd
                        },
                        modifier = Modifier.size(56.dp),
                    )
                }
            }

            // Azioni secondarie in basso, stile quick tile di Android
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TileButton(
                    icon = Icons.Default.Edit,
                    label = strings.writeButton,
                    onClick = onOpenManual,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                )
                TileButton(
                    icon = Icons.AutoMirrored.Filled.EventNote,
                    label = strings.eventsButton,
                    onClick = onOpenList,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Bottone largo e arrotondato, nello stile dei quick tile di Android */
@Composable
private fun TileButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier.height(72.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.size(10.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}
