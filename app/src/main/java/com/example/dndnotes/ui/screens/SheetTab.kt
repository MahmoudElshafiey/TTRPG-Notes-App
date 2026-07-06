package com.example.dndnotes.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
fun SheetTab(note: Note, onUpdate: (Note) -> Unit) {
    val sheetData = remember(note.sheet) {
        try {
            Json.decodeFromString<List<List<String>>>(note.sheet)
        } catch (e: Exception) {
            listOf(listOf(""))
        }
    }.toMutableStateList()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(8.dp)) {
            Button(onClick = {
                val newRow = List(if (sheetData.isEmpty()) 1 else sheetData[0].size) { "" }
                sheetData.add(newRow)
                onUpdate(note.copy(sheet = Json.encodeToString(sheetData.toList())))
            }) {
                Text("Add Row")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                for (i in sheetData.indices) {
                    sheetData[i] = sheetData[i] + ""
                }
                onUpdate(note.copy(sheet = Json.encodeToString(sheetData.toList())))
            }) {
                Text("Add Col")
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(sheetData) { rowIndex, row ->
                LazyRow {
                    itemsIndexed(row) { colIndex, cellValue ->
                        CellItem(
                            value = cellValue,
                            onValueChange = { newValue ->
                                val updatedRow = row.toMutableList()
                                updatedRow[colIndex] = newValue
                                sheetData[rowIndex] = updatedRow
                                onUpdate(note.copy(sheet = Json.encodeToString(sheetData.toList())))
                            },
                            isHeader = rowIndex == 0 || colIndex == 0
                        )
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
    isHeader: Boolean
) {
    var showDialog by remember { mutableStateOf(false) }
    var textValue by remember(value) { mutableStateOf(value) }

    Box(
        modifier = Modifier
            .size(width = 100.dp, height = 50.dp)
            .border(0.5.dp, Color.Gray)
            .clickable { showDialog = true },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Edit Cell") },
            text = {
                TextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    modifier = Modifier.fillMaxWidth()
                )
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
