package com.example.dndnotes.data.local

import androidx.room.*
import com.example.dndnotes.data.model.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesRaw(): List<Category>

    @Query("SELECT * FROM categories WHERE campaignId = :campaignId ORDER BY orderIndex ASC")
    fun getAllCategories(campaignId: Long): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE campaignId = :campaignId AND parentId IS NULL ORDER BY orderIndex ASC")
    fun getRootCategories(campaignId: Long): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE parentId = :parentId ORDER BY orderIndex ASC")
    fun getSubcategories(parentId: Long): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: Long)

    /** Used by the REPLACE-mode restore, which empties the whole table at once. */
    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()
}
