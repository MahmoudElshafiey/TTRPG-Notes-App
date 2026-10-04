package com.example.dndnotes.data.backup

import kotlinx.serialization.Serializable

/**
 * The on-disk backup format.
 *
 * This is the exact same shape produced by the manual "Create Backup" button, so
 * automatic backups and hand-made backups are interchangeable - any file this app
 * writes can be read back by the import button and vice versa.
 */
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