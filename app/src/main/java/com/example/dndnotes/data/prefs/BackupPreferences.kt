package com.example.dndnotes.data.prefs

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar

/** How often the automatic backup should run. */
enum class BackupFrequency { DAILY, WEEKLY }

/**
 * User-configurable automatic backup settings.
 *
 * [minuteOfDay] and [dayOfWeek] follow the [Calendar] constants. `java.util.Calendar` is
 * used rather than `java.time` because the app supports API 24, where `java.time` is
 * unavailable without library desugaring.
 */
data class BackupSettings(
    val enabled: Boolean = true,
    val frequency: BackupFrequency = BackupFrequency.WEEKLY,
    val minuteOfDay: Int = DEFAULT_MINUTE_OF_DAY,
    val dayOfWeek: Int = DEFAULT_DAY_OF_WEEK,
    val keepCount: Int = DEFAULT_KEEP_COUNT,
    val folderUri: String? = null
) {
    val hour: Int get() = minuteOfDay / 60
    val minute: Int get() = minuteOfDay % 60

    companion object {
        /** 03:00 - late enough that it will not interrupt a session. */
        const val DEFAULT_MINUTE_OF_DAY = 3 * 60
        const val DEFAULT_DAY_OF_WEEK = Calendar.SUNDAY
        const val DEFAULT_KEEP_COUNT = 12
        val KEEP_COUNT_RANGE = 3..50
    }
}

class BackupPreferences(private val context: Context) {

    private val enabledKey = booleanPreferencesKey("backup_enabled")
    private val frequencyKey = stringPreferencesKey("backup_frequency")
    private val minuteOfDayKey = intPreferencesKey("backup_minute_of_day")
    private val dayOfWeekKey = intPreferencesKey("backup_day_of_week")
    private val keepCountKey = intPreferencesKey("backup_keep_count")
    private val folderUriKey = stringPreferencesKey("backup_folder_uri")
    private val lastBackupAtKey = longPreferencesKey("backup_last_at")
    private val lastBackupNameKey = stringPreferencesKey("backup_last_name")
    private val lastBackupSizeKey = longPreferencesKey("backup_last_size")
    private val dirtyKey = booleanPreferencesKey("backup_dirty")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var flushJob: Job? = null

    val settingsFlow: Flow<BackupSettings> = context.settingsDataStore.data.map { prefs ->
        BackupSettings(
            enabled = prefs[enabledKey] ?: true,
            frequency = prefs[frequencyKey]?.let { name ->
                runCatching { BackupFrequency.valueOf(name) }.getOrNull()
            } ?: BackupFrequency.WEEKLY,
            minuteOfDay = prefs[minuteOfDayKey] ?: BackupSettings.DEFAULT_MINUTE_OF_DAY,
            dayOfWeek = prefs[dayOfWeekKey] ?: BackupSettings.DEFAULT_DAY_OF_WEEK,
            keepCount = (prefs[keepCountKey] ?: BackupSettings.DEFAULT_KEEP_COUNT)
                .coerceIn(BackupSettings.KEEP_COUNT_RANGE),
            folderUri = prefs[folderUriKey]
        )
    }

    val lastBackupAtFlow: Flow<Long> = context.settingsDataStore.data.map { it[lastBackupAtKey] ?: 0L }

    val lastBackupInfoFlow: Flow<LastBackupInfo> = context.settingsDataStore.data.map { prefs ->
        LastBackupInfo(
            at = prefs[lastBackupAtKey] ?: 0L,
            fileName = prefs[lastBackupNameKey],
            sizeBytes = prefs[lastBackupSizeKey] ?: 0L
        )
    }

    suspend fun currentSettings(): BackupSettings = settingsFlow.first()

    suspend fun lastBackupInfo(): LastBackupInfo = lastBackupInfoFlow.first()

    suspend fun setEnabled(enabled: Boolean) = put { it[enabledKey] = enabled }

    suspend fun setFrequency(frequency: BackupFrequency) = put { it[frequencyKey] = frequency.name }

    suspend fun setTime(hour: Int, minute: Int) =
        put { it[minuteOfDayKey] = hour.coerceIn(0, 23) * 60 + minute.coerceIn(0, 59) }

    suspend fun setDayOfWeek(dayOfWeek: Int) = put { it[dayOfWeekKey] = dayOfWeek.coerceIn(1, 7) }

    suspend fun setKeepCount(count: Int) =
        put { it[keepCountKey] = count.coerceIn(BackupSettings.KEEP_COUNT_RANGE) }

    /** Stores the tree URI and takes a durable grant so backups keep working after reboot. */
    suspend fun setFolderUri(uri: String?) {
        context.contentResolver.takePersistableUriPermissionSafely(uri)
        put { prefs ->
            if (uri == null) prefs.remove(folderUriKey) else prefs[folderUriKey] = uri
        }
    }

    suspend fun recordBackup(fileName: String, sizeBytes: Long) = put { prefs ->
        prefs[lastBackupAtKey] = System.currentTimeMillis()
        prefs[lastBackupNameKey] = fileName
        prefs[lastBackupSizeKey] = sizeBytes
        prefs[dirtyKey] = false
    }

    /**
     * Flags that there are changes not yet covered by a backup.
     *
     * Deliberately non-suspending and cheap: it is called on every database write, and
     * the editor debounces writes while the user is actively typing. Only the flag flip
     * is synchronous; the DataStore write is coalesced a couple of seconds later.
     */
    fun markDirty() {
        flushJob?.cancel()
        flushJob = scope.launch {
            delay(DIRTY_FLUSH_DELAY_MS)
            put { it[dirtyKey] = true }
        }
    }

    /** Forces the pending dirty flag to disk. Called when the app leaves the foreground. */
    suspend fun flushDirty() {
        flushJob?.cancel()
        put { it[dirtyKey] = true }
    }

    suspend fun isDirty(): Boolean = context.settingsDataStore.data.first()[dirtyKey] ?: false

    suspend fun clearDirty() = put { it[dirtyKey] = false }

    private suspend fun put(block: (MutablePreferences) -> Unit) {
        context.settingsDataStore.edit(block)
    }

    private companion object {
        /** Long enough to coalesce a burst of edits, short enough to survive a quick trip to the background. */
        const val DIRTY_FLUSH_DELAY_MS = 2_000L
    }
}

data class LastBackupInfo(
    val at: Long,
    val fileName: String?,
    val sizeBytes: Long
) {
    val hasBackup: Boolean get() = at > 0L
}

/**
 * Takes a persistable read+write grant on a picked tree URI.
 *
 * Providers are allowed to grant only what they support, so a failure here is logged and
 * ignored rather than thrown - the backup write itself will surface a real permission
 * problem later.
 */
private fun ContentResolver.takePersistableUriPermissionSafely(uriString: String?) {
    if (uriString.isNullOrBlank()) return
    val uri = Uri.parse(uriString)
    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    try {
        takePersistableUriPermission(uri, flags)
    } catch (e: SecurityException) {
        Log.w("BackupPreferences", "Could not persist permission for $uri", e)
    }
}