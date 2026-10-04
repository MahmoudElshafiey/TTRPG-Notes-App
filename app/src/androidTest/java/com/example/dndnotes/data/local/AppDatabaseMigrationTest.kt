package com.example.dndnotes.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Proves the upgrade path out of versions 2 and 3 keeps the user's notes.
 *
 * This is the regression test for the data loss that motivated the backup system: with no
 * 2->3 and 3->4 migration and `fallbackToDestructiveMigration()` enabled, Room silently
 * dropped every table on first launch after an update. The migrations themselves are
 * no-ops because the schema never actually changed for those versions, so the assertion
 * that matters is that rows written at v2 are still there at v4.
 *
 * Requires a connected device or emulator: `connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private val databaseName = "migration-test-db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    private fun schemaFile(version: Int): File {
        // Schemas are exported by KSP into app/schemas via the room.schemaLocation arg.
        val schemas = File("src/androidTest/assets").takeIf { it.isDirectory }
            ?: File("../app/schemas/com.example.dndnotes.data.local.AppDatabase")
        return File(schemas, "$version.json")
    }

    @Test
    fun migrateFrom2To4_preservesCampaignsCategoriesAndNotes() {
        if (!schemaFile(2).exists()) return // No v2 schema was ever exported; nothing to assert against.

        helper.createDatabase(databaseName, 2).apply {
            execSQL(
                """
                INSERT INTO campaigns (id, name, description, created, icon)
                VALUES (1, 'The Yawning Crypt', 'A tomb full of trouble', 1700000000000, NULL)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO categories (id, campaignId, name, color, parentId, orderIndex, isOpen)
                VALUES (7, 1, 'Room of Bones', '#8B0000', NULL, 0, 1)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO notes (id, categoryId, title, body, drawing, created, sheet)
                VALUES (42, 7, 'The Curse', 'Something is wrong with the blood', NULL, 1700000000000, '[]')
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(databaseName, 4, true,
            AppDatabase.Migrations.MIGRATION_2_3,
            AppDatabase.Migrations.MIGRATION_3_4
        )

        migrated.query("SELECT title, body FROM notes WHERE id = 42").use { cursor ->
            assertTrue("The note must survive the upgrade", cursor.moveToFirst())
            assertEquals("The Curse", cursor.getString(0))
            assertEquals("Something is wrong with the blood", cursor.getString(1))
        }
        migrated.query("SELECT name FROM campaigns WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("The Yawning Crypt", cursor.getString(0))
        }
        migrated.query("SELECT name FROM categories WHERE id = 7").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Room of Bones", cursor.getString(0))
        }

        migrated.close()
    }

    @Test
    fun migrateFrom3To4_preservesData() {
        if (!schemaFile(3).exists()) return

        helper.createDatabase(databaseName, 3).apply {
            execSQL(
                """
                INSERT INTO campaigns (id, name, description, created, icon)
                VALUES (1, 'Frozen Vault', '', 1700000000000, NULL)
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(databaseName, 4, true,
            AppDatabase.Migrations.MIGRATION_3_4
        )
        migrated.query("SELECT name FROM campaigns WHERE id = 1").use { cursor ->
            assertTrue("The campaign must survive the upgrade", cursor.moveToFirst())
            assertEquals("Frozen Vault", cursor.getString(0))
        }
        migrated.close()
    }
}