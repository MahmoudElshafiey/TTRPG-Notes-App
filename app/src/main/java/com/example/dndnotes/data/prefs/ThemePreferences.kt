package com.example.dndnotes.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.dndnotes.ui.theme.DndTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThemePreferences(private val context: Context) {
    private val themeKey = stringPreferencesKey("app_theme")
    private val backgroundImageKey = stringPreferencesKey("background_image_uri")
    private val categoryGridViewKey = booleanPreferencesKey("category_grid_view")
    private val noteGridViewKey = booleanPreferencesKey("note_grid_view")

    val themeFlow: Flow<DndTheme> = context.settingsDataStore.data
        .map { preferences ->
            val themeName = preferences[themeKey] ?: DndTheme.DARK_PARCHMENT.name
            try {
                DndTheme.valueOf(themeName)
            } catch (e: Exception) {
                DndTheme.DARK_PARCHMENT
            }
        }

    val backgroundImageFlow: Flow<String?> = context.settingsDataStore.data
        .map { preferences -> preferences[backgroundImageKey] }

    val categoryGridViewFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[categoryGridViewKey] ?: false }

    val noteGridViewFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[noteGridViewKey] ?: false }

    suspend fun saveTheme(theme: DndTheme) {
        context.settingsDataStore.edit { preferences ->
            preferences[themeKey] = theme.name
        }
    }

    suspend fun saveBackgroundImage(uri: String?) {
        context.settingsDataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(backgroundImageKey)
            } else {
                preferences[backgroundImageKey] = uri
            }
        }
    }

    suspend fun saveCategoryGridView(gridView: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[categoryGridViewKey] = gridView
        }
    }

    suspend fun saveNoteGridView(gridView: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[noteGridViewKey] = gridView
        }
    }
}
