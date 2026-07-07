package com.example.dndnotes.data.model

data class NoteSummary(
    val id: Long,
    val categoryId: Long,
    val title: String,
    val body: String,
    val created: Long,
    val hasDrawing: Boolean
)
