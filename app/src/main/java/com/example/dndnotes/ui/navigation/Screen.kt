package com.example.dndnotes.ui.navigation

sealed class Screen(val route: String) {
    object CampaignList : Screen("campaigns")
    object CategoryList : Screen("categories/{campaignId}") {
        fun createRoute(campaignId: Long) = "categories/$campaignId"
    }
    object NoteList : Screen("notes/{categoryId}") {
        fun createRoute(categoryId: Long) = "notes/$categoryId"
    }
    object NoteEditor : Screen("editor/{noteId}") {
        fun createRoute(noteId: Long) = "editor/$noteId"
    }
    object Search : Screen("search")
    object Settings : Screen("settings")
}
