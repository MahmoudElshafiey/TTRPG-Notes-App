package com.example.dndnotes.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dndnotes.data.model.Campaign
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignScreen(
    viewModel: CampaignViewModel,
    importExportViewModel: ImportExportViewModel,
    onCreateDocument: (String, (Uri?) -> Unit) -> Unit,
    onOpenDocument: ((Uri?) -> Unit) -> Unit,
    onCampaignClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val campaigns by viewModel.allCampaigns.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var campaignToDelete by remember { mutableStateOf<Campaign?>(null) }
    var campaignToEdit by remember { mutableStateOf<Campaign?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Campaign") },
                actions = {
                    IconButton(onClick = {
                        onOpenDocument { uri ->
                            uri?.let {
                                importExportViewModel.importAllData(context.contentResolver, it) { success ->
                                    if (success) Toast.makeText(context, "Backup restored!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Restore Backup")
                    }
                    IconButton(onClick = {
                        onCreateDocument("dnd_notes_backup.json") { uri ->
                            uri?.let {
                                importExportViewModel.exportAllData(context.contentResolver, it) { success ->
                                    if (success) Toast.makeText(context, "Backup saved!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Create Backup")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Campaign")
            }
        }
    ) { padding ->
        if (campaigns.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No campaigns yet. Tap + to start your adventure!")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(campaigns, key = { it.id }) { campaign ->
                    CampaignItem(
                        campaign = campaign,
                        onClick = { onCampaignClick(campaign.id) },
                        onEdit = { campaignToEdit = it },
                        onDelete = { campaignToDelete = it }
                    )
                    HorizontalDivider()
                }
            }
        }

        if (showAddDialog) {
            CampaignDialog(
                title = "New Campaign",
                confirmText = "Create",
                onDismiss = { showAddDialog = false },
                onConfirm = { name, desc ->
                    viewModel.addCampaign(name, desc)
                    showAddDialog = false
                }
            )
        }

        campaignToEdit?.let { campaign ->
            CampaignDialog(
                title = "Edit Campaign",
                confirmText = "Save",
                initialName = campaign.name,
                initialDescription = campaign.description,
                onDismiss = { campaignToEdit = null },
                onConfirm = { name, desc ->
                    viewModel.updateCampaign(campaign.copy(name = name, description = desc))
                    campaignToEdit = null
                }
            )
        }

        campaignToDelete?.let { campaign ->
            AlertDialog(
                onDismissRequest = { campaignToDelete = null },
                title = { Text("Delete Campaign") },
                text = { Text("Are you sure you want to delete '${campaign.name}'? All notes and categories in this campaign will be permanently deleted.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteCampaign(campaign)
                            campaignToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { campaignToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun CampaignDialog(
    title: String,
    confirmText: String,
    initialName: String = "",
    initialDescription: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Campaign Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, description) }) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CampaignItem(
    campaign: Campaign,
    onClick: () -> Unit,
    onEdit: (Campaign) -> Unit,
    onDelete: (Campaign) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = campaign.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (campaign.description.isNotBlank()) {
                Text(
                    text = campaign.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Text(
                text = "Created: ${dateFormat.format(Date(campaign.created))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        IconButton(onClick = { onEdit(campaign) }) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Campaign",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        }

        IconButton(onClick = { onDelete(campaign) }) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Campaign",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
            )
        }
    }
}
