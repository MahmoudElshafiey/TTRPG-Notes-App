package com.example.dndnotes.data.backup

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.dndnotes.data.prefs.BackupFrequency
import com.example.dndnotes.data.prefs.BackupSettings
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Owns the WorkManager schedule for automatic backups.
 */
object BackupScheduler {

    const val PERIODIC_WORK_NAME = "dndnotes_automatic_backup"
    const val ONE_SHOT_WORK_NAME = "dndnotes_automatic_backup_now"

    /**
     * (Re)schedules the periodic backup to match [settings].
     *
     * `UPDATE` keeps the existing work chain but adopts the new interval and initial
     * delay, which means every change to the schedule restarts the countdown. There is no
     * way around that with WorkManager, so the UI should present the schedule as
     * "roughly every X" rather than as an exact appointment.
     */
    fun schedule(context: Context, settings: BackupSettings) {
        val workManager = WorkManager.getInstance(context)
        if (!settings.enabled) {
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            return
        }

        val intervalDays = when (settings.frequency) {
            BackupFrequency.DAILY -> 1L
            BackupFrequency.WEEKLY -> 7L
        }

        val request = PeriodicWorkRequestBuilder<BackupWorker>(intervalDays, TimeUnit.DAYS)
            .setInitialDelay(computeInitialDelayMillis(settings, System.currentTimeMillis()), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.NONE)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .addTag(PERIODIC_WORK_NAME)
            .build()

        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    /**
     * Queues a single backup as soon as WorkManager gets to it. Used by the "Back up now"
     * button and by the on-app-open and first-run triggers.
     *
     * [existingWorkPolicy] decides what happens if a run is already queued, so a burst of
     * triggers cannot stack up duplicate full-database exports.
     */
    fun runNow(
        context: Context,
        existingWorkPolicy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP
    ) {
        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(Constraints.NONE)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .addTag(PERIODIC_WORK_NAME)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(ONE_SHOT_WORK_NAME, existingWorkPolicy, request)
    }

    /**
     * Milliseconds from [nowMillis] until the next configured backup slot.
     *
     * Pure and side-effect free so the schedule arithmetic can be unit tested without
     * Android. Uses [Calendar] rather than `java.time` to stay compatible with API 24.
     *
     * Always returns a positive delay: a slot that has already passed today rolls forward
     * to tomorrow (daily) or next week (weekly).
     */
    fun computeInitialDelayMillis(settings: BackupSettings, nowMillis: Long): Long {
        val slot = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, settings.hour)
            set(Calendar.MINUTE, settings.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        when (settings.frequency) {
            BackupFrequency.DAILY -> {
                if (slot.timeInMillis <= nowMillis) slot.add(Calendar.DAY_OF_YEAR, 1)
            }
            BackupFrequency.WEEKLY -> {
                // Calendar.SUNDAY == 1 ... Calendar.SATURDAY == 7
                val daysAhead = (settings.dayOfWeek - slot.get(Calendar.DAY_OF_WEEK) + 7) % 7
                slot.add(Calendar.DAY_OF_YEAR, daysAhead)
                if (slot.timeInMillis <= nowMillis) slot.add(Calendar.DAY_OF_YEAR, 7)
            }
        }

        return (slot.timeInMillis - nowMillis).coerceAtLeast(1_000L)
    }
}