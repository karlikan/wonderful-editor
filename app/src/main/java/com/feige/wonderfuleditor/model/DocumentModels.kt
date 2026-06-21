package com.feige.wonderfuleditor.model

import java.util.UUID

data class TextStyle(
    var fontSize: Float = 16f,
    var letterSpacing: Float = 0f, // в sp
    var lineHeight: Float = 24f, // в sp
    var fontFamily: String? = null, // null = По умолчанию, "Serif", "Sans", "Monospace", или имя кастомного шрифта
    var color: String = "#000000", // Hex цвет
    var isBold: Boolean = false,
    var isItalic: Boolean = false,
    var isUnderline: Boolean = false,
    var isStrikethrough: Boolean = false,
    var alignment: String = "LEFT", // LEFT, CENTER, RIGHT, JUSTIFY
    var indent: Float = 0f // отступ слева в dp
)

data class DocumentBlock(
    val id: String = UUID.randomUUID().toString(),
    var type: String = "paragraph", // "h1", "h2", "h3", "paragraph", "image", "columns"
    var text: String = "",
    var style: TextStyle = TextStyle(),
    
    // Для ImageBlock
    var imagePath: String? = null,
    var imageAlignment: String = "CENTER", // LEFT, CENTER, RIGHT
    var imageSizePercent: Int = 100,
    
    // Для ColumnBlock (две колонки)
    var leftColumn: List<DocumentBlock> = emptyList(),
    var rightColumn: List<DocumentBlock> = emptyList()
)

data class PageNumberConfig(
    var enabled: Boolean = false,
    var position: String = "NONE", // NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    var fontSize: Float = 12f,
    var color: String = "#777777",
    var fontFamily: String? = null,
    var isBold: Boolean = false,
    var isItalic: Boolean = false
)

data class DocumentMetadata(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var fileName: String = "",
    val createdTime: Long = System.currentTimeMillis(),
    var modifiedTime: Long = System.currentTimeMillis(),
    var wordCount: Int = 0,
    var characterCount: Int = 0,
    var pageNumberConfig: PageNumberConfig = PageNumberConfig()
)

data class Document(
    var metadata: DocumentMetadata = DocumentMetadata(),
    var blocks: List<DocumentBlock> = listOf(DocumentBlock(type = "paragraph"))
)
