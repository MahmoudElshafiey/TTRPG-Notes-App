package com.example.dndnotes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.dndnotes.data.model.ConsumableItem
import com.example.dndnotes.data.model.Note

@Composable
fun ConsumablesTab(
    note: Note,
    viewModel: ConsumablesViewModel
) {
    LaunchedEffect(note.id) {
        viewModel.loadConsumables(note.id)
    }

    val consumables by viewModel.consumables.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = { viewModel.addConsumable(note.id, "New Item", 5) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add Consumable")
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(consumables) { item ->
                ConsumableItemRow(
                    item = item,
                    onUpdate = { viewModel.updateConsumable(it) },
                    onDelete = { viewModel.deleteConsumable(it) }
                )
            }
        }
    }
}

@Composable
fun ConsumableItemRow(
    item: ConsumableItem,
    onUpdate: (ConsumableItem) -> Unit,
    onDelete: (ConsumableItem) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable { showEditDialog = true }
                )
                IconButton(onClick = { onDelete(item) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }

            Spacer(Modifier.height(8.dp))

            // Pips
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 1..item.max) {
                    val isFilled = i <= item.current
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(1.dp, MaterialTheme.colorScheme.primary)
                            .background(if (isFilled) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable {
                                val newCurrent = if (isFilled && i == item.current) i - 1 else i
                                onUpdate(item.copy(current = newCurrent))
                            }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${item.current} / ${item.max}")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onUpdate(item.copy(current = item.max)) }) {
                    Text("Restore All")
                }
            }
        }
    }

    if (showEditDialog) {
        var name by remember { mutableStateOf(item.name) }
        var max by remember { mutableStateOf(item.max.toString()) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Consumable") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") }
                    )
                    TextField(
                        value = max,
                        onValueChange = { if (it.all { char -> char.isDigit() }) max = it },
                        label = { Text("Max Value") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newMax = max.toIntOrNull() ?: item.max
                    onUpdate(item.copy(
                        name = name,
                        max = newMax,
                        current = if (item.current > newMax) newMax else item.current
                    ))
                    showEditDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
