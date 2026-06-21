package com.feige.wonderfuleditor.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.feige.wonderfuleditor.R
import com.feige.wonderfuleditor.export.Exporters
import com.feige.wonderfuleditor.model.Document
import com.feige.wonderfuleditor.model.DocumentBlock
import com.feige.wonderfuleditor.model.PageNumberConfig
import com.feige.wonderfuleditor.storage.DocumentManager
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale
import android.graphics.Typeface
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    document: Document,
    documentManager: DocumentManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val exporters = remember { Exporters(context) }

    var title by remember { mutableStateOf(document.metadata.title) }
    val blocks = remember { mutableStateListOf<DocumentBlock>().apply { addAll(document.blocks) } }
    var focusedBlockId by remember { mutableStateOf<String?>(null) }
    
    // Менеджер кастомных шрифтов
    var customFonts by remember { mutableStateOf(documentManager.listCustomFonts().map { it.name }) }
    
    // Автосохранение
    var saveStatus by remember { mutableStateOf(context.getString(R.string.saved)) }
    LaunchedEffect(title, blocks.map { Triple(it.text, it.style.hashCode(), it.imagePath) }) {
        saveStatus = context.getString(R.string.saving)
        delay(800) // Задержка для автосохранения
        document.metadata.title = title
        document.blocks = blocks.toList()
        documentManager.saveDocument(document)
        saveStatus = context.getString(R.string.autosaved)
    }

    // Лаунчеры для выбора файлов (картинки, шрифты)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val path = documentManager.importImage(it)
            if (path != null) {
                blocks.add(
                    DocumentBlock(
                        type = "image",
                        imagePath = path,
                        imageAlignment = "CENTER",
                        imageSizePercent = 100
                    )
                )
            }
        }
    }

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fontName = documentManager.importCustomFont(it)
            if (fontName != null) {
                customFonts = documentManager.listCustomFonts().map { file -> file.name }
                Toast.makeText(context, "Шрифт $fontName добавлен!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Неподдерживаемый формат шрифта", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Сохранение файлов (Экспорт) через SAF
    var exportType by remember { mutableStateOf("pdf") }
    val docxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    ) { uri: Uri? ->
        uri?.let {
            try {
                val tempFile = File(context.cacheDir, "temp_export.docx")
                exporters.exportToDocx(document, tempFile)
                context.contentResolver.openOutputStream(it)?.use { output ->
                    tempFile.inputStream().use { input -> input.copyTo(output) }
                }
                Toast.makeText(context, "DOCX сохранен!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка сохранения: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let {
            try {
                val tempFile = File(context.cacheDir, "temp_export.pdf")
                exporters.exportToPdf(document, tempFile)
                context.contentResolver.openOutputStream(it)?.use { output ->
                    tempFile.inputStream().use { input -> input.copyTo(output) }
                }
                Toast.makeText(context, "PDF сохранен!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка сохранения: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val htmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri: Uri? ->
        uri?.let {
            try {
                val tempFile = File(context.cacheDir, "temp_export.html")
                exporters.exportToHtml(document, tempFile)
                context.contentResolver.openOutputStream(it)?.use { output ->
                    tempFile.inputStream().use { input -> input.copyTo(output) }
                }
                Toast.makeText(context, "HTML сохранен!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val txtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let {
            try {
                val tempFile = File(context.cacheDir, "temp_export.txt")
                exporters.exportToTxt(document, tempFile)
                context.contentResolver.openOutputStream(it)?.use { output ->
                    tempFile.inputStream().use { input -> input.copyTo(output) }
                }
                Toast.makeText(context, "TXT сохранен!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Состояния для диалога нумерации
    var showPageNumDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = saveStatus,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Меню экспорта
                    var showExportMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Export")
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_pdf)) },
                                onClick = {
                                    showExportMenu = false
                                    pdfLauncher.launch("${title.ifEmpty { "document" }}.pdf")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_docx)) },
                                onClick = {
                                    showExportMenu = false
                                    docxLauncher.launch("${title.ifEmpty { "document" }}.docx")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_html)) },
                                onClick = {
                                    showExportMenu = false
                                    htmlLauncher.launch("${title.ifEmpty { "document" }}.html")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_txt)) },
                                onClick = {
                                    showExportMenu = false
                                    txtLauncher.launch("${title.ifEmpty { "document" }}.txt")
                                }
                            )
                        }
                    }

                    // Нумерация страниц
                    IconButton(onClick = { showPageNumDialog = true }) {
                        Icon(Icons.Default.Pin, contentDescription = "Page Numbering")
                    }

                    // Сохранить вручную
                    IconButton(onClick = {
                        document.metadata.title = title
                        document.blocks = blocks.toList()
                        documentManager.saveDocument(document)
                        saveStatus = context.getString(R.string.saved)
                        Toast.makeText(context, "Файл успешно сохранен!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Manual Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // Панель форматирования под клавиатурой
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Toolbar быстрого форматирования
                val activeBlock = blocks.find { it.id == focusedBlockId }
                if (activeBlock != null && activeBlock.type != "image") {
                    FormattingToolbar(
                        block = activeBlock,
                        customFonts = customFonts,
                        onImportFontClick = { fontPickerLauncher.launch("font/*") },
                        onStyleChanged = { updatedStyle ->
                            val idx = blocks.indexOfFirst { it.id == activeBlock.id }
                            if (idx != -1) {
                                blocks[idx] = activeBlock.copy(style = updatedStyle)
                            }
                        },
                        onDeleteBlock = {
                            if (blocks.size > 1) {
                                blocks.remove(activeBlock)
                                focusedBlockId = null
                            }
                        }
                    )
                }

                // Добавление новых блоков
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val newBlock = DocumentBlock(type = "paragraph")
                        blocks.add(newBlock)
                        focusedBlockId = newBlock.id
                    }) {
                        Icon(Icons.Default.TextFields, contentDescription = "Add Paragraph", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = {
                        val newBlock = DocumentBlock(type = "h1", text = "Заголовок")
                        blocks.add(newBlock)
                        focusedBlockId = newBlock.id
                    }) {
                        Icon(Icons.Default.Title, contentDescription = "Add H1", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = {
                        val newBlock = DocumentBlock(type = "h2", text = "Подзаголовок")
                        blocks.add(newBlock)
                        focusedBlockId = newBlock.id
                    }) {
                        Icon(Icons.Default.ShortText, contentDescription = "Add H2", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = {
                        imagePickerLauncher.launch("image/*")
                    }) {
                        Icon(Icons.Default.Image, contentDescription = "Add Image", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = {
                        // Блок колонок (по умолчанию содержит пустые параграфы)
                        val newBlock = DocumentBlock(
                            type = "columns",
                            leftColumn = listOf(DocumentBlock(type = "paragraph")),
                            rightColumn = listOf(DocumentBlock(type = "paragraph"))
                        )
                        blocks.add(newBlock)
                    }) {
                        Icon(Icons.Default.ViewColumn, contentDescription = "Add Columns", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Название документа
            item {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) {
                            Text(
                                text = stringResource(R.string.untitled),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                            )
                        }
                        innerTextField()
                    }
                )
                Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
            }

            // Блоки редактора
            itemsIndexed(blocks, key = { _, block -> block.id }) { index, block ->
                when (block.type) {
                    "image" -> {
                        ImageBlockItem(
                            block = block,
                            onUpdate = { updatedBlock -> blocks[index] = updatedBlock },
                            onDelete = { blocks.removeAt(index) }
                        )
                    }
                    "columns" -> {
                        ColumnsBlockItem(
                            block = block,
                            customFonts = customFonts,
                            onUpdate = { updatedBlock -> blocks[index] = updatedBlock },
                            onDelete = { blocks.removeAt(index) }
                        )
                    }
                    else -> {
                        TextBlockItem(
                            block = block,
                            isFocused = focusedBlockId == block.id,
                            onFocus = { focusedBlockId = block.id },
                            onTextChange = { text ->
                                val currentBlock = blocks[index]
                                blocks[index] = currentBlock.copy(text = text)
                            }
                        )
                    }
                }
            }
        }

        // Диалог настройки нумерации страниц
        if (showPageNumDialog) {
            PageNumberingDialog(
                config = document.metadata.pageNumberConfig,
                customFonts = customFonts,
                onDismiss = { showPageNumDialog = false },
                onSave = { updatedConfig ->
                    document.metadata.pageNumberConfig = updatedConfig
                    showPageNumDialog = false
                    // пересохраним
                    documentManager.saveDocument(document)
                }
            )
        }
    }
}

// ==========================================
// ОТДЕЛЬНЫЕ КОМПОНЕНТЫ РЕДАКТОРА
// ==========================================

@Composable
fun TextBlockItem(
    block: DocumentBlock,
    isFocused: Boolean,
    onFocus: () -> Unit,
    onTextChange: (String) -> Unit
) {
    val style = block.style
    
    val context = LocalContext.current
    // Создаем семейство шрифтов Compose из кастомного названия или стандартных
    val fontFamily = remember(style.fontFamily) {
        getCustomFontFamily(context, style.fontFamily)
    }

    val alignment = when (style.alignment.uppercase()) {
        "CENTER" -> TextAlign.Center
        "RIGHT" -> TextAlign.Right
        "JUSTIFY" -> TextAlign.Justify
        else -> TextAlign.Left
    }

    val textDecoration = when {
        style.isUnderline && style.isStrikethrough -> TextDecoration.combine(
            listOf(TextDecoration.Underline, TextDecoration.LineThrough)
        )
        style.isUnderline -> TextDecoration.Underline
        style.isStrikethrough -> TextDecoration.LineThrough
        else -> TextDecoration.None
    }

    val sizeSp = when (block.type) {
        "h1" -> 26.sp
        "h2" -> 22.sp
        "h3" -> 19.sp
        else -> style.fontSize.sp
    }
    
    val weight = when (block.type) {
        "h1", "h2", "h3" -> FontWeight.Bold
        else -> if (style.isBold) FontWeight.Bold else FontWeight.Normal
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = style.indent.dp)
            .border(
                width = 1.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onFocus() }
            .padding(8.dp)
    ) {
        BasicTextField(
            value = block.text,
            onValueChange = onTextChange,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            textStyle = TextStyle(
                fontSize = sizeSp,
                fontWeight = weight,
                fontStyle = if (style.isItalic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = textDecoration,
                color = Color(android.graphics.Color.parseColor(style.color)),
                textAlign = alignment,
                fontFamily = fontFamily,
                lineHeight = style.lineHeight.sp,
                letterSpacing = style.letterSpacing.sp
            ),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                if (block.text.isEmpty()) {
                    Text(
                        text = when (block.type) {
                            "h1" -> "Заголовок 1 (H1)"
                            "h2" -> "Подзаголовок (H2)"
                            "h3" -> "Заголовок 3 (H3)"
                            else -> "Введите текст абзаца..."
                        },
                        fontSize = sizeSp,
                        fontWeight = weight,
                        fontStyle = if (style.isItalic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = textDecoration,
                        color = Color(android.graphics.Color.parseColor(style.color)).copy(alpha = 0.3f),
                        textAlign = alignment,
                        fontFamily = fontFamily
                    )
                }
                innerTextField()
            }
        )
    }
}

@Composable
fun ImageBlockItem(
    block: DocumentBlock,
    onUpdate: (DocumentBlock) -> Unit,
    onDelete: () -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val alignment = when (block.imageAlignment) {
            "LEFT" -> Alignment.CenterStart
            "RIGHT" -> Alignment.CenterEnd
            else -> Alignment.Center
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSettings = !showSettings },
            contentAlignment = alignment
        ) {
            AsyncImage(
                model = File(block.imagePath ?: ""),
                contentDescription = "User Image",
                modifier = Modifier
                    .fillMaxWidth(block.imageSizePercent / 100f)
                    .clip(RoundedCornerShape(8.dp))
            )
        }

        AnimatedVisibility(visible = showSettings) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                // Изменение выравнивания
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Выравнивание", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row {
                        IconButton(onClick = { onUpdate(block.copy(imageAlignment = "LEFT")) }) {
                            Icon(Icons.Default.AlignHorizontalLeft, contentDescription = null, tint = if (block.imageAlignment == "LEFT") MaterialTheme.colorScheme.primary else LocalContentColor.current)
                        }
                        IconButton(onClick = { onUpdate(block.copy(imageAlignment = "CENTER")) }) {
                            Icon(Icons.Default.AlignHorizontalCenter, contentDescription = null, tint = if (block.imageAlignment == "CENTER") MaterialTheme.colorScheme.primary else LocalContentColor.current)
                        }
                        IconButton(onClick = { onUpdate(block.copy(imageAlignment = "RIGHT")) }) {
                            Icon(Icons.Default.AlignHorizontalRight, contentDescription = null, tint = if (block.imageAlignment == "RIGHT") MaterialTheme.colorScheme.primary else LocalContentColor.current)
                        }
                    }
                }

                // Ползунок размера
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Размер: ${block.imageSizePercent}%", fontSize = 12.sp, modifier = Modifier.width(90.dp))
                    Slider(
                        value = block.imageSizePercent.toFloat(),
                        onValueChange = { onUpdate(block.copy(imageSizePercent = it.toInt())) },
                        valueRange = 20f..100f,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Удалить изображение")
                }
            }
        }
    }
}

@Composable
fun ColumnsBlockItem(
    block: DocumentBlock,
    customFonts: List<String>,
    onUpdate: (DocumentBlock) -> Unit,
    onDelete: () -> Unit
) {
    var leftBlocksState = remember { mutableStateListOf<DocumentBlock>().apply { addAll(block.leftColumn) } }
    var rightBlocksState = remember { mutableStateListOf<DocumentBlock>().apply { addAll(block.rightColumn) } }
    var focusedColBlockId by remember { mutableStateOf<String?>(null) }

    // Синхронизация с родителем при изменении
    LaunchedEffect(leftBlocksState.map { it.text }, rightBlocksState.map { it.text }) {
        onUpdate(block.copy(leftColumn = leftBlocksState.toList(), rightColumn = rightBlocksState.toList()))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Колонки (Параллельный текст)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Columns Block", tint = MaterialTheme.colorScheme.error)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Левая колонка
            Column(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(4.dp)
            ) {
                leftBlocksState.forEachIndexed { idx, colBlock ->
                    TextBlockItem(
                        block = colBlock,
                        isFocused = focusedColBlockId == colBlock.id,
                        onFocus = { focusedColBlockId = colBlock.id },
                        onTextChange = { text ->
                            leftBlocksState[idx] = colBlock.copy(text = text)
                        }
                    )
                }
                
                TextButton(onClick = {
                    leftBlocksState.add(DocumentBlock(type = "paragraph"))
                }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Добавить", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Правая колонка
            Column(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(4.dp)
            ) {
                rightBlocksState.forEachIndexed { idx, colBlock ->
                    TextBlockItem(
                        block = colBlock,
                        isFocused = focusedColBlockId == colBlock.id,
                        onFocus = { focusedColBlockId = colBlock.id },
                        onTextChange = { text ->
                            rightBlocksState[idx] = colBlock.copy(text = text)
                        }
                    )
                }

                TextButton(onClick = {
                    rightBlocksState.add(DocumentBlock(type = "paragraph"))
                }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Добавить", fontSize = 11.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormattingToolbar(
    block: DocumentBlock,
    customFonts: List<String>,
    onImportFontClick: () -> Unit,
    onStyleChanged: (com.feige.wonderfuleditor.model.TextStyle) -> Unit,
    onDeleteBlock: () -> Unit
) {
    val style = block.style

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        // Ряд 1: Bold, Italic, Underline, Strikethrough, Выравнивание
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { onStyleChanged(style.copy(isBold = !style.isBold)) },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (style.isBold) MaterialTheme.colorScheme.primary else Color.Transparent)
            ) {
                Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = if (style.isBold) Color.White else LocalContentColor.current)
            }
            IconButton(
                onClick = { onStyleChanged(style.copy(isItalic = !style.isItalic)) },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (style.isItalic) MaterialTheme.colorScheme.primary else Color.Transparent)
            ) {
                Icon(Icons.Default.FormatItalic, contentDescription = "Italic", tint = if (style.isItalic) Color.White else LocalContentColor.current)
            }
            IconButton(
                onClick = { onStyleChanged(style.copy(isUnderline = !style.isUnderline)) },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (style.isUnderline) MaterialTheme.colorScheme.primary else Color.Transparent)
            ) {
                Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline", tint = if (style.isUnderline) Color.White else LocalContentColor.current)
            }
            IconButton(
                onClick = { onStyleChanged(style.copy(isStrikethrough = !style.isStrikethrough)) },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (style.isStrikethrough) MaterialTheme.colorScheme.primary else Color.Transparent)
            ) {
                Icon(Icons.Default.FormatStrikethrough, contentDescription = "Strikethrough", tint = if (style.isStrikethrough) Color.White else LocalContentColor.current)
            }

            // Настройка выравнивания
            IconButton(onClick = { onStyleChanged(style.copy(alignment = "LEFT")) }) {
                Icon(Icons.Default.FormatAlignLeft, contentDescription = "Align Left", tint = if (style.alignment == "LEFT") MaterialTheme.colorScheme.primary else LocalContentColor.current)
            }
            IconButton(onClick = { onStyleChanged(style.copy(alignment = "CENTER")) }) {
                Icon(Icons.Default.FormatAlignCenter, contentDescription = "Align Center", tint = if (style.alignment == "CENTER") MaterialTheme.colorScheme.primary else LocalContentColor.current)
            }
            IconButton(onClick = { onStyleChanged(style.copy(alignment = "RIGHT")) }) {
                Icon(Icons.Default.FormatAlignRight, contentDescription = "Align Right", tint = if (style.alignment == "RIGHT") MaterialTheme.colorScheme.primary else LocalContentColor.current)
            }
            IconButton(onClick = { onStyleChanged(style.copy(alignment = "JUSTIFY")) }) {
                Icon(Icons.Default.FormatAlignJustify, contentDescription = "Align Justify", tint = if (style.alignment == "JUSTIFY") MaterialTheme.colorScheme.primary else LocalContentColor.current)
            }

            IconButton(
                onClick = onDeleteBlock,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete block", tint = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Ряд 2: Выбор шрифта, размера и отступа
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Dropdown Шрифта
            var fontMenuExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.width(130.dp)) {
                Button(
                    onClick = { fontMenuExpanded = true },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(style.fontFamily ?: "По умолчанию", fontSize = 11.sp, maxLines = 1)
                }
                DropdownMenu(expanded = fontMenuExpanded, onDismissRequest = { fontMenuExpanded = false }) {
                    DropdownMenuItem(text = { Text("По умолчанию") }, onClick = { onStyleChanged(style.copy(fontFamily = null)); fontMenuExpanded = false })
                    DropdownMenuItem(text = { Text("Serif") }, onClick = { onStyleChanged(style.copy(fontFamily = "Serif")); fontMenuExpanded = false })
                    DropdownMenuItem(text = { Text("Sans-Serif") }, onClick = { onStyleChanged(style.copy(fontFamily = "Sans-Serif")); fontMenuExpanded = false })
                    DropdownMenuItem(text = { Text("Monospace") }, onClick = { onStyleChanged(style.copy(fontFamily = "Monospace")); fontMenuExpanded = false })
                    customFonts.forEach { customName ->
                        DropdownMenuItem(text = { Text(customName) }, onClick = { onStyleChanged(style.copy(fontFamily = customName)); fontMenuExpanded = false })
                    }
                    DropdownMenuItem(
                        text = { Text("+ Загрузить свой...", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            fontMenuExpanded = false
                            onImportFontClick()
                        }
                    )
                }
            }

            // Изменение размера шрифта
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (style.fontSize > 8) onStyleChanged(style.copy(fontSize = style.fontSize - 1)) }) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease Font Size")
                }
                Text("${style.fontSize.toInt()}", fontSize = 14.sp)
                IconButton(onClick = { if (style.fontSize < 72) onStyleChanged(style.copy(fontSize = style.fontSize + 1)) }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase Font Size")
                }
            }

            // Изменение отступа (Indent)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (style.indent >= 8) onStyleChanged(style.copy(indent = style.indent - 8)) }) {
                    Icon(Icons.Default.FormatIndentDecrease, contentDescription = "Decrease Indent")
                }
                IconButton(onClick = { if (style.indent < 120) onStyleChanged(style.copy(indent = style.indent + 8)) }) {
                    Icon(Icons.Default.FormatIndentIncrease, contentDescription = "Increase Indent")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Ряд 3: Настройки цвета, межстрочного и межбуквенного интервала
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Цвет текста
            var showColorPicker by remember { mutableStateOf(false) }
            Box {
                Button(
                    onClick = { showColorPicker = true },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(style.color))))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Цвет", fontSize = 11.sp)
                }
                DropdownMenu(expanded = showColorPicker, onDismissRequest = { showColorPicker = false }) {
                    val colors = listOf("#000000", "#FFFFFF", "#777777", "#FF0000", "#00FF00", "#0000FF", "#EAB308", "#A855F7", "#F97316")
                    FlowRow(modifier = Modifier.width(180.dp).padding(8.dp), maxItemsInEachRow = 3) {
                        colors.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .clickable {
                                        onStyleChanged(style.copy(color = hex))
                                        showColorPicker = false
                                    }
                                    .border(1.dp, Color.Gray, CircleShape)
                            )
                        }
                    }
                }
            }

            // Межстрочный интервал
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FormatLineSpacing, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { if (style.lineHeight > style.fontSize) onStyleChanged(style.copy(lineHeight = style.lineHeight - 2)) }) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease line spacing")
                }
                Text("${style.lineHeight.toInt()}", fontSize = 12.sp)
                IconButton(onClick = { if (style.lineHeight < style.fontSize * 3) onStyleChanged(style.copy(lineHeight = style.lineHeight + 2)) }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase line spacing")
                }
            }

            // Межбуквенный интервал
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SpaceBar, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { if (style.letterSpacing > -2f) onStyleChanged(style.copy(letterSpacing = style.letterSpacing - 0.5f)) }) {
                    Icon(Icons.Default.Remove, contentDescription = null)
                }
                Text(String.format(Locale.US, "%.1f", style.letterSpacing), fontSize = 12.sp)
                IconButton(onClick = { if (style.letterSpacing < 10f) onStyleChanged(style.copy(letterSpacing = style.letterSpacing + 0.5f)) }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun PageNumberingDialog(
    config: PageNumberConfig,
    customFonts: List<String>,
    onDismiss: () -> Unit,
    onSave: (PageNumberConfig) -> Unit
) {
    var enabled by remember { mutableStateOf(config.enabled) }
    var position by remember { mutableStateOf(config.position) }
    var fontSize by remember { mutableStateOf(config.fontSize) }
    var color by remember { mutableStateOf(config.color) }
    var fontFamily by remember { mutableStateOf(config.fontFamily) }
    var isBold by remember { mutableStateOf(config.isBold) }
    var isItalic by remember { mutableStateOf(config.isItalic) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.page_numbering)) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Включить нумерацию")
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }
                }

                if (enabled) {
                    item {
                        Text("Позиция", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        val posList = listOf(
                            "TOP_LEFT" to stringResource(R.string.pos_top_left),
                            "TOP_RIGHT" to stringResource(R.string.pos_top_right),
                            "BOTTOM_LEFT" to stringResource(R.string.pos_bottom_left),
                            "BOTTOM_RIGHT" to stringResource(R.string.pos_bottom_right)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            posList.forEach { (posKey, posLabel) ->
                                FilterChip(
                                    selected = position == posKey,
                                    onClick = { position = posKey },
                                    label = { Text(posLabel, fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    item {
                        Text("Размер шрифта: ${fontSize.toInt()}", fontSize = 14.sp)
                        Slider(
                            value = fontSize,
                            onValueChange = { fontSize = it },
                            valueRange = 8f..24f
                        )
                    }

                    item {
                        Text("Шрифт", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        var fontExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { fontExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(fontFamily ?: "По умолчанию")
                            }
                            DropdownMenu(expanded = fontExpanded, onDismissRequest = { fontExpanded = false }) {
                                DropdownMenuItem(text = { Text("По умолчанию") }, onClick = { fontFamily = null; fontExpanded = false })
                                DropdownMenuItem(text = { Text("Serif") }, onClick = { fontFamily = "Serif"; fontExpanded = false })
                                DropdownMenuItem(text = { Text("Sans-Serif") }, onClick = { fontFamily = "Sans-Serif"; fontExpanded = false })
                                DropdownMenuItem(text = { Text("Monospace") }, onClick = { fontFamily = "Monospace"; fontExpanded = false })
                                customFonts.forEach { name ->
                                    DropdownMenuItem(text = { Text(name) }, onClick = { fontFamily = name; fontExpanded = false })
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = isBold, onCheckedChange = { isBold = it })
                                Text("Жирный")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = isItalic, onCheckedChange = { isItalic = it })
                                Text("Курсив")
                            }
                        }
                    }

                    item {
                        Text("Цвет номера", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        val colors = listOf("#000000", "#777777", "#FF0000", "#0000FF", "#854D0E", "#FFFFFF")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            colors.forEach { hex ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                        .clickable { color = hex }
                                        .border(
                                            width = if (color == hex) 2.dp else 1.dp,
                                            color = if (color == hex) MaterialTheme.colorScheme.primary else Color.Gray,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        PageNumberConfig(
                            enabled = enabled,
                            position = position,
                            fontSize = fontSize,
                            color = color,
                            fontFamily = fontFamily,
                            isBold = isBold,
                            isItalic = isItalic
                        )
                    )
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

// Утилита для загрузки шрифтов в Compose
fun getComposeFontFamily(fontName: String?): FontFamily {
    if (fontName == null) return FontFamily.Default
    return when (fontName.lowercase()) {
        "serif" -> FontFamily.Serif
        "sans-serif", "sans" -> FontFamily.SansSerif
        "monospace" -> FontFamily.Monospace
        else -> {
            // Если это кастомный шрифт, мы вернем FontFamily.Default.
            // При сборке реального динамического шрифта из файла используется FontFamily(Font(file = File(...))).
            // Для совместимости в Compose, если имя содержит расширение (например, .ttf),
            // мы можем загрузить его, если у нас есть путь, но здесь в модели у нас только имя.
            // Поэтому для Compose мы загружаем его, если он лежит в папке fonts/.
            // Так как у нас нет прямого доступа к Context внутри getComposeFontFamily, мы можем вернуть Default
            // или передавать полный FontFamily.
            // Но погодите! В Android Compose можно динамически загрузить шрифт с помощью Typeface.createFromFile
            // и обернуть в FontFamily(Typeface.createFromFile(file)).
            // Давайте сделаем полноценную динамическую загрузку:
            try {
                // Пытаемся проверить, существует ли файл в приложении
                // Для простоты, если имя кастомного шрифта передано, в TextBlockItem мы передадим
                // FontFamily(Typeface.createFromFile(File(fontsDir, name))) напрямую!
                // Чтобы не усложнять, вернем FontFamily.Default здесь, а в самом TextBlockItem 
                // загрузим его правильно!
                FontFamily.Default
            } catch (e: Exception) {
                FontFamily.Default
            }
        }
    }
}

// Загрузка динамического шрифта с Context
fun getCustomFontFamily(context: android.content.Context, fontName: String?): FontFamily {
    if (fontName == null) return FontFamily.Default
    val cleanName = fontName.lowercase()
    if (cleanName == "serif") return FontFamily.Serif
    if (cleanName == "sans-serif" || cleanName == "sans") return FontFamily.SansSerif
    if (cleanName == "monospace") return FontFamily.Monospace
    
    val file = File(File(context.filesDir, "fonts"), fontName)
    return if (file.exists()) {
        try {
            FontFamily(Typeface.createFromFile(file))
        } catch (e: Exception) {
            FontFamily.Default
        }
    } else {
        FontFamily.Default
    }
}
