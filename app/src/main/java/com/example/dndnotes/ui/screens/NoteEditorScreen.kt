package com.example.dndnotes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import java.io.InputStream
import com.example.dndnotes.data.model.ImageAttachment
import com.example.dndnotes.data.model.Note
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    viewModel: NoteViewModel,
    imagesViewModel: ImagesViewModel,
    consumablesViewModel: ConsumablesViewModel
) {
    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
        imagesViewModel.loadImages(noteId)
    }

    val note by viewModel.currentNote.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Write", "Images", "Drawing", "Sheet", "Consumables")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(note?.title ?: "Edit Note") }
            )
        }
    ) { padding ->
        note?.let { currentNote ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                SecondaryTabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> WriteTab(currentNote, onUpdate = { viewModel.updateNote(it) })
                        1 -> ImagesTab(currentNote, viewModel = imagesViewModel)
                        2 -> DrawingTab(currentNote, onUpdate = { viewModel.updateNote(it) })
                        3 -> SheetTab(currentNote, onUpdate = { viewModel.updateNote(it) })
                        4 -> ConsumablesTab(currentNote, viewModel = consumablesViewModel)
                    }
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun WriteTab(note: Note, onUpdate: (Note) -> Unit) {
    var title by remember(note.id) { mutableStateOf(note.title) }
    var body by remember(note.id) { mutableStateOf(note.body) }

    // Debounced Save
    LaunchedEffect(title, body) {
        if (title != note.title || body != note.body) {
            delay(1000) // Wait for 1 second of inactivity
            onUpdate(note.copy(title = title, body = body))
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Title") },
            textStyle = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextField(
            value = body,
            onValueChange = { body = it },
            modifier = Modifier.fillMaxWidth().weight(1f),
            label = { Text("Body") }
        )
    }
}

@Composable
fun ImagesTab(note: Note, viewModel: ImagesViewModel) {
    val images by viewModel.images.collectAsState()
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val inputStream: InputStream? = context.contentResolver.openInputStream(it)
            val bytes = inputStream?.readBytes()
            if (bytes != null) {
                val base64 = Base64.encodeToString(bytes, Base64.DEFAULT)
                val fileName = it.lastPathSegment ?: "image_${System.currentTimeMillis()}"
                viewModel.addImage(note.id, fileName, base64, context.contentResolver.getType(it) ?: "image/*")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = { launcher.launch("image/*") },
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Image")
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(images) { image ->
                ImageItem(image = image, onDelete = { viewModel.deleteImage(image) })
            }
        }
    }
}

@Composable
fun ImageItem(image: ImageAttachment, onDelete: () -> Unit) {
    val bitmap = remember(image.data) {
        val bytes = Base64.decode(image.data, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box {
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = image.name,
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd).padding(8.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
            Text(
                text = image.name,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
