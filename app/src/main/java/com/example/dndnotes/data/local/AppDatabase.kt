package com.example.dndnotes.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.dndnotes.data.model.Campaign
import com.example.dndnotes.data.model.Category
import com.example.dndnotes.data.model.Note
import com.example.dndnotes.data.model.ImageAttachment
import com.example.dndnotes.data.model.ConsumableItem

@Database(
    entities = [Campaign::class, Category::class, Note::class, ImageAttachment::class, ConsumableItem::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun campaignDao(): CampaignDao
    abstract fun categoryDao(): CategoryDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dnd_notes_database"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create campaigns table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS campaigns (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        name TEXT NOT NULL, 
                        description TEXT NOT NULL, 
                        created INTEGER NOT NULL, 
                        icon TEXT
                    )
                """.trimIndent())

                // 2. Insert a default campaign
                db.execSQL("""
                    INSERT INTO campaigns (id, name, description, created) 
                    VALUES (1, 'Default Campaign', 'Initial campaign for existing notes', ${System.currentTimeMillis()})
                """.trimIndent())

                // 3. Add campaignId to categories
                db.execSQL("ALTER TABLE categories ADD COLUMN campaignId INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}
