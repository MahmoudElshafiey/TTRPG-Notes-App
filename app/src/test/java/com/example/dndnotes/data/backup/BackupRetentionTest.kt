package com.example.dndnotes.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Retention policy and file naming.
 *
 * These two rules together are what stop the backup folder from growing forever, so they
 * are pinned down independently of any Android API.
 */
class BackupRetentionTest {

    private fun names(count: Int) = (1..count)
        .map { BackupFileNames.newFileName(1_700_000_000_000L + it * 60_000L) }
        // Callers sort newest-first before pruning.
        .sortedDescending()

    @Test
    fun `keeping twelve of twenty deletes the eight oldest`() {
        val all = names(20)
        val doomed = selectNamesToDelete(all, keepCount = 12)

        assertEquals(8, doomed.size)
        assertEquals(all.takeLast(8), doomed)
        // The 12 newest are exactly what survives.
        assertEquals(all.take(12), all.filterNot { it in doomed })
    }

    @Test
    fun `nothing is deleted when the folder is at or under the limit`() {
        assertTrue(selectNamesToDelete(names(12), keepCount = 12).isEmpty())
        assertTrue(selectNamesToDelete(names(3), keepCount = 12).isEmpty())
    }

    @Test
    fun `a single file over the limit is deleted`() {
        assertEquals(1, selectNamesToDelete(names(13), keepCount = 12).size)
    }

    @Test
    fun `keep count of zero would delete everything, so callers must clamp`() {
        // Documents the guard: BackupSettings.KEEP_COUNT_RANGE is the real protection,
        // this is the backstop.
        assertEquals(5, selectNamesToDelete(names(5), keepCount = 0).size)
        assertEquals(5, selectNamesToDelete(names(5), keepCount = -1).size)
    }

    @Test
    fun `file names sort chronologically as plain strings`() {
        // This is the property that lets listBackups order by name instead of trusting
        // lastModified, which several cloud providers report as 0.
        val chronological = listOf(
            BackupFileNames.newFileName(1_700_000_000_000L),
            BackupFileNames.newFileName(1_700_003_600_000L),
            BackupFileNames.newFileName(1_700_007_200_000L)
        )
        assertEquals(chronological, chronological.sorted())
    }

    @Test
    fun `file names carry the prefix and json extension`() {
        val name = BackupFileNames.newFileName(1_767_225_600_000L)
        assertTrue(name, name.startsWith(BackupFileNames.PREFIX))
        assertTrue(name, name.endsWith(BackupFileNames.EXTENSION))
        assertTrue(BackupFileNames.isBackupFile(name))
    }

    @Test
    fun `unrelated files in the folder are ignored`() {
        assertFalse(BackupFileNames.isBackupFile("notes.json"))
        assertFalse(BackupFileNames.isBackupFile("dnd_notes_backup_.json"))
        assertFalse(BackupFileNames.isBackupFile("dnd_notes_backup_20261004_030000.txt"))
        assertFalse(BackupFileNames.isBackupFile(null))
    }
}