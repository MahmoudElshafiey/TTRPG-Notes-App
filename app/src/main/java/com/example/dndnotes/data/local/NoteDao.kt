package com.example.dndnotes.data.local

import androidx.room.*
import com.example.dndnotes.data.model.Note
import com.example.dndnotes.data.model.ImageAttachment
import com.example.dndnotes.data.model.ConsumableItem
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE categoryId = :categoryId ORDER BY created DESC")
    fun getNotesByCategory(categoryId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun getNoteById(noteId: Long): Flow<Note?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

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
}
