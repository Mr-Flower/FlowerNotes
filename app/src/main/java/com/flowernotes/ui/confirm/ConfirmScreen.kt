package com.flowernotes.ui.confirm

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowernotes.data.EventoData
import com.flowernotes.i18n.LocalStrings

/**
 * Schermata di conferma/modifica. In creazione mostra una card per ciascun
 * evento estratto dal testo (possono essere più d'uno); in modifica carica
 * l'evento dal calendario e ne mostra una sola.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmScreen(
    initialEventi: List<EventoData>,
    editEventId: Long?,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    viewModel: ConfirmViewModel = viewModel(),
) {
    val strings = LocalStrings.current
    // Una sola volta, non a ogni ricomposizione
    LaunchedEffect(Unit) {
        if (editEventId != null) {
            viewModel.loadEdit(editEventId)
        } else {
            viewModel.loadNew(initialEventi)
        }
    }
    val uiState = viewModel.uiState

    LaunchedEffect(uiState) {
        if (uiState is ConfirmUiState.Saved) {
            onSaved()
        }
    }

    // Permessi calendario richiesti al momento del salvataggio
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            viewModel.save()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            viewModel.isEditMode -> strings.confirmEditTitle
                            viewModel.eventi.size > 1 -> strings.confirmTitleMulti
                            else -> strings.confirmTitle
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.cancel)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (uiState is ConfirmUiState.Loading) {
                CircularProgressIndicator()
                return@Column
            }

            viewModel.eventi.forEachIndexed { index, fields ->
                EventoCard(
                    fields = fields,
                    enabled = uiState !is ConfirmUiState.Saving,
                    onRemove = if (viewModel.eventi.size > 1) {
                        { viewModel.removeAt(index) }
                    } else null,
                )
            }

            if (uiState is ConfirmUiState.Error) {
                Text(
                    uiState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (uiState is ConfirmUiState.Saving) {
                CircularProgressIndicator()
            }

            // Stessa larghezza (weight) e stessa altezza fissa: etichetta corta
            // per evitare che "Salva sul calendario" vada su due righe
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    enabled = uiState !is ConfirmUiState.Saving,
                ) {
                    Text(strings.cancel, maxLines = 1)
                }
                Button(
                    onClick = {
                        viewModel.dismissError()
                        val validationError = viewModel.validate()
                        if (validationError == null) {
                            calendarPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.WRITE_CALENDAR,
                                )
                            )
                        } else {
                            viewModel.save() // save() mostrerà l'errore di validazione
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    enabled = uiState !is ConfirmUiState.Saving && viewModel.eventi.isNotEmpty(),
                ) {
                    Text(strings.saveButton, maxLines = 1)
                }
            }
        }
    }
}
