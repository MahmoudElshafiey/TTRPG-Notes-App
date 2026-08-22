package com.example.dndnotes.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.dndnotes.data.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
fun SheetTab(note: Note, onUpdate: (Note) -> Unit) {
    val sheetData = remember(note.id) { mutableStateListOf<List<String>>() }
    var sheetLoaded by remember(note.id) { mutableStateOf(false) }

    LaunchedEffect(note.id) {
        val parsed = withContext(Dispatchers.Default) {
            try {
                Json.decodeFromString<List<List<String>>>(note.sheet)
            } catch (e: Exception) {
                listOf(listOf(""))
            }
        }
        sheetData.clear()
        sheetData.addAll(parsed)
        sheetLoaded = true
    }

    // Debounced Save (runs only after the sheet has loaded)
    LaunchedEffect(note.id, sheetLoaded) {
        if (!sheetLoaded) return@LaunchedEffect
        snapshotFlow { sheetData.toList() }
            .collectLatest { data ->
                delay(1000)
                val currentJson = withContext(Dispatchers.Default) {
                    Json.encodeToString(data)
                }
                if (currentJson != note.sheet) {
                    onUpdate(note.copy(sheet = currentJson))
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!sheetLoaded) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val newRow = List(if (sheetData.isEmpty()) 1 else sheetData[0].size) { "" }
                        sheetData.add(newRow)
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("Add Row", maxLines = 1)
                }
                Button(
                    onClick = {
                        if (sheetData.isNotEmpty()) {
                            sheetData.removeAt(sheetData.size - 1)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("Rem Row", maxLines = 1)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        for (i in sheetData.indices) {
                            sheetData[i] = sheetData[i] + ""
                        }
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("Add Col", maxLines = 1)
                }
                Button(
                    onClick = {
                        for (i in sheetData.indices) {
                            if (sheetData[i].isNotEmpty()) {
                                sheetData[i] = sheetData[i].dropLast(1)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("Rem Col", maxLines = 1)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            itemsIndexed(sheetData, key = { index, _ -> "row_${note.id}_$index" }) { rowIndex, row ->
                LazyRow(modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(row, key = { index, _ -> "cell_${note.id}_${rowIndex}_$index" }) { colIndex, cellValue ->
                        CellItem(
                            value = cellValue,
                            onValueChange = { newValue ->
                                val updatedRow = row.toMutableList()
                                updatedRow[colIndex] = newValue
                                sheetData[rowIndex] = updatedRow
                            },
                            onDeleteRow = {
                                sheetData.removeAt(rowIndex)
                            },
                            onDeleteCol = {
                                for (i in sheetData.indices) {
                                    val r = sheetData[i].toMutableList()
                                    if (colIndex < r.size) {
                                        r.removeAt(colIndex)
                                        sheetData[i] = r
                                    }
                                }
                            },
                            isHeader = rowIndex == 0 || colIndex == 0
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
fun CellItem(
    value: String,
    onValueChange: (String) -> Unit,
    onDeleteRow: () -> Unit,
    onDeleteCol: () -> Unit,
    isHeader: Boolean
) {
    var showDialog by remember { mutableStateOf(false) }
    var textValue by remember(value) { mutableStateOf(value) }

    Box(
        modifier = Modifier
            .size(width = 100.dp, height = 50.dp)
            .border(0.5.dp, Color.Gray)
            .clickable { showDialog = true }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
            maxLines = 2
        )

        // Simple context menu via long press could be added here, 
        // but for now let's add a small icon or just use a different dialog.
        // Let's use the edit dialog to also offer deletion options.
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Edit Cell") },
            text = {
                Column {
                    TextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                onDeleteRow()
                                showDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete Row")
                        }
                        TextButton(
                            onClick = {
                                onDeleteCol()
                                showDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete Column")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onValueChange(textValue)
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
