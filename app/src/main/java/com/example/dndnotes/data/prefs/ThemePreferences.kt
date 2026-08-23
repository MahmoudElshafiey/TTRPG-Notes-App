package com.example.dndnotes.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.dndnotes.ui.theme.DndTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class ThemePreferences(private val context: Context) {
    private val themeKey = stringPreferencesKey("app_theme")
    private val backgroundImageKey = stringPreferencesKey("background_image_uri")
    private val categoryGridViewKey = booleanPreferencesKey("category_grid_view")
    private val noteGridViewKey = booleanPreferencesKey("note_grid_view")

    val themeFlow: Flow<DndTheme> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[themeKey] ?: DndTheme.DARK_PARCHMENT.name
            try {
                DndTheme.valueOf(themeName)
            } catch (e: Exception) {
                DndTheme.DARK_PARCHMENT
            }
        }

    val backgroundImageFlow: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[backgroundImageKey] }

    val categoryGridViewFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[categoryGridViewKey] ?: false }

    val noteGridViewFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[noteGridViewKey] ?: false }

    suspend fun saveTheme(theme: DndTheme) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = theme.name
        }
    }

    suspend fun saveBackgroundImage(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(backgroundImageKey)
            } else {
                preferences[backgroundImageKey] = uri
            }
        }
    }

    suspend fun saveCategoryGridView(gridView: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[categoryGridViewKey] = gridView
        }
    }

    suspend fun saveNoteGridView(gridView: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[noteGridViewKey] = gridView
        }
    }
}
