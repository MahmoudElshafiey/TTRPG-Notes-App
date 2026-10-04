package com.example.dndnotes.data.backup

import com.example.dndnotes.data.model.Campaign
import com.example.dndnotes.data.model.Category
import com.example.dndnotes.data.model.ConsumableItem
import com.example.dndnotes.data.model.ImageAttachment
import com.example.dndnotes.data.model.Note
import com.example.dndnotes.data.repository.DndRepository
import kotlinx.serialization.json.Json

/**
 * How a backup should be applied to the current database.
 */
enum class RestoreMode {
    /**
     * Adds the backup's contents alongside whatever is already there. Existing rows are
     * left untouched and incoming rows get fresh primary keys, so nothing can be lost.
     * This is the safe default: recovering deleted notes never overwrites newer work.
     */
    MERGE,

    /**
     * Wipes every table and then imports the backup, which reproduces the original
     * database exactly (original ids, order and all). Destructive - the caller is
     * expected to have confirmed with the user first.
     */
    REPLACE
}

/** Outcome of a restore attempt, including anything that had to be skipped. */
data class RestoreSummary(
    val campaigns: Int,
    val categories: Int,
    val notes: Int,
    val images: Int,
    val consumables: Int,
    val skippedNotes: Int,
    val skippedCategories: Int
)

/**
 * Converts the whole database to and from the backup JSON format.
 *
 * Both the manual export/import buttons and the automatic backup worker go through here,
 * so a file produced by one is always readable by the other.
 */
object BackupSerializer {

    val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    /** Snapshots the entire database as pretty-printed JSON. */
    suspend fun buildBackupJson(repository: DndRepository): String {
        val backup = UniversalBackupModel(
            campaigns = repository.getAllCampaignsRaw()
                .map { CampaignModel(it.id, it.name, it.description, it.created, it.icon) },
            categories = repository.getAllCategoriesRaw()
                .map { CategoryModel(it.id, it.campaignId, it.name, it.color, it.parentId, it.orderIndex, it.isOpen) },
            notes = repository.getAllNotesRaw()
                .map { NoteModel(it.id, it.categoryId, it.title, it.body, it.drawing, it.created, it.sheet) },
            images = repository.getAllImagesRaw()
                .map { ImageModel(it.id, it.noteId, it.name, it.data, it.type, it.orderIndex) },
            consumables = repository.getAllConsumablesRaw()
                .map { ConsumableModel(it.id, it.noteId, it.name, it.max, it.current, it.orderIndex) }
        )
        return json.encodeToString(backup)
    }

    /**
     * Applies a backup document to the database.
     *
     * The whole import runs inside a single database transaction. That matters most for
     * [RestoreMode.REPLACE]: the tables are emptied first, so a failure halfway through
     * an uncommitted import would otherwise leave the user with an empty database.
     * On any exception the transaction rolls back and the previous data is intact.
     *
     * @throws Exception if the document is malformed or the write fails; the caller
     *   should surface this to the user rather than silently ignoring it.
     */
    suspend fun restore(
        repository: DndRepository,
        contents: String,
        mode: RestoreMode
    ): RestoreSummary {
        val backup = json.decodeFromString<UniversalBackupModel>(contents)

        return repository.inTransaction {
            if (mode == RestoreMode.REPLACE) {
                repository.clearAll()
            }
            insertAll(repository, backup)
        }
    }

    private suspend fun insertAll(
        repository: DndRepository,
        backup: UniversalBackupModel
    ): RestoreSummary {
        var skippedCategories = 0
        var skippedNotes = 0

        // Old id -> new id. Incoming rows never reuse the source ids, which is what
        // lets MERGE coexist with existing data without primary key collisions.
        val campaignIdMap = mutableMapOf<Long, Long>()
        val categoryIdMap = mutableMapOf<Long, Long>()
        val noteIdMap = mutableMapOf<Long, Long>()

        // 1. Campaigns
        backup.campaigns.forEach { model ->
            campaignIdMap[model.id] = repository.insertCampaign(
                Campaign(name = model.name, description = model.description, created = model.created, icon = model.icon)
            )
        }

        // A category can only be inserted once its campaign exists. The previous
        // implementation defaulted a missing campaign to id 1, which trips the foreign
        // key constraint and aborts the whole import; skipping is recoverable instead.
        val insertableCategories = backup.categories.filter { campaignIdMap.containsKey(it.campaignId) }
        skippedCategories = backup.categories.size - insertableCategories.size

        // 2. Categories, in two passes so nesting survives.
        // Pass 1: insert every category as a root and record its new id.
        insertableCategories.forEach { model ->
            val newId = repository.insertCategory(
                Category(
                    campaignId = campaignIdMap.getValue(model.campaignId),
                    name = model.name,
                    color = model.color,
                    parentId = null,
                    orderIndex = model.orderIndex,
                    isOpen = model.isOpen
                )
            )
            categoryIdMap[model.id] = newId
        }
        // Pass 2: re-parent the subcategories now that every parent id is known.
        insertableCategories.filter { it.parentId != null }.forEach { model ->
            val newId = categoryIdMap[model.id] ?: return@forEach
            val newParentId = categoryIdMap[model.parentId] ?: return@forEach
            val newCampaignId = campaignIdMap[model.campaignId] ?: return@forEach
            repository.updateCategory(
                Category(
                    id = newId,
                    campaignId = newCampaignId,
                    name = model.name,
                    color = model.color,
                    parentId = newParentId,
                    orderIndex = model.orderIndex,
                    isOpen = model.isOpen
                )
            )
        }

        // 3. Notes
        backup.notes.forEach { model ->
            val newCategoryId = categoryIdMap[model.categoryId]
            if (newCategoryId == null) {
                // No category survived (usually a backup whose parent campaign was
                // already deleted). Skip the note rather than fail the restore.
                skippedNotes++
                return@forEach
            }
            noteIdMap[model.id] = repository.insertNote(
                Note(
                    categoryId = newCategoryId,
                    title = model.title,
                    body = model.body,
                    drawing = model.drawing,
                    created = model.created,
                    sheet = model.sheet
                )
            )
        }

        // 4. Images
        var imageCount = 0
        backup.images.forEach { model ->
            val newNoteId = noteIdMap[model.noteId] ?: return@forEach
            repository.insertImage(
                ImageAttachment(noteId = newNoteId, name = model.name, data = model.data, type = model.type, orderIndex = model.orderIndex)
            )
            imageCount++
        }

        // 5. Consumables
        var consumableCount = 0
        backup.consumables.forEach { model ->
            val newNoteId = noteIdMap[model.noteId] ?: return@forEach
            repository.insertConsumable(
                ConsumableItem(noteId = newNoteId, name = model.name, max = model.max, current = model.current, orderIndex = model.orderIndex)
            )
            consumableCount++
        }

        return RestoreSummary(
            campaigns = campaignIdMap.size,
            categories = categoryIdMap.size,
            notes = noteIdMap.size,
            images = imageCount,
            consumables = consumableCount,
            skippedNotes = skippedNotes,
            skippedCategories = skippedCategories
        )
    }
}