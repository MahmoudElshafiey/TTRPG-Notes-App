package com.example.dndnotes.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dndnotes.data.backup.BackupFile
import com.example.dndnotes.data.backup.RestoreMode
import com.example.dndnotes.data.prefs.BackupFrequency
import com.example.dndnotes.data.prefs.BackupSettings
import java.util.Calendar

/**
 * The "Automatic Backups" block of the settings screen.
 *
 * Split out of [SettingsScreen] so that file is not carrying both the theme/background
 * controls and the whole backup UI.
 */
@Composable
fun AutomaticBackupsSection(
    viewModel: BackupViewModel,
    onChooseFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val settings = state.settings

    var showTimePicker by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<BackupFile?>(null) }
    var confirmReplaceFor by remember { mutableStateOf<BackupFile?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {

        Text("Automatic Backups", style = MaterialTheme.typography.titleMedium)
        Text(
            "Writes a full copy of every campaign, note, image and consumable to a folder " +
                "you choose. No internet permission is required.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(12.dp))

        // --- Folder ---------------------------------------------------------
        OutlinedButton(
            onClick = onChooseFolder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Folder, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (state.hasFolder) "Change backup folder" else "Choose backup folder")
        }

        if (state.hasFolder) {
            Text(
                text = state.folderName ?: state.settings.folderUri.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        state.folderError?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        // --- Master switch --------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Back up automatically", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = lastBackupSummary(state),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = settings.enabled,
                onCheckedChange = viewModel::setEnabled
            )
        }

        // Everything below is meaningless while the switch is off.
        if (!settings.enabled) {
            Text(
                "Backups are off. You can still create one with the button below.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        // --- Frequency ------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = settings.frequency == BackupFrequency.DAILY,
                onClick = { viewModel.setFrequency(BackupFrequency.DAILY) },
                label = { Text("Daily") },
                enabled = settings.enabled
            )
            FilterChip(
                selected = settings.frequency == BackupFrequency.WEEKLY,
                onClick = { viewModel.setFrequency(BackupFrequency.WEEKLY) },
                label = { Text("Weekly") },
                enabled = settings.enabled
            )
            Spacer(Modifier.weight(1f))
            AssistChip(
                onClick = {
                    if (settings.frequency == BackupFrequency.DAILY) showTimePicker = true
                    else showDayPicker = true
                },
                label = {
                    Text(
                        if (settings.frequency == BackupFrequency.DAILY) {
                            "At ${BackupViewModel.timeLabel(settings.minuteOfDay)}"
                        } else {
                            "${BackupViewModel.dayLabel(settings.dayOfWeek)} " +
                                BackupViewModel.timeLabel(settings.minuteOfDay)
                        }
                    )
                },
                enabled = settings.enabled
            )
        }

        if (settings.frequency == BackupFrequency.WEEKLY && settings.enabled) {
            Text(
                "Days chosen: ${BackupViewModel.dayLabel(settings.dayOfWeek)}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // --- Retention ------------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Backups to keep", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Older files in the folder are deleted automatically.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                settings.keepCount.toString(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            OutlinedButton(
                onClick = { viewModel.setKeepCount(settings.keepCount - 1) },
                enabled = settings.enabled && settings.keepCount > BackupSettings.KEEP_COUNT_RANGE.first,
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) { Text("-") }
            Spacer(Modifier.width(6.dp))
            OutlinedButton(
                onClick = { viewModel.setKeepCount(settings.keepCount + 1) },
                enabled = settings.enabled && settings.keepCount < BackupSettings.KEEP_COUNT_RANGE.last,
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) { Text("+") }
        }

        Spacer(Modifier.height(12.dp))

        // --- Manual backup --------------------------------------------------
        Button(
            onClick = viewModel::backupNow,
            enabled = state.hasFolder && !state.isBackingUp,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isBackingUp) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text("Backing up...")
            } else {
                Text("Back up now")
            }
        }

        // --- Existing backups ----------------------------------------------
        if (state.hasFolder) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Saved backups", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = viewModel::refreshFiles, enabled = !state.isLoadingFiles) {
                    Text("Refresh")
                }
            }

            when {
                state.isLoadingFiles && state.files.isEmpty() -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Reading folder...", style = MaterialTheme.typography.bodySmall)
                    }
                }
                state.files.isEmpty() -> {
                    Text(
                        "No backups in this folder yet.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                else -> {
                    // A plain Column, not a LazyColumn: this already sits inside the
                    // settings screen's own scroll container, and nesting a lazy list in
                    // a scrolling parent needs an explicit height to behave.
                    Column {
                        state.files.forEach { file ->
                            BackupFileRow(
                                file = file,
                                busy = state.isRestoring,
                                onRestore = { pendingRestore = file },
                                onDelete = { viewModel.deleteBackup(file) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        if (state.isRestoring) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Restoring ${state.restoringFileName ?: ""}...",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    // --- Dialogs -----------------------------------------------------------

    if (showTimePicker) {
        TimePickerDialog(
            initialHour = settings.hour,
            initialMinute = settings.minute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                viewModel.setTime(hour, minute)
                showTimePicker = false
            }
        )
    }

    if (showDayPicker) {
        DayOfWeekPickerDialog(
            initialDay = settings.dayOfWeek,
            onDismiss = { showDayPicker = false },
            onConfirm = { day ->
                viewModel.setDayOfWeek(day)
                showDayPicker = false
            }
        )
    }

    // Restore asks every time. Merge is the default because it cannot lose anything.
    pendingRestore?.let { file ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Restore backup") },
            text = {
                Text(
                    "${file.name}\n\n" +
                        "Merge: adds this backup's contents alongside your current notes, " +
                        "leaving everything already there untouched. Safer, but you end up " +
                        "with copies under new names if the notes already exist.\n\n" +
                        "Replace everything: deletes all current data first, then restores " +
                        "the backup exactly as it was."
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.restore(file, RestoreMode.MERGE)
                    pendingRestore = null
                }) { Text("Merge") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    confirmReplaceFor = file
                }) { Text("Replace everything...") }
            }
        )
    }

    // Second, deliberate step before anything is destroyed.
    confirmReplaceFor?.let { file ->
        TypedReplaceConfirmationDialog(
            fileName = file.name,
            onDismiss = { confirmReplaceFor = null },
            onConfirm = {
                viewModel.restore(file, RestoreMode.REPLACE)
                confirmReplaceFor = null
            }
        )
    }
}

@Composable
private fun BackupFileRow(
    file: BackupFile,
    busy: Boolean,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = BackupViewModel.formatTimestamp(file.lastModified) +
                    " · " + BackupViewModel.formatBytes(file.sizeBytes),
                style = MaterialTheme.typography.bodySmall
            )
        }
        IconButton(onClick = onRestore, enabled = !busy) {
            Icon(Icons.Default.Restore, contentDescription = "Restore this backup")
        }
        IconButton(onClick = onDelete, enabled = !busy) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete this backup",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun lastBackupSummary(state: BackupUiState): String {
    val last = state.lastBackup
    return if (!last.hasBackup) {
        "No automatic backup has run yet."
    } else {
        "Last backup: ${BackupViewModel.formatTimestamp(last.at)} · " +
            BackupViewModel.formatBytes(last.sizeBytes)
    }
}

@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backup time") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Android does not run backups at an exact minute - battery saver and " +
                        "Doze can shift it by a few hours.",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberStepper(label = "Hour", value = hour, range = 0..23) { hour = it }
                    NumberStepper(label = "Minute", value = minute, range = 0..59) { minute = it }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(hour, minute) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun NumberStepper(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { if (value > range.first) onChange(value - 1) }) { Text("-") }
            Text(
                // Zero-padded without consulting the locale: these are clock digits,
                // not user-facing prose.
                value.toString().padStart(2, '0'),
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(onClick = { if (value < range.last) onChange(value + 1) }) { Text("+") }
        }
    }
}

@Composable
private fun DayOfWeekPickerDialog(
    initialDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var day by remember { mutableStateOf(initialDay) }
    // Calendar.MONDAY == 2 ... Calendar.SUNDAY == 1
    val days = listOf(
        Calendar.MONDAY to "Mon",
        Calendar.TUESDAY to "Tue",
        Calendar.WEDNESDAY to "Wed",
        Calendar.THURSDAY to "Thu",
        Calendar.FRIDAY to "Fri",
        Calendar.SATURDAY to "Sat",
        Calendar.SUNDAY to "Sun"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backup day") },
        text = {
            Column {
                days.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { day = value }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        if (day == value) {
                            Text("Selected", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(day) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * Second gate in front of a destructive restore: the user has to type the word REPLACE.
 */
@Composable
private fun TypedReplaceConfirmationDialog(
    fileName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var typed by remember { mutableStateOf("") }
    val confirmed = typed.trim().equals("REPLACE", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete everything?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "All campaigns, categories, notes, images and consumables currently in " +
                        "the app will be permanently deleted, then $fileName will be restored.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "This cannot be undone. Type REPLACE to continue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    singleLine = true,
                    label = { Text("REPLACE") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = confirmed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Delete all and restore") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep my data") } }
    )
}