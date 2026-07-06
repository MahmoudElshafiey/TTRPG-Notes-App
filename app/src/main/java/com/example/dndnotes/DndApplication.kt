package com.example.dndnotes

import android.app.Application
import com.example.dndnotes.data.local.AppDatabase
import com.example.dndnotes.data.prefs.ThemePreferences
import com.example.dndnotes.data.repository.DndRepository

class DndApplication : Application() {
    private val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { DndRepository(database.campaignDao(), database.categoryDao(), database.noteDao()) }
    val themePreferences by lazy { ThemePreferences(this) }
}
