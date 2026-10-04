package com.example.dndnotes.data.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.example.dndnotes.data.prefs.BackupPreferences
import com.example.dndnotes.data.prefs.BackupSettings
import com.example.dndnotes.data.repository.DndRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Why a backup ran. Manual runs ignore the enabled switch; automatic ones honour it. */
enum class BackupTrigger { SCHEDULED, APP_OPENED, FIRST_RUN, MANUAL }

/** Result of a backup attempt. */
sealed interface BackupOutcome {
    data class Success(val file: BackupFile, val pruned: Int) : BackupOutcome

    /** Not an error - there was simply nothing to do. */
    data class Skipped(val reason: String) : BackupOutcome

    data class Failed(val message: String, val cause: Throwable? = null) : BackupOutcome
}

/**
 * Writes a full snapshot of the database into the user's chosen folder and applies the
 * retention policy.
 *
 * Every entry point funnels through [performBackup]: the WorkManager worker, the "Back up
 * now" button and the automatic triggers all produce byte-identical files.
 */
class BackupManager(
    private val context: Context,
    private val repository: DndRepository,
    private val backupPreferences: BackupPreferences,
    private val fileStore: BackupFileStore
) {

    /**
     * @param onSuccess invoked with the summary when a file was written; used to refresh
     *   the backup list in the UI.
     */
    suspend fun performBackup(
        trigger: BackupTrigger = BackupTrigger.SCHEDULED,
        onSuccess: ((BackupFile) -> Unit)? = null
    ): BackupOutcome {
        val settings = backupPreferences.currentSettings()

        if (trigger != BackupTrigger.MANUAL && !settings.enabled) {
            return BackupOutcome.Skipped("Automatic backups are turned off.")
        }

        val treeUri = settings.folderUri?.let { Uri.parse(it) }
            ?: return BackupOutcome.Skipped("No backup folder has been chosen yet.")

        if (!fileStore.hasPersistedPermission(treeUri)) {
            return BackupOutcome.Failed(
                "Permission to the backup folder was lost. Please choose the folder again."
            )
        }

        return try {
            val json = BackupSerializer.buildBackupJson(repository)
            val fileName = BackupFileNames.newFileName(System.currentTimeMillis())
            val file = fileStore.writeBackup(treeUri, fileName, json)
            val pruned = fileStore.prune(treeUri, settings.keepCount)
            backupPreferences.recordBackup(file.name, file.sizeBytes)
            onSuccess?.invoke(file)
            Log.i(TAG, "Wrote ${file.name} (${file.sizeBytes} bytes) via $trigger, pruned $pruned")
            BackupOutcome.Success(file, pruned)
        } catch (e: BackupFolderUnavailableException) {
            Log.w(TAG, "Backup folder unavailable", e)
            BackupOutcome.Failed(e.message ?: "The backup folder is no longer available.", e)
        } catch (e: IOException) {
            Log.e(TAG, "Backup failed", e)
            BackupOutcome.Failed(e.message ?: "Could not write the backup file.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            BackupOutcome.Failed(e.message ?: "Unexpected error while backing up.", e)
        }
    }

    /**
     * Decides whether opening the app should trigger a backup, and does it if so.
     *
     * Two independent nudges:
     *  - **first run**: automatic backups are enabled with a folder but nothing has ever
     *    been written, so there is no safety net at all yet.
     *  - **changed since last backup**: something has been edited or deleted. This is what
     *    actually limits the damage from a bad event, because a weekly timer alone could
     *    lose a week of work.
     *
     * [minGapMillis] stops a large full-database export on every single app launch.
     */
    suspend fun runStartupBackupIfNeeded(
        nowMillis: Long = System.currentTimeMillis(),
        minGapMillis: Long = DEFAULT_MIN_APP_OPEN_GAP_MILLIS
    ): BackupOutcome {
        val settings = backupPreferences.currentSettings()
        if (!settings.enabled || settings.folderUri.isNullOrBlank()) {
            return BackupOutcome.Skipped("Automatic backups are not configured.")
        }

        val last = backupPreferences.lastBackupInfo()
        val trigger = when {
            !last.hasBackup -> BackupTrigger.FIRST_RUN
            backupPreferences.isDirty() && nowMillis - last.at >= minGapMillis -> BackupTrigger.APP_OPENED
            else -> return BackupOutcome.Skipped("No unsaved changes since the last backup.")
        }

        Log.i(TAG, "Triggering $trigger backup on app open")
        return performBackup(trigger)
    }

    suspend fun listBackups(settings: BackupSettings): Result<List<BackupFile>> = withContext(Dispatchers.IO) {
        val treeUri = settings.folderUri?.let { Uri.parse(it) }
            ?: return@withContext Result.success(emptyList())
        runCatching { fileStore.listBackups(treeUri) }
    }

    /**
     * Display name of the chosen folder for the settings row, falling back to the last
     * path segment of the document id, or null when the folder cannot be resolved.
     */
    fun describeFolder(settings: BackupSettings): String? {
        val treeUri = settings.folderUri?.let { Uri.parse(it) } ?: return null
        return runCatching {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            val queried = context.contentResolver.query(
                docUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
            queried?.takeIf { it.isNotBlank() } ?: docId.substringAfterLast(':').ifBlank { docId }
        }.getOrNull()
    }

    companion object {
        private const val TAG = "BackupManager"

        /**
         * Minimum gap between app-open-triggered backups. Opening the app several times in
         * an afternoon should not produce several multi-megabyte JSON files.
         */
        const val DEFAULT_MIN_APP_OPEN_GAP_MILLIS = 6L * 60 * 60 * 1000
    }
}