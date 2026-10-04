package com.example.dndnotes.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dndnotes.data.backup.BackupFileStore
import com.example.dndnotes.data.backup.BackupManager
import com.example.dndnotes.data.prefs.BackupPreferences
import com.example.dndnotes.data.prefs.ThemePreferences
import com.example.dndnotes.data.repository.DndRepository
import com.example.dndnotes.ui.screens.BackupViewModel
import com.example.dndnotes.ui.screens.CampaignViewModel
import com.example.dndnotes.ui.screens.CategoryViewModel
import com.example.dndnotes.ui.screens.ConsumablesViewModel
import com.example.dndnotes.ui.screens.ImagesViewModel
import com.example.dndnotes.ui.screens.ImportExportViewModel
import com.example.dndnotes.ui.screens.NoteViewModel
import com.example.dndnotes.ui.theme.ThemeViewModel

class ViewModelFactory(
    private val application: Application,
    private val repository: DndRepository,
    private val themePreferences: ThemePreferences,
    private val backupPreferences: BackupPreferences,
    private val backupManager: BackupManager,
    private val backupFileStore: BackupFileStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ImportExportViewModel::class.java) -> {
                ImportExportViewModel(repository) as T
            }
            modelClass.isAssignableFrom(CampaignViewModel::class.java) -> {
                CampaignViewModel(repository) as T
            }
            modelClass.isAssignableFrom(CategoryViewModel::class.java) -> {
                CategoryViewModel(repository) as T
            }
            modelClass.isAssignableFrom(NoteViewModel::class.java) -> {
                NoteViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ConsumablesViewModel::class.java) -> {
                ConsumablesViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ImagesViewModel::class.java) -> {
                ImagesViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ThemeViewModel::class.java) -> {
                ThemeViewModel(themePreferences) as T
            }
            modelClass.isAssignableFrom(BackupViewModel::class.java) -> {
                BackupViewModel(
                    application = application,
                    repository = repository,
                    backupPreferences = backupPreferences,
                    backupManager = backupManager,
                    backupFileStore = backupFileStore
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}