package com.example.dndnotes.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.dndnotes.ui.screens.*
import com.example.dndnotes.ui.theme.ThemeViewModel

@Composable
fun DndNavGraph(
    navController: NavHostController,
    campaignViewModel: CampaignViewModel,
    categoryViewModel: CategoryViewModel,
    noteViewModel: NoteViewModel,
    imagesViewModel: ImagesViewModel,
    consumablesViewModel: ConsumablesViewModel,
    themeViewModel: ThemeViewModel,
    importExportViewModel: ImportExportViewModel,
    onCreateDocument: (String, (Uri?) -> Unit) -> Unit,
    onOpenDocument: ((Uri?) -> Unit) -> Unit,
    startDestination: String = Screen.CampaignList.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.CampaignList.route) {
            CampaignScreen(
                viewModel = campaignViewModel,
                importExportViewModel = importExportViewModel,
                onCreateDocument = onCreateDocument,
                onOpenDocument = onOpenDocument,
                onCampaignClick = { campaignId ->
                    navController.navigate(Screen.CategoryList.createRoute(campaignId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(
            route = Screen.CategoryList.route,
            arguments = listOf(navArgument("campaignId") { type = NavType.LongType })
        ) { backStackEntry ->
            val campaignId = backStackEntry.arguments?.getLong("campaignId") ?: 1L
            CategoryScreen(
                campaignId = campaignId,
                viewModel = categoryViewModel,
                themeViewModel = themeViewModel,
                onCategoryClick = { categoryId ->
                    navController.navigate(Screen.NoteList.createRoute(categoryId))
                },
                onBackToCampaigns = {
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = Screen.NoteList.route,
            arguments = listOf(navArgument("categoryId") { type = NavType.LongType })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: 0L
            NoteListScreen(
                categoryId = categoryId,
                viewModel = noteViewModel,
                onNoteClick = { noteId ->
                    navController.navigate(Screen.NoteEditor.createRoute(noteId))
                }
            )
        }
        composable(
            route = Screen.NoteEditor.route,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
            NoteEditorScreen(
                noteId = noteId,
                viewModel = noteViewModel,
                imagesViewModel = imagesViewModel,
                consumablesViewModel = consumablesViewModel
            )
        }
        composable(Screen.Search.route) {
            // TODO: SearchScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                themeViewModel = themeViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
