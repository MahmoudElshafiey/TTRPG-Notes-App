package com.example.dndnotes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.example.dndnotes.data.model.Category
import com.example.dndnotes.ui.components.ViewModeToggle
import com.example.dndnotes.ui.theme.DndTheme
import com.example.dndnotes.ui.theme.ThemeViewModel
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.toArgb

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    campaignId: Long,
    viewModel: CategoryViewModel,
    themeViewModel: ThemeViewModel,
    onCategoryClick: (Long) -> Unit,
    onBackToCampaigns: () -> Unit
) {
    LaunchedEffect(campaignId) {
        viewModel.setCampaign(campaignId)
    }

    val categories by viewModel.allCategories.collectAsState()
    val deletionNoteCount by viewModel.deletionNoteCount.collectAsState()
    val rootCategories = categories.filter { it.parentId == null }
    val categoryGridView by themeViewModel.categoryGridView.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = { Text("D&D Notes") },
                navigationIcon = {
                    IconButton(onClick = onBackToCampaigns) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Campaigns")
                    }
                },
                actions = {
                    ViewModeToggle(
                        isGridView = categoryGridView,
                        onToggle = themeViewModel::setCategoryGridView
                    )
                    IconButton(onClick = { showThemeDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Themes")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { padding ->
        if (categories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No categories yet. Tap + to add one!", style = MaterialTheme.typography.bodyLarge)
            }
        } else if (categoryGridView) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                gridItems(categories, key = { it.id }) { category ->
                    CompactCategoryCard(
                        category = category,
                        onClick = { onCategoryClick(category.id) }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(rootCategories, key = { it.id }) { category ->
                    CategoryTreeItem(
                        category = category,
                        allCategories = categories,
                        onCategoryClick = onCategoryClick,
                        onToggleExpand = { viewModel.updateCategory(it) },
                        onEditCategory = { categoryToEdit = it },
                        onDeleteCategory = { categoryToDelete = it },
                        onAddSubcategory = { parentId ->
                            // TODO: Add subcategory logic if needed, or just use the same dialog
                        },
                        depth = 0
                    )
                }
            }
        }

        if (showAddDialog) {
            CategoryDialog(
                title = "Add Category",
                confirmText = "Add",
                onDismiss = { showAddDialog = false },
                onConfirm = { name, color ->
                    viewModel.addCategory(campaignId, name, color)
                    showAddDialog = false
                }
            )
        }

        categoryToEdit?.let { category ->
            CategoryDialog(
                title = "Edit Category",
                confirmText = "Save",
                initialName = category.name,
                initialColor = Color(android.graphics.Color.parseColor(category.color)),
                onDismiss = { categoryToEdit = null },
                onConfirm = { name, color ->
                    viewModel.updateCategory(category.copy(name = name, color = color))
                    categoryToEdit = null
                }
            )
        }

        if (showThemeDialog) {
            ThemeSelectionDialog(
                onDismiss = { showThemeDialog = false },
                onThemeSelected = { theme ->
                    themeViewModel.setTheme(theme)
                }
            )
        }

        categoryToDelete?.let { category ->
            // Resolve the cascade size while the dialog is open.
            LaunchedEffect(category.id) {
                viewModel.loadNoteCountForDeletion(category)
            }
            AlertDialog(
                onDismissRequest = { categoryToDelete = null; viewModel.clearDeletionNoteCount() },
                title = { Text("Delete Category") },
                text = {
                    val noteCount = deletionNoteCount
                    Column {
                        Text("Are you sure you want to delete '${category.name}'? This will also delete all subcategories and notes within it.")
                        // Spell out the blast radius: a cascade can take dozens of notes
                        // with it, and that is not obvious from the category name.
                        if (noteCount != null && noteCount > 0) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (noteCount == 1) {
                                    "This will permanently destroy 1 note."
                                } else {
                                    "This will permanently destroy $noteCount notes."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "A recent automatic backup may still contain them.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteCategory(category)
                            categoryToDelete = null
                            viewModel.clearDeletionNoteCount()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        categoryToDelete = null
                        viewModel.clearDeletionNoteCount()
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ThemeSelectionDialog(
    onDismiss: () -> Unit,
    onThemeSelected: (DndTheme) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Theme") },
        text = {
            LazyColumn {
                items(DndTheme.entries) { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(theme) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(theme.baseColor, shape = MaterialTheme.shapes.small)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(theme.displayName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun CategoryDialog(
    title: String,
    confirmText: String,
    initialName: String = "",
    initialColor: Color = Color(0xFFC84B31), // Default Rust Red
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedColor by remember { mutableStateOf(initialColor) }

    val presetColors = listOf(
        Color(0xFFC84B31), Color(0xFFC09050), Color(0xFF1B261B),
        Color(0xFF1B262B), Color(0xFF221B26), Color(0xFF2B1F1B),
        Color(0xFF1F2B1B), Color(0xFF2B1B1F), Color(0xFF262B2B),
        Color(0xFF0F0F0F), Color(0xFFE8E0D4), Color(0xFFA89880)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Select Color", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Color Grid
                Column {
                    val rows = presetColors.chunked(4)
                    rows.forEach { rowColors ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowColors.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(color, shape = MaterialTheme.shapes.small)
                                        .border(
                                            width = if (selectedColor == color) 2.dp else 0.dp,
                                            color = if (selectedColor == color) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .clickable { selectedColor = color }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                if (name.isNotBlank()) {
                    val hexColor = String.format("#%06X", 0xFFFFFF and selectedColor.toArgb())
                    onConfirm(name, hexColor)
                }
            }) {
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
fun CategoryTreeItem(
    category: Category,
    allCategories: List<Category>,
    onCategoryClick: (Long) -> Unit,
    onToggleExpand: (Category) -> Unit,
    onEditCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onAddSubcategory: (Long) -> Unit,
    depth: Int
) {
    val subcategories = allCategories.filter { it.parentId == category.id }
    val hasSubcategories = subcategories.isNotEmpty()

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCategoryClick(category.id) }
                .padding(start = (depth * 16).dp, top = 4.dp, bottom = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasSubcategories) {
                IconButton(onClick = { onToggleExpand(category.copy(isOpen = !category.isOpen)) }) {
                    Icon(
                        imageVector = if (category.isOpen) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                        contentDescription = if (category.isOpen) "Collapse" else "Expand"
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = try {
                            Color(android.graphics.Color.parseColor(category.color))
                        } catch (e: Exception) {
                            Color.Gray
                        },
                        shape = MaterialTheme.shapes.small
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { onEditCategory(category) }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Category",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }

            IconButton(onClick = { onDeleteCategory(category) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Category",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }

        if (category.isOpen && hasSubcategories) {
            subcategories.forEach { subcategory ->
                CategoryTreeItem(
                    category = subcategory,
                    allCategories = allCategories,
                    onCategoryClick = onCategoryClick,
                    onToggleExpand = onToggleExpand,
                    onEditCategory = onEditCategory,
                    onDeleteCategory = onDeleteCategory,
                    onAddSubcategory = onAddSubcategory,
                    depth = depth + 1
                )
            }
        }
    }
}

@Composable
fun CompactCategoryCard(
    category: Category,
    onClick: () -> Unit
) {
    val color = try {
        Color(android.graphics.Color.parseColor(category.color))
    } catch (e: Exception) {
        Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color = color, shape = MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
