package com.example.dndnotes.data.repository

import androidx.room.withTransaction
import com.example.dndnotes.data.local.AppDatabase
import com.example.dndnotes.data.model.Campaign
import com.example.dndnotes.data.model.Category
import com.example.dndnotes.data.model.Note
import com.example.dndnotes.data.model.NoteSummary
import com.example.dndnotes.data.model.ImageAttachment
import com.example.dndnotes.data.model.ConsumableItem
import kotlinx.coroutines.flow.Flow

class DndRepository(
    private val database: AppDatabase,
    /**
     * Invoked after every mutation so listeners (currently the automatic backup system)
     * can tell that there is unsaved work worth writing out. Kept as a lambda so the
     * data layer does not depend on the preference store.
     */
    private val onDataChanged: () -> Unit = {}
) {
    private val campaignDao = database.campaignDao()
    private val categoryDao = database.categoryDao()
    private val noteDao = database.noteDao()

    /**
     * Runs [block] in a single database transaction, rolling back if it throws.
     * Used by restore so a partial import can never be committed.
     */
    suspend fun <T> inTransaction(block: suspend () -> T): T =
        database.withTransaction { block() }

    // Campaigns
    val allCampaigns: Flow<List<Campaign>> = campaignDao.getAllCampaigns()
    suspend fun getAllCampaignsRaw() = campaignDao.getAllCampaignsRaw()
    suspend fun getCampaignById(id: Long) = campaignDao.getCampaignById(id)
    suspend fun insertCampaign(campaign: Campaign) = campaignDao.insertCampaign(campaign).also { onDataChanged() }
    suspend fun updateCampaign(campaign: Campaign) = campaignDao.updateCampaign(campaign).also { onDataChanged() }
    suspend fun deleteCampaign(campaign: Campaign) = campaignDao.deleteCampaign(campaign).also { onDataChanged() }

    // Categories
    fun getAllCategories(campaignId: Long): Flow<List<Category>> = categoryDao.getAllCategories(campaignId)
    suspend fun getAllCategoriesRaw() = categoryDao.getAllCategoriesRaw()
    fun getRootCategories(campaignId: Long): Flow<List<Category>> = categoryDao.getRootCategories(campaignId)
    fun getSubcategories(parentId: Long) = categoryDao.getSubcategories(parentId)
    suspend fun insertCategory(category: Category) = categoryDao.insertCategory(category).also { onDataChanged() }
    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category).also { onDataChanged() }
    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category).also { onDataChanged() }
    suspend fun getNoteCountForCategory(categoryId: Long) = noteDao.getNoteCountForCategory(categoryId)
    suspend fun getSubtreeNoteCount(categoryId: Long) = noteDao.getSubtreeNoteCount(categoryId)

    // Notes
    fun getNotesByCategory(categoryId: Long): Flow<List<Note>> = noteDao.getNotesByCategory(categoryId)
    fun getNoteSummariesByCategory(categoryId: Long): Flow<List<NoteSummary>> = noteDao.getNoteSummariesByCategory(categoryId)
    suspend fun getAllNotesRaw() = noteDao.getAllNotes()
    suspend fun getNoteByIdRaw(noteId: Long) = noteDao.getNoteByIdRaw(noteId)
    suspend fun insertNote(note: Note) = noteDao.insertNote(note).also { onDataChanged() }
    suspend fun updateNote(note: Note) = noteDao.updateNote(note).also { onDataChanged() }
    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note).also { onDataChanged() }
    suspend fun deleteNoteById(noteId: Long) = noteDao.deleteNoteById(noteId).also { onDataChanged() }
    fun searchNotes(query: String) = noteDao.searchNotes(query)

    // Images
    fun getImagesForNote(noteId: Long): Flow<List<ImageAttachment>> = noteDao.getImagesForNote(noteId)
    suspend fun getAllImagesRaw() = noteDao.getAllImages()
    suspend fun insertImage(image: ImageAttachment) = noteDao.insertImage(image).also { onDataChanged() }
    suspend fun deleteImage(image: ImageAttachment) = noteDao.deleteImage(image).also { onDataChanged() }

    // Consumables
    fun getConsumablesForNote(noteId: Long): Flow<List<ConsumableItem>> = noteDao.getConsumablesForNote(noteId)
    suspend fun getAllConsumablesRaw() = noteDao.getAllConsumables()
    suspend fun insertConsumable(consumable: ConsumableItem) = noteDao.insertConsumable(consumable).also { onDataChanged() }
    suspend fun updateConsumable(consumable: ConsumableItem) = noteDao.updateConsumable(consumable).also { onDataChanged() }
    suspend fun deleteConsumable(consumable: ConsumableItem) = noteDao.deleteConsumable(consumable).also { onDataChanged() }

    /**
     * Empties every table. Only ever called as the first step of a REPLACE restore,
     * inside the same transaction as the import that follows it.
     *
     * Children are deleted before parents even though foreign keys already cascade,
     * so this stays correct if the cascade rules are ever relaxed.
     */
    suspend fun clearAll() {
        noteDao.deleteAllImages()
        noteDao.deleteAllConsumables()
        noteDao.deleteAllNotes()
        categoryDao.deleteAllCategories()
        campaignDao.deleteAllCampaigns()
    }
}