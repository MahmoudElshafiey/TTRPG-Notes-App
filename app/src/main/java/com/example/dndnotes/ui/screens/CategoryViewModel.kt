package com.example.dndnotes.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dndnotes.data.model.Category
import com.example.dndnotes.data.repository.DndRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryViewModel(private val repository: DndRepository) : ViewModel() {
    private val _currentCampaignId = MutableStateFlow<Long?>(null)

    val allCategories: StateFlow<List<Category>> = _currentCampaignId.flatMapLatest { campaignId ->
        if (campaignId != null) repository.getAllCategories(campaignId)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Note count for the pending delete confirmation; null while not confirming. */
    private val _deletionNoteCount = MutableStateFlow<Int?>(null)
    val deletionNoteCount: StateFlow<Int?> = _deletionNoteCount.asStateFlow()

    fun setCampaign(campaignId: Long) {
        _currentCampaignId.value = campaignId
    }

    fun addCategory(campaignId: Long, name: String, color: String, parentId: Long? = null) {
        viewModelScope.launch {
            val orderIndex = allCategories.value.filter { it.parentId == parentId }.size
            repository.insertCategory(
                Category(
                    campaignId = campaignId,
                    name = name,
                    color = color,
                    parentId = parentId,
                    orderIndex = orderIndex
                )
            )
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    /**
     * Loads how many notes (including those in subcategories) deleting [category] would
     * destroy. Deleting a category cascades, so the confirmation dialog states the real
     * cost rather than just naming the category.
     */
    fun loadNoteCountForDeletion(category: Category) {
        viewModelScope.launch {
            _deletionNoteCount.value = repository.getSubtreeNoteCount(category.id)
        }
    }

    fun clearDeletionNoteCount() {
        _deletionNoteCount.value = null
    }
}
