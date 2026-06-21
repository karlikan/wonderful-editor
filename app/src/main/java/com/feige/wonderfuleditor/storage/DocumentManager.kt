package com.feige.wonderfuleditor.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.google.gson.Gson
import com.feige.wonderfuleditor.model.Document
import com.feige.wonderfuleditor.model.DocumentBlock
import com.feige.wonderfuleditor.model.DocumentMetadata
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class DocumentManager(private val context: Context) {
    private val gson = Gson()
    
    private val documentsDir = File(context.filesDir, "documents").apply { mkdirs() }
    private val trashDir = File(context.filesDir, "trash").apply { mkdirs() }
    val fontsDir = File(context.filesDir, "fonts").apply { mkdirs() }
    private val imagesDir = File(context.filesDir, "images").apply { mkdirs() }

    fun listDocuments(sortByNewest: Boolean = true): List<Document> {
        val files = documentsDir.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()
        val docs = files.mapNotNull { file ->
            try {
                gson.fromJson(file.readText(), Document::class.java)
            } catch (e: Exception) {
                null
            }
        }
        return if (sortByNewest) {
            docs.sortedByDescending { it.metadata.modifiedTime }
        } else {
            docs.sortedBy { it.metadata.modifiedTime }
        }
    }

    fun listTrash(): List<Document> {
        val files = trashDir.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()
        return files.mapNotNull { file ->
            try {
                gson.fromJson(file.readText(), Document::class.java)
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.metadata.modifiedTime }
    }

    fun saveDocument(doc: Document) {
        val (words, chars) = calculateStats(doc.blocks)
        doc.metadata.wordCount = words
        doc.metadata.characterCount = chars
        doc.metadata.modifiedTime = System.currentTimeMillis()
        
        val file = File(documentsDir, "${doc.metadata.id}.json")
        file.writeText(gson.toJson(doc))
    }

    fun renameDocument(doc: Document, newTitle: String) {
        doc.metadata.title = newTitle
        saveDocument(doc)
    }

    fun duplicateDocument(doc: Document, copySuffix: String): Document {
        val newId = UUID.randomUUID().toString()
        val duplicatedBlocks = duplicateBlocks(doc.blocks)
        
        val newMetadata = doc.metadata.copy(
            id = newId,
            title = "${doc.metadata.title}$copySuffix",
            createdTime = System.currentTimeMillis(),
            modifiedTime = System.currentTimeMillis()
        )
        
        val newDoc = Document(metadata = newMetadata, blocks = duplicatedBlocks)
        saveDocument(newDoc)
        return newDoc
    }

    private fun duplicateBlocks(blocks: List<DocumentBlock>): List<DocumentBlock> {
        return blocks.map { block ->
            block.copy(
                id = UUID.randomUUID().toString(),
                leftColumn = duplicateBlocks(block.leftColumn),
                rightColumn = duplicateBlocks(block.rightColumn),
                style = block.style.copy()
            )
        }
    }

    fun moveToTrash(doc: Document) {
        val sourceFile = File(documentsDir, "${doc.metadata.id}.json")
        if (sourceFile.exists()) {
            val destFile = File(trashDir, "${doc.metadata.id}.json")
            // Обновим время модификации при перемещении в корзину
            doc.metadata.modifiedTime = System.currentTimeMillis()
            destFile.writeText(gson.toJson(doc))
            sourceFile.delete()
        }
    }

    fun restoreFromTrash(doc: Document) {
        val sourceFile = File(trashDir, "${doc.metadata.id}.json")
        if (sourceFile.exists()) {
            val destFile = File(documentsDir, "${doc.metadata.id}.json")
            doc.metadata.modifiedTime = System.currentTimeMillis()
            destFile.writeText(gson.toJson(doc))
            sourceFile.delete()
        }
    }

    fun deletePermanently(docId: String) {
        val file = File(trashDir, "$docId.json")
        if (file.exists()) {
            file.delete()
        }
    }

    fun emptyTrash() {
        val files = trashDir.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()
        for (file in files) {
            file.delete()
        }
    }

    fun getGlobalStats(): Triple<Int, Int, Int> { // (filesCount, wordCount, charCount)
        val docs = listDocuments()
        val totalWords = docs.sumOf { it.metadata.wordCount }
        val totalChars = docs.sumOf { it.metadata.characterCount }
        return Triple(docs.size, totalWords, totalChars)
    }

    fun getLastEditedDocument(): Document? {
        return listDocuments(sortByNewest = true).firstOrNull()
    }

    // Вспомогательный метод для подсчета статистики слов и букв
    private fun calculateStats(blocks: List<DocumentBlock>): Pair<Int, Int> {
        var words = 0
        var chars = 0
        for (block in blocks) {
            if (block.type == "columns") {
                val (leftWords, leftChars) = calculateStats(block.leftColumn)
                val (rightWords, rightChars) = calculateStats(block.rightColumn)
                words += leftWords + rightWords
                chars += leftChars + rightChars
            } else if (block.type != "image") {
                val text = block.text
                chars += text.length
                val wordsList = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                words += wordsList.size
            }
        }
        return Pair(words, chars)
    }

    // Импорт шрифтов
    fun importCustomFont(uri: Uri): String? {
        val name = getFileName(uri) ?: "font_${UUID.randomUUID()}.ttf"
        if (!name.endsWith(".ttf", true) && !name.endsWith(".otf", true)) {
            return null
        }
        val destFile = File(fontsDir, name)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            return name
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun listCustomFonts(): List<File> {
        return fontsDir.listFiles { _, name -> name.endsWith(".ttf", true) || name.endsWith(".otf", true) }?.toList() ?: emptyList()
    }

    fun deleteCustomFont(fontName: String): Boolean {
        val file = File(fontsDir, fontName)
        return if (file.exists()) file.delete() else false
    }

    // Копирование картинки в приватную папку для персистентности
    fun importImage(uri: Uri): String? {
        val ext = getFileExtension(uri) ?: "jpg"
        val uniqueName = "${UUID.randomUUID()}.$ext"
        val destFile = File(imagesDir, uniqueName)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            return destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = cursor.getString(index)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.path
            val cut = name?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                name = name?.substring(cut + 1)
            }
        }
        return name
    }

    private fun getFileExtension(uri: Uri): String? {
        val contentResolver = context.contentResolver
        val type = contentResolver.getType(uri)
        if (type != null) {
            val parts = type.split("/")
            if (parts.size > 1) {
                return parts[1]
            }
        }
        val path = uri.path ?: return null
        val cut = path.lastIndexOf('.')
        return if (cut != -1) path.substring(cut + 1) else null
    }
}
