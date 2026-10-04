package com.example.dndnotes

import android.app.Application
import android.util.Log
import com.example.dndnotes.data.backup.BackupFileStore
import com.example.dndnotes.data.backup.BackupManager
import com.example.dndnotes.data.backup.BackupScheduler
import com.example.dndnotes.data.local.AppDatabase
import com.example.dndnotes.data.prefs.BackupPreferences
import com.example.dndnotes.data.prefs.ThemePreferences
import com.example.dndnotes.data.repository.DndRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class DndApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database by lazy { AppDatabase.getDatabase(this) }

    val themePreferences by lazy { ThemePreferences(this) }

    val backupPreferences by lazy { BackupPreferences(this) }

    /**
     * The repository is told about every write so the backup system knows there is
     * unsaved work. It is created lazily and needs [backupPreferences] to exist first.
     */
    val repository: DndRepository by lazy {
        DndRepository(database) { backupPreferences.markDirty() }
    }

    val backupFileStore by lazy { BackupFileStore(this) }

    val backupManager by lazy {
        BackupManager(this, repository, backupPreferences, backupFileStore)
    }

    override fun onCreate() {
        super.onCreate()

        // Keep the WorkManager schedule in step with the stored settings. On a fresh
        // install DataStore emits defaults immediately, so this also performs the
        // first-time schedule setup without a separate code path.
        applicationScope.launch {
            backupPreferences.settingsFlow
                .drop(1) // The default emission needs no scheduling; nothing exists yet.
                .distinctUntilChanged()
                .collect { settings ->
                    runCatching { BackupScheduler.schedule(this@DndApplication, settings) }
                        .onFailure { Log.e(TAG, "Could not schedule automatic backups", it) }
                }
        }

        // Safety net: catch unsaved work the moment the app is opened, rather than
        // waiting for the scheduled slot.
        applicationScope.launch {
            runCatching { backupManager.runStartupBackupIfNeeded() }
                .onFailure { Log.e(TAG, "Startup backup check failed", it) }
        }
    }

    /**
     * Called when the app leaves the foreground so the pending "there are unsaved changes"
     * flag reaches disk before the process can be killed.
     */
    fun onEnteredBackground() {
        applicationScope.launch {
            runCatching { backupPreferences.flushDirty() }
        }
    }

    private companion object {
        const val TAG = "DndApplication"
    }
}