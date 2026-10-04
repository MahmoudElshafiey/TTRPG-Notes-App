package com.example.dndnotes.data.local

import androidx.room.*
import com.example.dndnotes.data.model.Campaign
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignDao {
    @Query("SELECT * FROM campaigns")
    suspend fun getAllCampaignsRaw(): List<Campaign>

    @Query("SELECT * FROM campaigns ORDER BY created DESC")
    fun getAllCampaigns(): Flow<List<Campaign>>

    @Query("SELECT * FROM campaigns WHERE id = :id")
    suspend fun getCampaignById(id: Long): Campaign?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaign(campaign: Campaign): Long

    @Update
    suspend fun updateCampaign(campaign: Campaign)

    @Delete
    suspend fun deleteCampaign(campaign: Campaign)

    /** Used by the REPLACE-mode restore, which empties the whole table at once. */
    @Query("DELETE FROM campaigns")
    suspend fun deleteAllCampaigns()
}
