package com.example.dndnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.navigation.compose.rememberNavController
import com.example.dndnotes.ui.ViewModelFactory
import com.example.dndnotes.ui.navigation.DndNavGraph
import com.example.dndnotes.ui.screens.ImportExportViewModel
import com.example.dndnotes.ui.screens.CampaignViewModel
import com.example.dndnotes.ui.screens.CategoryViewModel
import com.example.dndnotes.ui.screens.ConsumablesViewModel
import com.example.dndnotes.ui.screens.ImagesViewModel
import com.example.dndnotes.ui.screens.NoteViewModel
import com.example.dndnotes.ui.screens.BackupViewModel
import com.example.dndnotes.ui.components.BackgroundImage
import com.example.dndnotes.ui.theme.DndNotesTheme
import com.example.dndnotes.ui.theme.DndTheme
import com.example.dndnotes.ui.theme.ThemeViewModel
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    private val dndApplication by lazy { application as DndApplication }
    private val repository by lazy { dndApplication.repository }
    private val factory by lazy {
        ViewModelFactory(
            application = dndApplication,
            repository = repository,
            themePreferences = dndApplication.themePreferences,
            backupPreferences = dndApplication.backupPreferences,
            backupManager = dndApplication.backupManager,
            backupFileStore = dndApplication.backupFileStore
        )
    }

    private val categoryViewModel: CategoryViewModel by viewModels { factory }
    private val campaignViewModel: CampaignViewModel by viewModels { factory }
    private val noteViewModel: NoteViewModel by viewModels { factory }
    private val imagesViewModel: ImagesViewModel by viewModels { factory }
    private val consumablesViewModel: ConsumablesViewModel by viewModels { factory }
    private val themeViewModel: ThemeViewModel by viewModels { factory }
    private val importExportViewModel: ImportExportViewModel by viewModels { factory }
    private val backupViewModel: BackupViewModel by viewModels { factory }

    private var onFileSelected: ((android.net.Uri?) -> Unit)? = null

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        onFileSelected?.invoke(uri)
    }

    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        onFileSelected?.invoke(uri)
    }

    /**
     * Lets the user pick the folder automatic backups are written to. The grant is taken
     * for good by BackupViewModel, so this only has to run once per folder choice.
     */
    private val pickBackupFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        backupViewModel.onFolderSelected(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val currentTheme by themeViewModel.currentTheme.collectAsState()
            val backgroundUri by themeViewModel.backgroundImageUri.collectAsState()

            DisposableEffect(currentTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                    navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                )
                onDispose {}
            }

            DndNotesTheme(theme = currentTheme) {
                Box(modifier = Modifier.fillMaxSize()) {
                    BackgroundImage(
                        uriString = backgroundUri,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Flush the pending "unsaved changes" flag so it survives the process
                    // being killed while the app sits in the background.
                    DisposableEffect(Unit) {
                        onDispose { dndApplication.onEnteredBackground() }
                    }

                    val scrimAlpha = if (backgroundUri != null) 0.55f else 1f
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = scrimAlpha))
                    )
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent,
                        // A transparent Surface would resolve its content color to
                        // Unspecified and let LocalContentColor fall back to black, so the
                        // theme's text color is pinned here explicitly.
                        contentColor = MaterialTheme.colorScheme.onBackground
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
                            backupViewModel = backupViewModel,
                            onCreateDocument = { fileName, callback ->
                                onFileSelected = callback
                                createDocumentLauncher.launch(fileName)
                            },
                            onOpenDocument = { callback ->
                                onFileSelected = callback
                                openDocumentLauncher.launch(arrayOf("application/json"))
                            },
                            onChooseBackupFolder = {
                                pickBackupFolderLauncher.launch(null)
                            }
                        )
                    }
                }
            }
        }
    }
}
