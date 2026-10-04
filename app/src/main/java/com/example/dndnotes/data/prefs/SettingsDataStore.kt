package com.example.dndnotes.data.prefs

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * The single DataStore instance backing all app settings.
 *
 * The `preferencesDataStore` delegate must be created exactly once per file name for a
 * given process. Declaring a second delegate for "settings" crashes at runtime with
 * "There are multiple DataStores active for the same file", so every preferences class
 * ([ThemePreferences], [BackupPreferences]) resolves the store through this property.
 */
internal val Context.settingsDataStore by preferencesDataStore(name = "settings")