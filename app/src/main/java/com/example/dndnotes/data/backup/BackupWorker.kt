package com.example.dndnotes.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dndnotes.DndApplication

/**
 * Runs a full automatic backup.
 *
 * WorkManager survives reboots and process death, so this is the mechanism that makes
 * backups happen without the app ever being opened. Failures retry with exponential
 * backoff rather than silently giving up, because a backup that quietly stops is worse
 * than one that keeps trying.
 */
class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? DndApplication
            ?: return Result.failure()

        return when (val outcome = app.backupManager.performBackup(BackupTrigger.SCHEDULED)) {
            is BackupOutcome.Success -> Result.success()
            // Nothing to do is a normal outcome, not a failure.
            is BackupOutcome.Skipped -> Result.success()
            is BackupOutcome.Failed -> {
                // A missing/revoked folder will not fix itself; retrying forever would
                // burn battery and never succeed.
                val permanent = app.backupPreferences.currentSettings().folderUri.isNullOrBlank()
                if (permanent || runAttemptCount >= MAX_ATTEMPTS) {
                    Result.failure()
                } else {
                    Result.retry()
                }
            }
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}