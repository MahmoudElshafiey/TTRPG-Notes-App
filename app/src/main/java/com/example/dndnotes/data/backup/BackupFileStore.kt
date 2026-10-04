package com.example.dndnotes.data.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Naming rules for backup files.
 *
 * The timestamp format is fixed-width and lexicographically sortable, which lets
 * [selectBackupsToDelete] order backups by name. That matters because not every
 * documents provider reports a usable `lastModified` (some cloud providers return 0).
 */
object BackupFileNames {
    const val PREFIX = "dnd_notes_backup_"
    const val EXTENSION = ".json"

    /** e.g. `dnd_notes_backup_20261004_030000.json` */
    fun newFileName(timestampMillis: Long): String {
        val format = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
        return PREFIX + format.format(java.util.Date(timestampMillis)) + EXTENSION
    }

    fun isBackupFile(name: String?): Boolean =
        name != null && name.startsWith(PREFIX) && name.endsWith(EXTENSION) && name.length > PREFIX.length + EXTENSION.length
}

/** A single backup file sitting in the user's chosen folder. */
data class BackupFile(
    val documentUri: Uri,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long
)

/** Raised when the chosen folder can no longer be used, e.g. the SD card was removed. */
class BackupFolderUnavailableException(message: String, cause: Throwable? = null) :
    IOException(message, cause)

/**
 * Reads and writes backup files in a user-selected folder through the Storage Access
 * Framework.
 *
 * SAF needs no runtime permission on any API level the app supports (minSdk 24); the
 * grant taken when the folder is picked persists across reboots. This class deliberately
 * uses the framework's [DocumentsContract] rather than `DocumentFile` to avoid pulling in
 * another dependency.
 */
class BackupFileStore(private val context: Context) {

    private val resolver get() = context.contentResolver

    /** True when we still hold a persisted read+write grant on the stored tree URI. */
    fun hasPersistedPermission(treeUri: Uri): Boolean =
        runCatching {
            resolver.persistedUriPermissions.any { it.uri == treeUri && it.isWritePermission }
        }.getOrDefault(false)

    suspend fun listBackups(treeUri: Uri): List<BackupFile> = withContext(Dispatchers.IO) {
        val childrenUri = childrenUriOf(treeUri)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        val found = mutableListOf<BackupFile>()
        try {
            resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                if (idIndex < 0 || nameIndex < 0) return@use

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex)
                    if (!BackupFileNames.isBackupFile(name)) continue
                    val docId = cursor.getString(idIndex) ?: continue
                    found += BackupFile(
                        documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId),
                        name = name,
                        sizeBytes = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L,
                        lastModified = if (modifiedIndex >= 0) cursor.getLong(modifiedIndex) else 0L
                    )
                }
            }
        } catch (e: Exception) {
            throw asUnavailable(e)
        }

        // Newest first, by name - see BackupFileNames for why not lastModified.
        found.sortedByDescending { it.name }
    }

    suspend fun readBackup(file: BackupFile): String = withContext(Dispatchers.IO) {
        try {
            resolver.openInputStream(file.documentUri)?.use { stream ->
                stream.bufferedReader().use { it.readText() }
            } ?: throw BackupFolderUnavailableException("Could not open ${file.name} for reading.")
        } catch (e: BackupFolderUnavailableException) {
            throw e
        } catch (e: Exception) {
            throw asUnavailable(e)
        }
    }

    suspend fun writeBackup(treeUri: Uri, fileName: String, contents: String): BackupFile =
        withContext(Dispatchers.IO) {
            try {
                val parentUri = DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri)
                )
                val document = DocumentsContract.createDocument(
                    resolver,
                    parentUri,
                    "application/json",
                    fileName
                ) ?: throw IOException("Could not create $fileName in the backup folder.")

                resolver.openOutputStream(document)?.use { stream ->
                    stream.write(contents.toByteArray(Charsets.UTF_8))
                    stream.flush()
                } ?: throw IOException("Could not open $fileName for writing.")

                // Prefer the size the provider reports; some do not fill it in on create.
                val reported = runCatching {
                    resolver.openAssetFileDescriptor(document, "r")?.use { it.length }
                }.getOrNull()
                val size = reported?.takeIf { it > 0 } ?: contents.toByteArray(Charsets.UTF_8).size.toLong()

                BackupFile(document, fileName, size, System.currentTimeMillis())
            } catch (e: SecurityException) {
                throw BackupFolderUnavailableException(
                    "Permission to the backup folder was lost. Please choose the folder again.",
                    e
                )
            } catch (e: BackupFolderUnavailableException) {
                throw e
            } catch (e: Exception) {
                throw asUnavailable(e)
            }
        }

    suspend fun deleteBackup(file: BackupFile): Boolean = withContext(Dispatchers.IO) {
        try {
            DocumentsContract.deleteDocument(resolver, file.documentUri)
        } catch (e: Exception) {
            Log.w(TAG, "Could not delete ${file.name}", e)
            false
        }
    }

    /**
     * Applies the retention policy, deleting everything beyond the newest [keepCount]
     * files. Returns the number of files removed.
     */
    suspend fun prune(treeUri: Uri, keepCount: Int): Int = withContext(Dispatchers.IO) {
        val all = listBackups(treeUri)
        val excess = selectBackupsToDelete(all, keepCount)
        var deleted = 0
        excess.forEach { file ->
            if (deleteBackup(file)) deleted++
        }
        if (deleted > 0) Log.i(TAG, "Pruned $deleted backup(s), keeping $keepCount")
        deleted
    }

    private fun childrenUriOf(treeUri: Uri): Uri = try {
        DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
    } catch (e: Exception) {
        throw asUnavailable(e)
    }

    private fun asUnavailable(e: Exception) =
        BackupFolderUnavailableException(
            "The backup folder is no longer available. Please choose the folder again.",
            e
        )

    private companion object {
        const val TAG = "BackupFileStore"
    }
}

/**
 * Pure helpers for the retention policy.
 *
 * Deliberately expressed over indices and names rather than [BackupFile] so the rule can
 * be unit tested on the JVM without constructing `android.net.Uri` stubs.
 */

/** Files to delete, given backups already sorted newest-first. */
internal fun selectBackupsToDelete(all: List<BackupFile>, keepCount: Int): List<BackupFile> =
    selectIndicesToDelete(all.size, keepCount).map { all[it] }

/** Names to delete, given backup names already sorted newest-first. */
internal fun selectNamesToDelete(namesNewestFirst: List<String>, keepCount: Int): List<String> =
    selectIndicesToDelete(namesNewestFirst.size, keepCount).map { namesNewestFirst[it] }

private fun selectIndicesToDelete(size: Int, keepCount: Int): List<Int> = when {
    keepCount <= 0 -> List(size) { it }
    size <= keepCount -> emptyList()
    else -> (keepCount until size).toList()
}