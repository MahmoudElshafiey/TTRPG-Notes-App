package com.example.dndnotes.ui.screens

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dndnotes.data.backup.BackupFile
import com.example.dndnotes.data.backup.BackupFileStore
import com.example.dndnotes.data.backup.BackupManager
import com.example.dndnotes.data.backup.BackupOutcome
import com.example.dndnotes.data.backup.BackupScheduler
import com.example.dndnotes.data.backup.BackupSerializer
import com.example.dndnotes.data.backup.BackupTrigger
import com.example.dndnotes.data.backup.RestoreMode
import com.example.dndnotes.data.prefs.BackupFrequency
import com.example.dndnotes.data.prefs.BackupPreferences
import com.example.dndnotes.data.prefs.BackupSettings
import com.example.dndnotes.data.prefs.LastBackupInfo
import com.example.dndnotes.data.repository.DndRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupUiState(
    val settings: BackupSettings = BackupSettings(),
    val folderName: String? = null,
    val lastBackup: LastBackupInfo = LastBackupInfo(0L, null, 0L),
    val files: List<BackupFile> = emptyList(),
    val isLoadingFiles: Boolean = false,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val restoringFileName: String? = null,
    /** Set when the chosen folder can no longer be read or written. */
    val folderError: String? = null,
    val message: String? = null
) {
    val hasFolder: Boolean get() = !settings.folderUri.isNullOrBlank()
}

/**
 * Drives the "Automatic Backups" section of the settings screen.
 *
 * Persisting a setting and re-arming the schedule happen together, so the WorkManager
 * schedule can never drift away from what the user sees on screen.
 */
class BackupViewModel(
    application: Application,
    private val repository: DndRepository,
    private val backupPreferences: BackupPreferences,
    private val backupManager: BackupManager,
    private val backupFileStore: BackupFileStore
) : AndroidViewModel(application) {

    private val transient = MutableStateFlow(
        TransientState(files = emptyList(), isLoadingFiles = false, isBackingUp = false, isRestoring = false, restoringFileName = null, folderError = null, message = null)
    )

    val uiState: StateFlow<BackupUiState> =
        combine(backupPreferences.settingsFlow, backupPreferences.lastBackupInfoFlow, transient) { settings, last, t ->
            BackupUiState(
                settings = settings,
                folderName = backupManager.describeFolder(settings),
                lastBackup = last,
                files = t.files,
                isLoadingFiles = t.isLoadingFiles,
                isBackingUp = t.isBackingUp,
                isRestoring = t.isRestoring,
                restoringFileName = t.restoringFileName,
                folderError = t.folderError,
                message = t.message
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupUiState())

    private data class TransientState(
        val files: List<BackupFile> = emptyList(),
        val isLoadingFiles: Boolean = false,
        val isBackingUp: Boolean = false,
        val isRestoring: Boolean = false,
        val restoringFileName: String? = null,
        val folderError: String? = null,
        val message: String? = null
    )

    init {
        refreshFiles()
    }

    // region Schedule settings

    fun setEnabled(enabled: Boolean) = updateSettings { backupPreferences.setEnabled(enabled) }

    fun setFrequency(frequency: BackupFrequency) = updateSettings { backupPreferences.setFrequency(frequency) }

    fun setTime(hour: Int, minute: Int) = updateSettings { backupPreferences.setTime(hour, minute) }

    fun setDayOfWeek(dayOfWeek: Int) = updateSettings { backupPreferences.setDayOfWeek(dayOfWeek) }

    fun setKeepCount(count: Int) = updateSettings { backupPreferences.setKeepCount(count) }

    private fun updateSettings(persist: suspend () -> Unit) {
        viewModelScope.launch {
            persist()
            // Re-arm from the value that was just written rather than the current one,
            // which may not have been collected yet.
            BackupScheduler.schedule(getApplication(), backupPreferences.currentSettings())
        }
    }

    // endregion

    // region Folder

    /** Called with the tree URI returned by the folder picker. */
    fun onFolderSelected(uri: Uri?) {
        viewModelScope.launch {
            backupPreferences.setFolderUri(uri?.toString())
            if (uri == null) {
                transient.update { it.copy(folderError = null, files = emptyList()) }
                return@launch
            }
            BackupScheduler.schedule(getApplication(), backupPreferences.currentSettings())
            transient.update { it.copy(folderError = null) }
            refreshFiles()
            takePersistableSafely(uri)
        }
    }

    private fun takePersistableSafely(uri: Uri) {
        val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            getApplication<Application>().contentResolver.takePersistableUriPermission(uri, flags)
        }.onFailure { Log.w(TAG, "Could not persist permission for $uri", it) }
    }

    fun clearFolder() {
        viewModelScope.launch {
            backupPreferences.setFolderUri(null)
            BackupScheduler.cancel(getApplication())
            transient.update { it.copy(files = emptyList(), folderError = null) }
        }
    }

    // endregion

    // region Backups

    fun refreshFiles() {
        viewModelScope.launch {
            transient.update { it.copy(isLoadingFiles = true) }
            val settings = backupPreferences.currentSettings()
            val treeUri = settings.folderUri?.let { Uri.parse(it) }
            if (treeUri == null) {
                transient.update { it.copy(isLoadingFiles = false, files = emptyList()) }
                return@launch
            }
            runCatching { backupFileStore.listBackups(treeUri) }
                .onSuccess { files ->
                    transient.update { it.copy(isLoadingFiles = false, files = files, folderError = null) }
                }
                .onFailure { error ->
                    Log.w(TAG, "Could not list backups", error)
                    transient.update {
                        it.copy(
                            isLoadingFiles = false,
                            files = emptyList(),
                            folderError = error.message ?: "The backup folder is no longer available."
                        )
                    }
                }
        }
    }

    fun backupNow() {
        if (transient.value.isBackingUp) return
        viewModelScope.launch {
            transient.update { it.copy(isBackingUp = true) }
            val outcome = backupManager.performBackup(BackupTrigger.MANUAL)
            val message = when (outcome) {
                is BackupOutcome.Success -> "Backup saved (${formatBytes(outcome.file.sizeBytes)})."
                is BackupOutcome.Skipped -> outcome.reason
                is BackupOutcome.Failed -> outcome.message
            }
            transient.update { it.copy(isBackingUp = false, message = message) }
            refreshFiles()
        }
    }

    fun deleteBackup(file: BackupFile) {
        viewModelScope.launch {
            val deleted = backupFileStore.deleteBackup(file)
            transient.update {
                it.copy(
                    message = if (deleted) "Deleted ${file.name}." else "Could not delete ${file.name}."
                )
            }
            refreshFiles()
        }
    }

    /**
     * Applies [file] to the database. [RestoreMode.REPLACE] empties every table first, so
     * the UI must confirm that with the user before calling this.
     */
    fun restore(file: BackupFile, mode: RestoreMode) {
        if (transient.value.isRestoring) return
        viewModelScope.launch {
            transient.update { it.copy(isRestoring = true, restoringFileName = file.name) }
            val message = runCatching {
                val settings = backupPreferences.currentSettings()
                val treeUri = settings.folderUri?.let { Uri.parse(it) }
                    ?: error("No backup folder has been chosen.")
                val contents = backupFileStore.readBackup(file)
                val summary = BackupSerializer.restore(repository, contents, mode)
                // The database now matches a backup we already hold, so there is nothing
                // new to write out.
                backupPreferences.clearDirty()
                describeSummary(summary, mode)
            }.fold(
                onSuccess = { it },
                onFailure = { "Restore failed: ${it.message ?: "could not read the backup."}" }
            )
            transient.update { it.copy(isRestoring = false, restoringFileName = null, message = message) }
            refreshFiles()
        }
    }

    private fun describeSummary(summary: com.example.dndnotes.data.backup.RestoreSummary, mode: RestoreMode): String {
        val verb = if (mode == RestoreMode.REPLACE) "Restored" else "Recovered"
        val skipped = summary.skippedNotes + summary.skippedCategories
        val base = "$verb ${summary.notes} notes, ${summary.categories} categories and ${summary.campaigns} campaigns."
        return if (skipped > 0) "$base $skipped item(s) were skipped." else base
    }

    fun consumeMessage() {
        transient.update { it.copy(message = null) }
    }

    // endregion

    companion object {
        private const val TAG = "BackupViewModel"

        fun formatBytes(bytes: Long): String = when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
            else -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
        }

        /** Short label for a day-of-week constant, e.g. Calendar.SUNDAY -> "Sun". */
        fun dayLabel(dayOfWeek: Int): String = listOf(
            "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
        ).getOrElse(dayOfWeek - 1) { "?" }

        fun timeLabel(minuteOfDay: Int): String =
            String.format(Locale.getDefault(), "%02d:%02d", minuteOfDay / 60, minuteOfDay % 60)

        /** Best-effort timestamp for display when the provider reports none. */
        fun formatTimestamp(millis: Long): String =
            if (millis <= 0L) "unknown time"
            else SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
                .format(Date(millis))
    }
}