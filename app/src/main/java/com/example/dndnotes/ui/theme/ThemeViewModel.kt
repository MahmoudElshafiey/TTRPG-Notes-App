package com.example.dndnotes.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dndnotes.data.prefs.ThemePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(private val themePreferences: ThemePreferences) : ViewModel() {
    val currentTheme: StateFlow<DndTheme> = themePreferences.themeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DndTheme.DARK_PARCHMENT)

    val backgroundImageUri: StateFlow<String?> = themePreferences.backgroundImageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setTheme(theme: DndTheme) {
        viewModelScope.launch {
            themePreferences.saveTheme(theme)
        }
    }

    fun setBackgroundImage(uri: String?) {
        viewModelScope.launch {
            themePreferences.saveBackgroundImage(uri)
        }
    }

    val categoryGridView: StateFlow<Boolean> = themePreferences.categoryGridViewFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val noteGridView: StateFlow<Boolean> = themePreferences.noteGridViewFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setCategoryGridView(gridView: Boolean) {
        viewModelScope.launch {
            themePreferences.saveCategoryGridView(gridView)
        }
    }

    fun setNoteGridView(gridView: Boolean) {
        viewModelScope.launch {
            themePreferences.saveNoteGridView(gridView)
        }
    }
}
