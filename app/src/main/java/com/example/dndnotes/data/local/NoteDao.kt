package com.example.dndnotes.data.local

import androidx.room.*
import com.example.dndnotes.data.model.Note
import com.example.dndnotes.data.model.NoteSummary
import com.example.dndnotes.data.model.ImageAttachment
import com.example.dndnotes.data.model.ConsumableItem
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT id, categoryId, title, body, created, (drawing IS NOT NULL AND drawing != '') as hasDrawing FROM notes WHERE categoryId = :categoryId ORDER BY created DESC")
    fun getNoteSummariesByCategory(categoryId: Long): Flow<List<NoteSummary>>

    @Query("SELECT * FROM notes WHERE categoryId = :categoryId ORDER BY created DESC")
    fun getNotesByCategory(categoryId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteByIdRaw(noteId: Long): Note?

    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun getNoteById(noteId: Long): Flow<Note?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)

    @Query("SELECT * FROM images WHERE noteId = :noteId ORDER BY orderIndex ASC")
    fun getImagesForNote(noteId: Long): Flow<List<ImageAttachment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: ImageAttachment): Long

    @Delete
    suspend fun deleteImage(image: ImageAttachment)

    @Query("SELECT * FROM consumables WHERE noteId = :noteId ORDER BY orderIndex ASC")
    fun getConsumablesForNote(noteId: Long): Flow<List<ConsumableItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsumable(consumable: ConsumableItem): Long

    @Update
    suspend fun updateConsumable(consumable: ConsumableItem)

    @Delete
    suspend fun deleteConsumable(consumable: ConsumableItem)
    
    @Query("SELECT * FROM notes")
    suspend fun getAllNotes(): List<Note>

    @Query("SELECT * FROM images")
    suspend fun getAllImages(): List<ImageAttachment>

    @Query("SELECT * FROM consumables")
    suspend fun getAllConsumables(): List<ConsumableItem>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%'")
    fun searchNotes(query: String): Flow<List<Note>>

    // Delete helpers, used by the REPLACE-mode restore.

    @Query("DELETE FROM images")
    suspend fun deleteAllImages()

    @Query("DELETE FROM consumables")
    suspend fun deleteAllConsumables()

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    /**
     * Counts the notes a category deletion would take with it. Deleting a category
     * cascades to its subcategories and every note inside them, so the confirmation
     * dialog needs the real number to warn the user.
     */
    @Query(
        """
        WITH RECURSIVE subtree(id) AS (
            SELECT id FROM categories WHERE id = :categoryId
            UNION ALL
            SELECT c.id FROM categories c INNER JOIN subtree s ON c.parentId = s.id
        )
        SELECT COUNT(*) FROM notes WHERE categoryId IN (SELECT id FROM subtree)
        """
    )
    suspend fun getSubtreeNoteCount(categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM notes WHERE categoryId = :categoryId")
    suspend fun getNoteCountForCategory(categoryId: Long): Int
}
