package com.example.dndnotes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.dndnotes.data.model.Note
import com.example.dndnotes.ui.components.DrawingCanvas
import com.example.dndnotes.ui.components.SerializablePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Composable
fun DrawingTab(note: Note, onUpdate: (Note) -> Unit) {
    val scope = rememberCoroutineScope()
    var strokeColor by remember { mutableStateOf(Color.Red) }
    var strokeWidth by remember { mutableStateOf(5f) }
    var undoTrigger by remember { mutableStateOf(0) }
    var clearTrigger by remember { mutableStateOf(0) }

    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta, Color.White, Color.Black)
    
    val initialPaths = remember(note.id) {
        try {
            if (!note.drawing.isNullOrEmpty()) {
                Json.decodeFromString<List<SerializablePath>>(note.drawing)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = { undoTrigger++ }) {
                Icon(Icons.Default.Undo, contentDescription = "Undo")
            }
            IconButton(onClick = { clearTrigger++ }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear")
            }
            
            // Scrollable color picker
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(colors) { color ->
                    val isSelected = strokeColor == color
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(color)
                            .then(
                                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.small)
                                else Modifier
                            )
                            .clickable { strokeColor = color }
                    )
                }
            }
        }

        DrawingCanvas(
            modifier = Modifier.weight(1f),
            initialPaths = initialPaths,
            onDrawingChanged = { paths ->
                scope.launch {
                    val jsonString = withContext(Dispatchers.Default) {
                        Json.encodeToString(paths)
                    }
                    onUpdate(note.copy(drawing = jsonString))
                }
            },
            strokeColor = strokeColor,
            strokeWidth = strokeWidth,
            undoTrigger = undoTrigger,
            clearTrigger = clearTrigger
        )
    }
}
