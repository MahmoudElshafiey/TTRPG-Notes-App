package com.example.dndnotes.data.repository

import com.example.dndnotes.data.local.CampaignDao
import com.example.dndnotes.data.local.CategoryDao
import com.example.dndnotes.data.local.NoteDao
import com.example.dndnotes.data.model.Campaign
import com.example.dndnotes.data.model.Category
import com.example.dndnotes.data.model.Note
import com.example.dndnotes.data.model.NoteSummary
import com.example.dndnotes.data.model.ImageAttachment
import com.example.dndnotes.data.model.ConsumableItem
import kotlinx.coroutines.flow.Flow

class DndRepository(
    private val campaignDao: CampaignDao,
    private val categoryDao: CategoryDao,
    private val noteDao: NoteDao
) {
    // Campaigns
    val allCampaigns: Flow<List<Campaign>> = campaignDao.getAllCampaigns()
    suspend fun getAllCampaignsRaw() = campaignDao.getAllCampaignsRaw()
    suspend fun getCampaignById(id: Long) = campaignDao.getCampaignById(id)
    suspend fun insertCampaign(campaign: Campaign) = campaignDao.insertCampaign(campaign)
    suspend fun updateCampaign(campaign: Campaign) = campaignDao.updateCampaign(campaign)
    suspend fun deleteCampaign(campaign: Campaign) = campaignDao.deleteCampaign(campaign)

    // Categories
    fun getAllCategories(campaignId: Long): Flow<List<Category>> = categoryDao.getAllCategories(campaignId)
    suspend fun getAllCategoriesRaw() = categoryDao.getAllCategoriesRaw()
    fun getRootCategories(campaignId: Long): Flow<List<Category>> = categoryDao.getRootCategories(campaignId)
    fun getSubcategories(parentId: Long) = categoryDao.getSubcategories(parentId)
    suspend fun insertCategory(category: Category) = categoryDao.insertCategory(category)
    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)
    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    // Notes
    fun getNotesByCategory(categoryId: Long) = noteDao.getNotesByCategory(categoryId)
    fun getNoteSummariesByCategory(categoryId: Long) = noteDao.getNoteSummariesByCategory(categoryId)
    suspend fun getAllNotesRaw() = noteDao.getAllNotes()
    fun getNoteById(noteId: Long) = noteDao.getNoteById(noteId)
    suspend fun getNoteByIdRaw(noteId: Long) = noteDao.getNoteByIdRaw(noteId)
    suspend fun insertNote(note: Note) = noteDao.insertNote(note)
    suspend fun updateNote(note: Note) = noteDao.updateNote(note)
    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)
    suspend fun deleteNoteById(noteId: Long) = noteDao.deleteNoteById(noteId)
    fun searchNotes(query: String) = noteDao.searchNotes(query)

    // Images
    fun getImagesForNote(noteId: Long) = noteDao.getImagesForNote(noteId)
    suspend fun getAllImagesRaw() = noteDao.getAllImages()
    suspend fun insertImage(image: ImageAttachment) = noteDao.insertImage(image)
    suspend fun deleteImage(image: ImageAttachment) = noteDao.deleteImage(image)

    // Consumables
    fun getConsumablesForNote(noteId: Long) = noteDao.getConsumablesForNote(noteId)
    suspend fun getAllConsumablesRaw() = noteDao.getAllConsumables()
    suspend fun insertConsumable(consumable: ConsumableItem) = noteDao.insertConsumable(consumable)
    suspend fun updateConsumable(consumable: ConsumableItem) = noteDao.updateConsumable(consumable)
    suspend fun deleteConsumable(consumable: ConsumableItem) = noteDao.deleteConsumable(consumable)
}
