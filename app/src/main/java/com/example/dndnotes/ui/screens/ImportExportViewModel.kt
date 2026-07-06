package com.example.dndnotes.ui.screens

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dndnotes.data.model.*
import com.example.dndnotes.data.repository.DndRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class UniversalBackupModel(
    val campaigns: List<CampaignModel>,
    val categories: List<CategoryModel>,
    val notes: List<NoteModel>,
    val images: List<ImageModel>,
    val consumables: List<ConsumableModel>
)

@Serializable
data class CampaignModel(val id: Long, val name: String, val description: String, val created: Long, val icon: String?)
@Serializable
data class CategoryModel(val id: Long, val campaignId: Long, val name: String, val color: String, val parentId: Long?, val orderIndex: Int, val isOpen: Boolean)
@Serializable
data class NoteModel(val id: Long, val categoryId: Long, val title: String, val body: String, val drawing: String?, val created: Long, val sheet: String)
@Serializable
data class ImageModel(val id: Long, val noteId: Long, val name: String, val data: String, val type: String, val orderIndex: Int)
@Serializable
data class ConsumableModel(val id: Long, val noteId: Long, val name: String, val max: Int, val current: Int, val orderIndex: Int)

class ImportExportViewModel(private val repository: DndRepository) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun exportAllData(contentResolver: ContentResolver, uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val backup = UniversalBackupModel(
                        campaigns = repository.getAllCampaignsRaw().map { CampaignModel(it.id, it.name, it.description, it.created, it.icon) },
                        categories = repository.getAllCategoriesRaw().map { CategoryModel(it.id, it.campaignId, it.name, it.color, it.parentId, it.orderIndex, it.isOpen) },
                        notes = repository.getAllNotesRaw().map { NoteModel(it.id, it.categoryId, it.title, it.body, it.drawing, it.created, it.sheet) },
                        images = repository.getAllImagesRaw().map { ImageModel(it.id, it.noteId, it.name, it.data, it.type, it.orderIndex) },
                        consumables = repository.getAllConsumablesRaw().map { ConsumableModel(it.id, it.noteId, it.name, it.max, it.current, it.orderIndex) }
                    )
                    val jsonString = json.encodeToString(backup)
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonString.toByteArray())
                    }
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
            onComplete(result)
        }
    }

    fun importAllData(contentResolver: ContentResolver, uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val jsonString = inputStream.bufferedReader().use { it.readText() }
                        val backup = json.decodeFromString<UniversalBackupModel>(jsonString)
                        
                        // ID Mapping to prevent conflicts and maintain relationships
                        val campaignIdMap = mutableMapOf<Long, Long>()
                        val categoryIdMap = mutableMapOf<Long, Long>()
                        val noteIdMap = mutableMapOf<Long, Long>()

                        // 1. Campaigns
                        backup.campaigns.forEach { model ->
                            val newId = repository.insertCampaign(Campaign(name = model.name, description = model.description, created = model.created, icon = model.icon))
                            campaignIdMap[model.id] = newId
                        }

                        // 2. Categories (Handling hierarchy is tricky, doing simple flat for now or two-pass)
                        // Pass 1: Insert all and map IDs
                        backup.categories.forEach { model ->
                            val newCampaignId = campaignIdMap[model.campaignId] ?: 1L
                            val newId = repository.insertCategory(Category(campaignId = newCampaignId, name = model.name, color = model.color, parentId = null, orderIndex = model.orderIndex, isOpen = model.isOpen))
                            categoryIdMap[model.id] = newId
                        }
                        // Pass 2: Update parent IDs
                        backup.categories.filter { it.parentId != null }.forEach { model ->
                            val newId = categoryIdMap[model.id]
                            val newParentId = categoryIdMap[model.parentId]
                            if (newId != null && newParentId != null) {
                                val current = Category(id = newId, campaignId = campaignIdMap[model.campaignId]!!, name = model.name, color = model.color, parentId = newParentId, orderIndex = model.orderIndex, isOpen = model.isOpen)
                                repository.updateCategory(current)
                            }
                        }

                        // 3. Notes
                        backup.notes.forEach { model ->
                            val newCategoryId = categoryIdMap[model.categoryId] ?: return@forEach
                            val newId = repository.insertNote(Note(categoryId = newCategoryId, title = model.title, body = model.body, drawing = model.drawing, created = model.created, sheet = model.sheet))
                            noteIdMap[model.id] = newId
                        }

                        // 4. Images
                        backup.images.forEach { model ->
                            val newNoteId = noteIdMap[model.noteId] ?: return@forEach
                            repository.insertImage(ImageAttachment(noteId = newNoteId, name = model.name, data = model.data, type = model.type, orderIndex = model.orderIndex))
                        }

                        // 5. Consumables
                        backup.consumables.forEach { model ->
                            val newNoteId = noteIdMap[model.noteId] ?: return@forEach
                            repository.insertConsumable(ConsumableItem(noteId = newNoteId, name = model.name, max = model.max, current = model.current, orderIndex = model.orderIndex))
                        }
                        true
                    } ?: false
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
            onComplete(result)
        }
    }
}
