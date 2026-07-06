package com.example.dndnotes.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dndnotes.data.model.ConsumableItem
import com.example.dndnotes.data.repository.DndRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ConsumablesViewModel(private val repository: DndRepository) : ViewModel() {
    private val _currentNoteId = MutableStateFlow<Long?>(null)
    
    val consumables: StateFlow<List<ConsumableItem>> = _currentNoteId.flatMapLatest { noteId ->
        if (noteId != null) repository.getConsumablesForNote(noteId)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadConsumables(noteId: Long) {
        _currentNoteId.value = noteId
    }

    fun addConsumable(noteId: Long, name: String, max: Int) {
        viewModelScope.launch {
            val orderIndex = consumables.value.size
            repository.insertConsumable(
                ConsumableItem(
                    noteId = noteId,
                    name = name,
                    max = max,
                    current = max,
                    orderIndex = orderIndex
                )
            )
        }
    }

    fun updateConsumable(item: ConsumableItem) {
        viewModelScope.launch {
            repository.updateConsumable(item)
        }
    }

    fun deleteConsumable(item: ConsumableItem) {
        viewModelScope.launch {
            repository.deleteConsumable(item)
        }
    }
}
