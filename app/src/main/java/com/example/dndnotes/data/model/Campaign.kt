package com.example.dndnotes.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "campaigns")
data class Campaign(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val created: Long = System.currentTimeMillis(),
    val icon: String? = null // For future use
)
