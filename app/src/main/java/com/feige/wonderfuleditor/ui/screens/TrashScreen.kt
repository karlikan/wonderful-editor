package com.feige.wonderfuleditor.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.wonderfuleditor.R
import com.feige.wonderfuleditor.model.Document
import com.feige.wonderfuleditor.storage.DocumentManager
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TrashScreen(
    documentManager: DocumentManager,
    onBack: () -> Unit
) {
    var trashList by remember { mutableStateOf(documentManager.listTrash()) }
    val selectedIds = remember { mutableStateListOf<String>() }
    
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showEmptyTrashConfirmation by remember { mutableStateOf(false) }
    
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    fun refreshTrash() {
        trashList = documentManager.listTrash()
        selectedIds.clear()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trash_bin)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (trashList.isNotEmpty()) {
                        IconButton(onClick = { showEmptyTrashConfirmation = true }) {
                            Icon(
                                Icons.Default.DeleteForever,
                                contentDescription = "Empty Trash",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (selectedIds.isNotEmpty()) {
                Surface(
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                selectedIds.forEach { id ->
                                    trashList.find { it.metadata.id == id }?.let { doc ->
                                        documentManager.restoreFromTrash(doc)
                                    }
                                }
                                refreshTrash()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.restore))
                        }

                        Button(
                            onClick = { showDeleteConfirmation = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f).padding(start = 8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.delete))
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            if (trashList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.trash_empty),
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(trashList, key = { it.metadata.id }) { doc ->
                        val isSelected = selectedIds.contains(doc.metadata.id)
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItemPlacement()
                                .combinedClickable(
                                    onClick = {
                                        if (selectedIds.isNotEmpty()) {
                                            if (isSelected) selectedIds.remove(doc.metadata.id)
                                            else selectedIds.add(doc.metadata.id)
                                        } else {
                                            // Однократный клик в обычном режиме выделяет файл
                                            selectedIds.add(doc.metadata.id)
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelected) {
                                            selectedIds.add(doc.metadata.id)
                                        }
                                    }
                                ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedIds.add(doc.metadata.id)
                                        else selectedIds.remove(doc.metadata.id)
                                    }
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = doc.metadata.title.ifEmpty { stringResource(R.string.untitled) },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Удален: ${dateFormatter.format(Date(doc.metadata.modifiedTime))}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = "${doc.metadata.wordCount} ${stringResource(R.string.words)}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                        Text(
                                            text = "${doc.metadata.characterCount} ${stringResource(R.string.chars)}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Диалог подтверждения удаления выбранного
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text(stringResource(R.string.delete_confirm_permanently)) },
                text = { Text(stringResource(R.string.delete_confirm_permanently_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            selectedIds.forEach { id ->
                                documentManager.deletePermanently(id)
                            }
                            showDeleteConfirmation = false
                            refreshTrash()
                        }
                    ) {
                        Text(stringResource(R.string.yes), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text(stringResource(R.string.no))
                    }
                }
            )
        }

        // Диалог очистки всей корзины
        if (showEmptyTrashConfirmation) {
            AlertDialog(
                onDismissRequest = { showEmptyTrashConfirmation = false },
                title = { Text("Очистить корзину?") },
                text = { Text("Вы уверены, что хотите навсегда удалить все файлы из корзины? Это действие нельзя отменить.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            documentManager.emptyTrash()
                            showEmptyTrashConfirmation = false
                            refreshTrash()
                        }
                    ) {
                        Text(stringResource(R.string.yes), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmptyTrashConfirmation = false }) {
                        Text(stringResource(R.string.no))
                    }
                }
            )
        }
    }
}
