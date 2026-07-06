package com.example.dndnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.dndnotes.ui.ViewModelFactory
import com.example.dndnotes.ui.navigation.DndNavGraph
import com.example.dndnotes.ui.screens.ImportExportViewModel
import com.example.dndnotes.ui.screens.CampaignViewModel
import com.example.dndnotes.ui.screens.CategoryViewModel
import com.example.dndnotes.ui.screens.ConsumablesViewModel
import com.example.dndnotes.ui.screens.ImagesViewModel
import com.example.dndnotes.ui.screens.NoteViewModel
import com.example.dndnotes.ui.theme.DndNotesTheme
import com.example.dndnotes.ui.theme.DndTheme
import com.example.dndnotes.ui.theme.ThemeViewModel
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    private val repository by lazy { (application as DndApplication).repository }
    private val themePrefs by lazy { (application as DndApplication).themePreferences }
    private val factory by lazy { ViewModelFactory(repository, themePrefs) }
    
    private val categoryViewModel: CategoryViewModel by viewModels { factory }
    private val campaignViewModel: CampaignViewModel by viewModels { factory }
    private val noteViewModel: NoteViewModel by viewModels { factory }
    private val imagesViewModel: ImagesViewModel by viewModels { factory }
    private val consumablesViewModel: ConsumablesViewModel by viewModels { factory }
    private val themeViewModel: ThemeViewModel by viewModels { factory }
    private val importExportViewModel: ImportExportViewModel by viewModels { factory }

    private var onFileSelected: ((android.net.Uri?) -> Unit)? = null

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        onFileSelected?.invoke(uri)
    }

    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        onFileSelected?.invoke(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val currentTheme by themeViewModel.currentTheme.collectAsState()

            DndNotesTheme(theme = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    DndNavGraph(
                        navController = navController,
                        campaignViewModel = campaignViewModel,
                        categoryViewModel = categoryViewModel,
                        noteViewModel = noteViewModel,
                        imagesViewModel = imagesViewModel,
                        consumablesViewModel = consumablesViewModel,
                        themeViewModel = themeViewModel,
                        importExportViewModel = importExportViewModel,
                        onCreateDocument = { fileName, callback ->
                            onFileSelected = callback
                            createDocumentLauncher.launch(fileName)
                        },
                        onOpenDocument = { callback ->
                            onFileSelected = callback
                            openDocumentLauncher.launch(arrayOf("application/json"))
                        }
                    )
                }
            }
        }
    }
}
