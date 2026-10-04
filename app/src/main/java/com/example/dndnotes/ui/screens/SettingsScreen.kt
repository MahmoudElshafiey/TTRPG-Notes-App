package com.example.dndnotes.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.dndnotes.ui.components.BackgroundImage
import com.example.dndnotes.ui.theme.ThemeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel,
    backupViewModel: BackupViewModel,
    onChooseBackupFolder: () -> Unit,
    onBack: () -> Unit
) {
    val currentTheme by themeViewModel.currentTheme.collectAsState()
    val backgroundUri by themeViewModel.backgroundImageUri.collectAsState()
    val backupState by backupViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showThemeDialog by remember { mutableStateOf(false) }

    // One-shot result from a manual backup or a restore.
    LaunchedEffect(backupState.message) {
        backupState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            backupViewModel.consumeMessage()
        }
    }

    val backgroundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val original = BitmapFactory.decodeStream(stream)
                        original?.let { bmp ->
                            val metrics = context.resources.displayMetrics
                            val target = maxOf(metrics.widthPixels, metrics.heightPixels)
                            val scale = (maxOf(bmp.width, bmp.height).toFloat() / target).coerceAtLeast(1f)
                            val scaled = if (scale > 1f) {
                                Bitmap.createScaledBitmap(
                                    bmp,
                                    (bmp.width / scale).toInt(),
                                    (bmp.height / scale).toInt(),
                                    true
                                )
                            } else {
                                bmp
                            }
                            val out = ByteArrayOutputStream()
                            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
                            if (scaled != bmp) scaled.recycle()
                            bmp.recycle()
                            val file = File(context.filesDir, "background.jpg")
                            file.writeBytes(out.toByteArray())
                            withContext(Dispatchers.Main.immediate) {
                                themeViewModel.setBackgroundImage(file.absolutePath)
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium)

            Button(
                onClick = { showThemeDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Theme: ${currentTheme.displayName}")
            }

            HorizontalDivider()

            Text("Background Image", style = MaterialTheme.typography.titleMedium)
            Text(
                "Shown behind all screens. Stored on this device only.",
                style = MaterialTheme.typography.bodySmall
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                if (backgroundUri != null) {
                    BackgroundImage(
                        uriString = backgroundUri,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("No background image")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { backgroundLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose Image")
                }

                if (backgroundUri != null) {
                    OutlinedButton(
                        onClick = {
                            File(context.filesDir, "background.jpg").delete()
                            themeViewModel.setBackgroundImage(null)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remove")
                    }
                }
            }

            HorizontalDivider()

            AutomaticBackupsSection(
                viewModel = backupViewModel,
                onChooseFolder = onChooseBackupFolder
            )
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { theme -> themeViewModel.setTheme(theme) }
        )
    }
}
