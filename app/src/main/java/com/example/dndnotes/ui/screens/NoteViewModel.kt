package com.example.dndnotes.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dndnotes.data.model.Note
import com.example.dndnotes.data.model.NoteSummary
import com.example.dndnotes.data.repository.DndRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NoteViewModel(private val repository: DndRepository) : ViewModel() {
    private val _currentCategoryId = MutableStateFlow<Long?>(null)
    
    val notes: StateFlow<List<NoteSummary>> = _currentCategoryId.flatMapLatest { categoryId ->
        if (categoryId != null) repository.getNoteSummariesByCategory(categoryId)
        else flowOf(emptyList())
    }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            if (_currentNote.value?.id == note.id) {
                _currentNote.value = note
            }
        }
    }

    fun updateDrawing(drawing: String?) {
        persistMerge { it.copy(drawing = drawing) }
    }

    fun updateSheet(sheet: String) {
        persistMerge { it.copy(sheet = sheet) }
    }

    fun updateTitleBody(title: String, body: String) {
        persistMerge { it.copy(title = title, body = body) }
    }

    private fun persistMerge(transform: (Note) -> Note) {
        val base = _currentNote.value ?: return
        val updated = transform(base)
        viewModelScope.launch {
            repository.updateNote(updated)
            _currentNote.value = updated
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.deleteNoteById(noteId)
        }
    }

    // Single Note Details (for Editor)
    private val _currentNote = MutableStateFlow<Note?>(null)
    val currentNote: StateFlow<Note?> = _currentNote.asStateFlow()

    fun loadNote(noteId: Long) {
        viewModelScope.launch {
            _currentNote.value = repository.getNoteByIdRaw(noteId)
        }
    }
}
