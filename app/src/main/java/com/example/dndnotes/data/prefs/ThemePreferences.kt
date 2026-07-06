package com.example.dndnotes.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.dndnotes.ui.theme.DndTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class ThemePreferences(private val context: Context) {
    private val themeKey = stringPreferencesKey("app_theme")

    val themeFlow: Flow<DndTheme> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[themeKey] ?: DndTheme.DARK_PARCHMENT.name
            try {
                DndTheme.valueOf(themeName)
            } catch (e: Exception) {
                DndTheme.DARK_PARCHMENT
            }
        }

    suspend fun saveTheme(theme: DndTheme) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = theme.name
        }
    }
}
