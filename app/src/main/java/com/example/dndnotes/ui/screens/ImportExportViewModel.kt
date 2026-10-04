package com.example.dndnotes.ui.screens

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dndnotes.data.backup.BackupSerializer
import com.example.dndnotes.data.backup.RestoreMode
import com.example.dndnotes.data.repository.DndRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Backs the manual export/import buttons.
 *
 * Only deals in user-picked [Uri]s; the JSON format itself lives in [BackupSerializer] so
 * these files stay interchangeable with the automatic backups.
 */
class ImportExportViewModel(private val repository: DndRepository) : ViewModel() {

    /**
     * Writes a full snapshot to [uri]. [prettyPrint] is on so a hand-made backup stays
     * readable and diffable.
     */
    fun exportAllData(contentResolver: ContentResolver, uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val jsonString = BackupSerializer.buildBackupJson(repository)
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                    } ?: error("Could not open the selected file for writing.")
                }
            }
            result.onFailure { it.printStackTrace() }
            onComplete(result.isSuccess)
        }
    }

    /**
     * Imports [uri] as a [RestoreMode.MERGE]: existing notes are kept and the backup's
     * contents are added alongside them under fresh ids.
     */
    fun importAllData(contentResolver: ContentResolver, uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val contents = contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().use { it.readText() }
                    } ?: error("Could not open the selected file.")
                    BackupSerializer.restore(repository, contents, RestoreMode.MERGE)
                }
            }
            result.onFailure { it.printStackTrace() }
            onComplete(result.isSuccess)
        }
    }
}