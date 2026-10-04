package com.example.dndnotes.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.dndnotes.data.model.Campaign
import com.example.dndnotes.data.model.Category
import com.example.dndnotes.data.model.ConsumableItem
import com.example.dndnotes.data.model.ImageAttachment
import com.example.dndnotes.data.model.Note

@Database(
    entities = [Campaign::class, Category::class, Note::class, ImageAttachment::class, ConsumableItem::class],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun campaignDao(): CampaignDao
    abstract fun categoryDao(): CategoryDao
    abstract fun noteDao(): NoteDao

    /**
     * Every migration, in one place and public so that `MigrationTestHelper` can exercise
     * the real upgrade path instead of a hand-copied duplicate.
     */
    object Migrations {

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create campaigns table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS campaigns (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        created INTEGER NOT NULL,
                        icon TEXT
                    )
                    """.trimIndent()
                )

                // 2. Insert a default campaign
                db.execSQL(
                    """
                    INSERT INTO campaigns (id, name, description, created)
                    VALUES (1, 'Default Campaign', 'Initial campaign for existing notes', ${System.currentTimeMillis()})
                    """.trimIndent()
                )

                // 3. Add campaignId to categories
                db.execSQL("ALTER TABLE categories ADD COLUMN campaignId INTEGER NOT NULL DEFAULT 1")
            }
        }

        // 2 -> 3 and 3 -> 4 are intentionally no-ops.
        //
        // The schema was never actually changed for versions 3 and 4: the database has
        // been declared at version 4 since the first commit and no @Entity has been
        // modified since (verified against git history). Without these migrations Room had
        // no upgrade path, and `fallbackToDestructiveMigration()` silently DROPPED every
        // table on the next launch after an app update - which is how a campaign of notes
        // could disappear "for no reason".
        //
        // They exist so that upgrading is a no-op instead of a data loss event. If a future
        // version does change the schema, replace these with real migrations; the schemas
        // committed under app/schemas/ make that diffable and testable.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) = Unit
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) = Unit
        }

        val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
    }

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
                .addMigrations(*Migrations.ALL)
                // Deliberately no fallbackToDestructiveMigration(): a missing migration
                // should fail loudly during development rather than quietly delete a
                // user's campaign in production.
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}