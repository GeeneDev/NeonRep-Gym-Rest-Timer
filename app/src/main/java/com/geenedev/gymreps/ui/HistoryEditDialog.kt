package com.geenedev.gymreps.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.geenedev.gymreps.model.HistoryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modal card prefilled with a history entry. The date it was saved is read-only;
 * name, icon, sets, time, rest and notes can be changed. Save hands back the edited copy.
 */
@Composable
fun HistoryEditDialog(
    entry: HistoryEntry,
    onSave: (HistoryEntry) -> Unit,
    onDismiss: () -> Unit,
    /** Photo row (current photo with change/remove, or add); applied by the caller on Save. */
    photo: @Composable () -> Unit = {},
) {
    var name by remember { mutableStateOf(entry.context) }
    var iconKey by remember { mutableStateOf(entry.iconKey.ifEmpty { ContextIcons.DEFAULT_KEY }) }
    var sets by remember { mutableStateOf(entry.sets) }
    val totalSeconds = entry.durationMs / 1000
    var minutes by remember { mutableStateOf((totalSeconds / 60).toString()) }
    var seconds by remember { mutableStateOf((totalSeconds % 60).toString()) }
    var rest by remember { mutableStateOf(if (entry.restSeconds > 0) entry.restSeconds.toString() else "") }
    var notes by remember { mutableStateOf(entry.notes) }
    var showIconPicker by remember { mutableStateOf(false) }

    val date = remember(entry.finishedAt) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(entry.finishedAt))
    }
    val canSave = name.isNotBlank() && sets > 0

    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Edit entry", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

                // Read-only: when the exercise was saved.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Event,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Saved $date",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = { showIconPicker = true }, modifier = Modifier.size(56.dp)) {
                        Icon(ContextIcons.forKey(iconKey), contentDescription = "Choose icon")
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Exercise") },
                        singleLine = true,
                        isError = name.isBlank(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.weight(1f),
                    )
                }

                photo()

                Column {
                    Text("Sets", style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(onClick = { if (sets > 1) sets-- }, enabled = sets > 1) {
                            Icon(Icons.Filled.Remove, contentDescription = "One set less")
                        }
                        Text(
                            sets.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(64.dp),
                        )
                        FilledTonalIconButton(onClick = { if (sets < 999) sets++ }) {
                            Icon(Icons.Filled.Add, contentDescription = "One set more")
                        }
                    }
                }

                Column {
                    Text("Time", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(minutes, { minutes = it }, "Min", Modifier.weight(1f), maxLength = 3)
                        NumberField(seconds, { seconds = it }, "Sec", Modifier.weight(1f), maxLength = 2)
                    }
                }

                NumberField(rest, { rest = it }, "Rest (seconds)", Modifier.fillMaxWidth(), maxLength = 4)

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    placeholder = { Text("Weights, machine height…") },
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = canSave,
                        onClick = {
                            val m = minutes.toLongOrNull() ?: 0L
                            val s = (seconds.toLongOrNull() ?: 0L).coerceAtMost(59)
                            onSave(
                                entry.copy(
                                    context = name.trim(),
                                    iconKey = iconKey,
                                    sets = sets,
                                    durationMs = (m * 60 + s) * 1000,
                                    restSeconds = rest.toIntOrNull() ?: 0,
                                    notes = notes.trim(),
                                ),
                            )
                        },
                    ) { Text("Save") }
                }
            }
        }
    }

    if (showIconPicker) {
        IconPickerDialog(
            selected = iconKey,
            onPick = {
                iconKey = it
                showIconPicker = false
            },
            onDismiss = { showIconPicker = false },
        )
    }
}

@Composable
private fun NumberField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier,
    maxLength: Int,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(maxLength)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}
