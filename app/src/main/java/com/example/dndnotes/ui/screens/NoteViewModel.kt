package com.example.dndnotes.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dndnotes.data.model.Note
import com.example.dndnotes.data.repository.DndRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NoteViewModel(private val repository: DndRepository) : ViewModel() {
    private val _currentCategoryId = MutableStateFlow<Long?>(null)
    
    val notes: StateFlow<List<Note>> = _currentCategoryId.flatMapLatest { categoryId ->
        if (categoryId != null) repository.getNotesByCategory(categoryId)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCategory(categoryId: Long) {
        _currentCategoryId.value = categoryId
    }

    fun addNote(categoryId: Long, title: String) {
        viewModelScope.launch {
            repository.insertNote(Note(categoryId = categoryId, title = title, body = ""))
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    // Single Note Details (for Editor)
    private val _currentNoteId = MutableStateFlow<Long?>(null)
    val currentNote: StateFlow<Note?> = _currentNoteId.flatMapLatest { noteId ->
        if (noteId != null) {
            repository.getNoteById(noteId)
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun loadNote(noteId: Long) {
        _currentNoteId.value = noteId
    }
}
