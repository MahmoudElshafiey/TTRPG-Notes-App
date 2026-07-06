package com.example.dndnotes.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dndnotes.data.model.ImageAttachment
import com.example.dndnotes.data.repository.DndRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ImagesViewModel(private val repository: DndRepository) : ViewModel() {
    private val _currentNoteId = MutableStateFlow<Long?>(null)
    
    val images: StateFlow<List<ImageAttachment>> = _currentNoteId.flatMapLatest { noteId ->
        if (noteId != null) repository.getImagesForNote(noteId)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadImages(noteId: Long) {
        _currentNoteId.value = noteId
    }

    fun addImage(noteId: Long, name: String, data: String, type: String) {
        viewModelScope.launch {
            val orderIndex = images.value.size
            repository.insertImage(
                ImageAttachment(
                    noteId = noteId,
                    name = name,
                    data = data,
                    type = type,
                    orderIndex = orderIndex
                )
            )
        }
    }

    fun deleteImage(image: ImageAttachment) {
        viewModelScope.launch {
            repository.deleteImage(image)
        }
    }
}
