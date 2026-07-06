package com.example.dndnotes.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

import androidx.room.ForeignKey

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = Campaign::class,
            parentColumns = ["id"],
            childColumns = ["campaignId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val campaignId: Long = 1,
    val name: String,
    val color: String, // Hex string
    val parentId: Long? = null,
    val orderIndex: Int,
    val isOpen: Boolean = false
)
