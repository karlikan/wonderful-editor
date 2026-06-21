package com.feige.wonderfuleditor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    documentManager: DocumentManager,
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    currentLang: String,
    onLangChange: (String) -> Unit,
    onOpenDocument: (Document) -> Unit,
    onCreateDocument: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var sortByNewest by remember { mutableStateOf(true) }
    var documents by remember { mutableStateOf(documentManager.listDocuments(sortByNewest)) }
    
    // Переменные состояния для диалогов
    var documentToRename by remember { mutableStateOf<Document?>(null) }
    var renameQuery by remember { mutableStateOf("") }
    
    var documentToDelete by remember { mutableStateOf<Document?>(null) }
    
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    fun refreshDocs() {
        documents = documentManager.listDocuments(sortByNewest)
    }

    // Обновляем список файлов при изменении сортировки
    LaunchedEffect(sortByNewest) {
        refreshDocs()
    }

    val filteredDocs = remember(documents, searchQuery) {
        if (searchQuery.isEmpty()) {
            documents
        } else {
            documents.filter { it.metadata.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Глобальная статистика для баннера
    val (filesCount, totalWords, totalChars) = remember(documents) {
        documentManager.getGlobalStats()
    }
    val lastEditedDoc = remember(documents) {
        documentManager.getLastEditedDocument()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.library),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ) 
                },
                actions = {
                    // Переключатель темы
                    var showThemeMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showThemeMenu = true }) {
                            Icon(Icons.Default.Palette, contentDescription = "Theme")
                        }
                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.theme_dark)) },
                                onClick = {
                                    onThemeChange("dark")
                                    showThemeMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.theme_light)) },
                                onClick = {
                                    onThemeChange("light")
                                    showThemeMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.theme_yellow)) },
                                onClick = {
                                    onThemeChange("yellow")
                                    showThemeMenu = false
                                }
                            )
                        }
                    }

                    // Переключатель языка
                    var showLangMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showLangMenu = true }) {
                            Icon(Icons.Default.Language, contentDescription = "Language")
                        }
                        DropdownMenu(
                            expanded = showLangMenu,
                            onDismissRequest = { showLangMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text("Русский") }, onClick = { onLangChange("ru"); showLangMenu = false })
                            DropdownMenuItem(text = { Text("English") }, onClick = { onLangChange("en"); showLangMenu = false })
                            DropdownMenuItem(text = { Text("Українська") }, onClick = { onLangChange("uk"); showLangMenu = false })
                        }
                    }

                    // Кнопка корзины
                    IconButton(onClick = onOpenTrash) {
                        Icon(Icons.Default.Delete, contentDescription = "Trash Bin")
                    }

                    // Кнопка о приложении
                    IconButton(onClick = onOpenAbout) {
                        Icon(Icons.Default.Info, contentDescription = "About")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateDocument,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Document")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Баннер со статистикой
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.stats_banner_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (lastEditedDoc != null) {
                            Text(
                                text = "${stringResource(R.string.stats_last_edit)}: \"${lastEditedDoc.metadata.title.ifEmpty { stringResource(R.string.untitled) }}\"",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = stringResource(R.string.stats_last_edit_time, dateFormatter.format(Date(lastEditedDoc.metadata.modifiedTime))),
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.stats_never_edited),
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color.White.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = stringResource(R.string.stats_total_files, filesCount), fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text(text = stringResource(R.string.stats_total_words, totalWords), fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text(text = stringResource(R.string.stats_total_chars, totalChars), fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Поиск и сортировка
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = { sortByNewest = !sortByNewest },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = if (sortByNewest) Icons.Default.SortByAlpha else Icons.Default.Sort,
                        contentDescription = "Sort Direction",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Список файлов
            if (filteredDocs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Файлов не найдено",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredDocs, key = { it.metadata.id }) { doc ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenDocument(doc) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = "Document Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = doc.metadata.title.ifEmpty { stringResource(R.string.untitled) },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = dateFormatter.format(Date(doc.metadata.modifiedTime)),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "${doc.metadata.wordCount} ${stringResource(R.string.words)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = "${doc.metadata.characterCount} ${stringResource(R.string.chars)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                // Кнопка действий
                                var showMenu by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.rename)) },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                            onClick = {
                                                documentToRename = doc
                                                renameQuery = doc.metadata.title
                                                showMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.duplicate)) },
                                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                            onClick = {
                                                val suffix = context.getString(R.string.duplicate_suffix)
                                                documentManager.duplicateDocument(doc, suffix)
                                                refreshDocs()
                                                showMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                documentToDelete = doc
                                                showMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Диалог переименования
        if (documentToRename != null) {
            AlertDialog(
                onDismissRequest = { documentToRename = null },
                title = { Text(stringResource(R.string.rename_dialog_title)) },
                text = {
                    OutlinedTextField(
                        value = renameQuery,
                        onValueChange = { renameQuery = it },
                        placeholder = { Text(stringResource(R.string.rename_dialog_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            documentToRename?.let { doc ->
                                documentManager.renameDocument(doc, renameQuery)
                            }
                            documentToRename = null
                            refreshDocs()
                        }
                    ) {
                        Text(stringResource(R.string.save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { documentToRename = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        // Диалог подтверждения удаления
        if (documentToDelete != null) {
            AlertDialog(
                onDismissRequest = { documentToDelete = null },
                title = { Text(stringResource(R.string.delete_confirm_title)) },
                text = { Text(stringResource(R.string.delete_confirm_message, documentToDelete?.metadata?.title ?: "")) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            documentToDelete?.let { doc ->
                                documentManager.moveToTrash(doc)
                            }
                            documentToDelete = null
                            refreshDocs()
                        }
                    ) {
                        Text(stringResource(R.string.yes), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { documentToDelete = null }) {
                        Text(stringResource(R.string.no))
                    }
                }
            )
        }
    }
}
