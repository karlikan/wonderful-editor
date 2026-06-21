package com.feige.wonderfuleditor.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.feige.wonderfuleditor.model.Document
import com.feige.wonderfuleditor.model.DocumentBlock
import com.feige.wonderfuleditor.model.PageNumberConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class Exporters(private val context: Context) {

    // Вспомогательный метод для получения шрифта
    private fun getTypeface(fontName: String?, isBold: Boolean, isItalic: Boolean): Typeface {
        val style = when {
            isBold && isItalic -> Typeface.BOLD_ITALIC
            isBold -> Typeface.BOLD
            isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        
        if (fontName == null) return Typeface.create(Typeface.DEFAULT, style)
        
        // Системные шрифты
        when (fontName.lowercase()) {
            "serif" -> return Typeface.create(Typeface.SERIF, style)
            "sans", "sans-serif" -> return Typeface.create(Typeface.SANS_SERIF, style)
            "monospace" -> return Typeface.create(Typeface.MONOSPACE, style)
        }
        
        // Кастомные шрифты из папки fonts/
        val fontsDir = File(context.filesDir, "fonts")
        val fontFile = File(fontsDir, fontName)
        if (fontFile.exists()) {
            try {
                val tf = Typeface.createFromFile(fontFile)
                return Typeface.create(tf, style)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return Typeface.create(Typeface.DEFAULT, style)
    }

    // ==========================================
    // 1. EXPORT TO TXT
    // ==========================================
    fun exportToTxt(doc: Document, outputFile: File) {
        val sb = StringBuilder()
        sb.append(doc.metadata.title).append("\n\n")
        
        fun appendBlocks(blocks: List<DocumentBlock>) {
            for (block in blocks) {
                when (block.type) {
                    "columns" -> {
                        sb.append("--- [КОЛОНКА СЛЕВА] ---\n")
                        appendBlocks(block.leftColumn)
                        sb.append("--- [КОЛОНКА СПРАВА] ---\n")
                        appendBlocks(block.rightColumn)
                        sb.append("------------------------\n")
                    }
                    "image" -> {
                        sb.append("[Изображение: ").append(block.imagePath ?: "нет").append("]\n")
                    }
                    else -> {
                        sb.append(block.text).append("\n")
                    }
                }
            }
        }
        
        appendBlocks(doc.blocks)
        outputFile.writeText(sb.toString())
    }

    // ==========================================
    // 2. EXPORT TO HTML
    // ==========================================
    fun exportToHtml(doc: Document, outputFile: File) {
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>${doc.metadata.title}</title>")
        sb.append("<style>")
        sb.append("body { font-family: sans-serif; padding: 40px; line-height: 1.5; color: #333; }")
        sb.append(".h1 { font-size: 2em; margin-bottom: 0.5em; font-weight: bold; }")
        sb.append(".h2 { font-size: 1.5em; margin-top: 1em; margin-bottom: 0.5em; font-weight: bold; }")
        sb.append(".h3 { font-size: 1.2em; margin-top: 1em; margin-bottom: 0.5em; font-weight: bold; }")
        sb.append(".paragraph { margin-bottom: 1em; }")
        sb.append(".columns-container { display: flex; gap: 20px; margin: 20px 0; }")
        sb.append(".column { flex: 1; }")
        sb.append(".image-container { text-align: center; margin: 20px 0; }")
        sb.append(".image-container img { max-width: 100%; height: auto; }")
        sb.append("</style></head><body>")
        
        sb.append("<h1 style=\"text-align:center;\">${doc.metadata.title}</h1><hr/>")
        
        fun buildBlockHtml(block: DocumentBlock, builder: StringBuilder) {
            val style = block.style
            val inlineCss = StringBuilder()
            inlineCss.append("font-size: ${style.fontSize}px; ")
            inlineCss.append("color: ${style.color}; ")
            if (style.letterSpacing != 0f) inlineCss.append("letter-spacing: ${style.letterSpacing}px; ")
            inlineCss.append("line-height: ${style.lineHeight}px; ")
            if (style.isBold) inlineCss.append("font-weight: bold; ")
            if (style.isItalic) inlineCss.append("font-style: italic; ")
            
            val textDec = mutableListOf<String>()
            if (style.isUnderline) textDec.add("underline")
            if (style.isStrikethrough) textDec.add("line-through")
            if (textDec.isNotEmpty()) inlineCss.append("text-decoration: ${textDec.joinToString(" ")}; ")
            
            inlineCss.append("text-align: ${style.alignment.lowercase()}; ")
            if (style.indent > 0) inlineCss.append("padding-left: ${style.indent}px; ")
            
            if (style.fontFamily != null) {
                inlineCss.append("font-family: '${style.fontFamily}', sans-serif; ")
            }

            when (block.type) {
                "h1" -> builder.append("<div class=\"h1\" style=\"$inlineCss\">${block.text}</div>")
                "h2" -> builder.append("<div class=\"h2\" style=\"$inlineCss\">${block.text}</div>")
                "h3" -> builder.append("<div class=\"h3\" style=\"$inlineCss\">${block.text}</div>")
                "paragraph" -> builder.append("<p class=\"paragraph\" style=\"$inlineCss\">${block.text}</p>")
                "image" -> {
                    val align = when (block.imageAlignment) {
                        "LEFT" -> "left"
                        "RIGHT" -> "right"
                        else -> "center"
                    }
                    builder.append("<div class=\"image-container\" style=\"text-align: $align;\">")
                    builder.append("<img src=\"file://${block.imagePath}\" style=\"width: ${block.imageSizePercent}%;\" />")
                    builder.append("</div>")
                }
                "columns" -> {
                    builder.append("<div class=\"columns-container\">")
                    builder.append("<div class=\"column\">")
                    for (b in block.leftColumn) {
                        buildBlockHtml(b, builder)
                    }
                    builder.append("</div>")
                    builder.append("<div class=\"column\">")
                    for (b in block.rightColumn) {
                        buildBlockHtml(b, builder)
                    }
                    builder.append("</div>")
                    builder.append("</div>")
                }
            }
        }

        for (block in doc.blocks) {
            buildBlockHtml(block, sb)
        }
        
        sb.append("</body></html>")
        outputFile.writeText(sb.toString())
    }

    // ==========================================
    // 3. EXPORT TO DOCX (ZIP with XML files)
    // ==========================================
    fun exportToDocx(doc: Document, outputFile: File) {
        val byteOS = ByteArrayOutputStream()
        val zipOS = ZipOutputStream(byteOS)
        
        // 1. [Content_Types].xml
        zipOS.putNextEntry(ZipEntry("[Content_Types].xml"))
        zipOS.write(getContentTypesXml().toByteArray(Charsets.UTF_8))
        zipOS.closeEntry()
        
        // 2. _rels/.rels
        zipOS.putNextEntry(ZipEntry("_rels/.rels"))
        zipOS.write(getRelsXml().toByteArray(Charsets.UTF_8))
        zipOS.closeEntry()
        
        // 3. word/_rels/document.xml.rels
        val imageFiles = mutableListOf<String>()
        fun collectImages(blocks: List<DocumentBlock>) {
            for (block in blocks) {
                if (block.type == "image" && !block.imagePath.isNullOrEmpty()) {
                    imageFiles.add(block.imagePath!!)
                } else if (block.type == "columns") {
                    collectImages(block.leftColumn)
                    collectImages(block.rightColumn)
                }
            }
        }
        collectImages(doc.blocks)
        
        zipOS.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
        zipOS.write(getDocumentRelsXml(imageFiles).toByteArray(Charsets.UTF_8))
        zipOS.closeEntry()
        
        // 4. word/styles.xml
        zipOS.putNextEntry(ZipEntry("word/styles.xml"))
        zipOS.write(getStylesXml().toByteArray(Charsets.UTF_8))
        zipOS.closeEntry()
        
        // 5. word/document.xml
        zipOS.putNextEntry(ZipEntry("word/document.xml"))
        zipOS.write(getDocumentXml(doc, imageFiles).toByteArray(Charsets.UTF_8))
        zipOS.closeEntry()
        
        // 6. word/media/ - write actual images
        imageFiles.forEachIndexed { index, path ->
            val file = File(path)
            if (file.exists()) {
                zipOS.putNextEntry(ZipEntry("word/media/image_${index + 1}.png"))
                file.inputStream().use { input ->
                    input.copyTo(zipOS)
                }
                zipOS.closeEntry()
            }
        }
        
        zipOS.close()
        
        FileOutputStream(outputFile).use { fos ->
            fos.write(byteOS.toByteArray())
        }
    }

    private fun getContentTypesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Default Extension="png" ContentType="image/png"/>
    <Default Extension="jpeg" ContentType="image/jpeg"/>
    <Default Extension="jpg" ContentType="image/jpeg"/>
    <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
    <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>"""
    }

    private fun getRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
    }

    private fun getDocumentRelsXml(images: List<String>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
        
        images.forEachIndexed { index, _ ->
            sb.append("""
    <Relationship Id="rIdImg_${index + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/image_${index + 1}.png"/>""")
        }
        
        sb.append("\n</Relationships>")
        return sb.toString()
    }

    private fun getStylesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
    <w:docDefaults>
        <w:rPrDefault>
            <w:rPr>
                <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/>
                <w:sz w:val="22"/>
            </w:rPr>
        </w:rPrDefault>
    </w:docDefaults>
</w:styles>"""
    }

    private fun getDocumentXml(doc: Document, images: List<String>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
    <w:body>""")

        // Добавим заголовок документа
        sb.append("""
        <w:p>
            <w:pPr>
                <w:jc w:val="center"/>
            </w:pPr>
            <w:r>
                <w:rPr>
                    <w:b/>
                    <w:sz w:val="36"/>
                </w:rPr>
                <w:t>${escapeXml(doc.metadata.title)}</w:t>
            </w:r>
        </w:p>
        <w:p/>
        """)

        fun writeBlockXml(block: DocumentBlock) {
            val style = block.style
            
            // Свойства абзаца
            val pPr = StringBuilder()
            val jcVal = when (style.alignment.uppercase()) {
                "CENTER" -> "center"
                "RIGHT" -> "right"
                "JUSTIFY" -> "both"
                else -> "left"
            }
            pPr.append("<w:jc w:val=\"$jcVal\"/>")
            
            if (style.indent > 0) {
                // В Word отступ измеряется в двадцатых долях пункта (dxa). 1dp приблизительно 20 dxa.
                val dxa = (style.indent * 20).toInt()
                pPr.append("<w:ind w:left=\"$dxa\"/>")
            }
            
            // Межстрочный интервал
            // 240 dxa = 1.0 строка. lineRule="auto" означает кратный интервал.
            val wordLineSpacing = (style.lineHeight / style.fontSize * 240).toInt()
            pPr.append("<w:spacing w:line=\"$wordLineSpacing\" w:lineRule=\"auto\"/>")

            // Свойства прогона (текста)
            val rPr = StringBuilder()
            if (style.isBold) rPr.append("<w:b/>")
            if (style.isItalic) rPr.append("<w:i/>")
            if (style.isUnderline) rPr.append("<w:u w:val=\"single\"/>")
            if (style.isStrikethrough) rPr.append("<w:strike/>")
            
            // Цвет
            val colorHex = style.color.replace("#", "")
            rPr.append("<w:color w:val=\"$colorHex\"/>")
            
            // Размер шрифта в полупунктах (pt * 2)
            val wordSz = (style.fontSize * 2).toInt()
            rPr.append("<w:sz w:val=\"$wordSz\"/>")

            // Название шрифта
            val fontName = style.fontFamily ?: "Calibri"
            rPr.append("<w:rFonts w:ascii=\"$fontName\" w:hAnsi=\"$fontName\"/>")

            when (block.type) {
                "h1", "h2", "h3", "paragraph" -> {
                    val tag = when(block.type) {
                        "h1" -> "<w:rPr><w:b/><w:sz w:val=\"32\"/><w:rFonts w:ascii=\"$fontName\" w:hAnsi=\"$fontName\"/></w:rPr>"
                        "h2" -> "<w:rPr><w:b/><w:sz w:val=\"28\"/><w:rFonts w:ascii=\"$fontName\" w:hAnsi=\"$fontName\"/></w:rPr>"
                        "h3" -> "<w:rPr><w:b/><w:sz w:val=\"24\"/><w:rFonts w:ascii=\"$fontName\" w:hAnsi=\"$fontName\"/></w:rPr>"
                        else -> "<w:rPr>$rPr</w:rPr>"
                    }
                    sb.append("""
        <w:p>
            <w:pPr>$pPr</w:pPr>
            <w:r>
                $tag
                <w:t>${escapeXml(block.text)}</w:t>
            </w:r>
        </w:p>""")
                }
                "image" -> {
                    if (!block.imagePath.isNullOrEmpty()) {
                        val imgIndex = images.indexOf(block.imagePath)
                        if (imgIndex != -1) {
                            val rId = "rIdImg_${imgIndex + 1}"
                            val widthEmu = (914400 * 5 * (block.imageSizePercent / 100f)).toLong() // 1 inch = 914400 EMU
                            val heightEmu = (914400 * 3 * (block.imageSizePercent / 100f)).toLong()
                            
                            val imgJc = when(block.imageAlignment) {
                                "CENTER" -> "center"
                                "RIGHT" -> "right"
                                else -> "left"
                            }
                            sb.append("""
        <w:p>
            <w:pPr><w:jc w:val="$imgJc"/></w:pPr>
            <w:r>
                <w:drawing>
                    <wp:inline distT="0" distB="0" distL="0" distR="0">
                        <wp:extent cx="$widthEmu" cy="$heightEmu"/>
                        <wp:docPr id="${imgIndex + 1}" name="Image_${imgIndex + 1}"/>
                        <wp:cNvGraphicFramePr>
                            <a:graphicFrameLocks noChangeAspect="1"/>
                        </wp:cNvGraphicFramePr>
                        <a:graphic>
                            <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
                                <pic:pic>
                                    <pic:nvPicPr>
                                        <pic:cNvPr id="${imgIndex + 1}" name="image_${imgIndex + 1}.png"/>
                                        <pic:cNvPicPr/>
                                    </pic:nvPicPr>
                                    <pic:blipFill>
                                        <a:blip r:embed="$rId"/>
                                        <a:stretch>
                                            <a:fillRect/>
                                        </a:stretch>
                                    </pic:blipFill>
                                    <pic:spPr>
                                        <a:xfrm>
                                            <a:off x="0" y="0"/>
                                            <a:ext cx="$widthEmu" cy="$heightEmu"/>
                                        </a:xfrm>
                                        <a:prstGeom prst="rect">
                                            <a:avLst/>
                                        </a:prstGeom>
                                    </pic:spPr>
                                </pic:pic>
                            </a:graphicData>
                        </a:graphic>
                    </wp:inline>
                </w:drawing>
            </w:r>
        </w:p>""")
                        }
                    }
                }
                "columns" -> {
                    // Колонки оборачиваем в невидимую таблицу с 2 ячейками
                    sb.append("""
        <w:tbl>
            <w:tblPr>
                <w:tblW w:w="5000" w:type="pct"/>
                <w:tblBorders>
                    <w:top w:val="none"/><w:left w:val="none"/><w:bottom w:val="none"/><w:right w:val="none"/>
                    <w:insideH w:val="none"/><w:insideV w:val="none"/>
                </w:tblBorders>
            </w:tblPr>
            <w:tr>
                <w:tc>
                    <w:tcPr><w:tcW w:w="2500" w:type="pct"/></w:tcPr>""")
                    
                    for (b in block.leftColumn) {
                        writeBlockXml(b)
                    }
                    
                    sb.append("""
                </w:tc>
                <w:tc>
                    <w:tcPr><w:tcW w:w="2500" w:type="pct"/></w:tcPr>""")
                    
                    for (b in block.rightColumn) {
                        writeBlockXml(b)
                    }
                    
                    sb.append("""
                </w:tc>
            </w:tr>
        </w:tbl>
        <w:p/>""") // Пустой абзац после таблицы
                }
            }
        }

        for (block in doc.blocks) {
            writeBlockXml(block)
        }

        sb.append("""
    </w:body>
</w:document>""")
        return sb.toString()
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    // ==========================================
    // 4. EXPORT TO PDF (Standard Canvas rendering)
    // ==========================================
    fun exportToPdf(doc: Document, outputFile: File) {
        val pdfDoc = PdfDocument()
        
        // Размеры страницы A4: 595 x 842 points
        val pageWidth = 595
        val pageHeight = 842
        val margin = 50f
        val printableWidth = pageWidth - (margin * 2)
        val printableHeight = pageHeight - (margin * 2)

        var pageNum = 1
        var currentY = margin

        // Инициализируем первую страницу
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        var page = pdfDoc.startPage(pageInfo)
        var canvas = page.canvas

        fun checkNewPage(neededHeight: Float) {
            if (currentY + neededHeight > pageHeight - margin) {
                // Рисуем номер страницы перед закрытием
                drawPageNumber(canvas, pageNum, doc.metadata.pageNumberConfig, pageWidth, pageHeight, margin)
                
                pdfDoc.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                page = pdfDoc.startPage(pageInfo)
                canvas = page.canvas
                currentY = margin
            }
        }

        fun renderBlocks(blocks: List<DocumentBlock>, xStart: Float, width: Float) {
            for (block in blocks) {
                when (block.type) {
                    "image" -> {
                        if (!block.imagePath.isNullOrEmpty()) {
                            val bitmap = BitmapFactory.decodeFile(block.imagePath)
                            if (bitmap != null) {
                                val scale = (width * (block.imageSizePercent / 100f)) / bitmap.width
                                val drawWidth = bitmap.width * scale
                                val drawHeight = bitmap.height * scale
                                
                                checkNewPage(drawHeight + 10f)
                                
                                val drawX = when (block.imageAlignment) {
                                    "CENTER" -> xStart + (width - drawWidth) / 2
                                    "RIGHT" -> xStart + width - drawWidth
                                    else -> xStart
                                }
                                
                                canvas.drawBitmap(bitmap, null, android.graphics.RectF(drawX, currentY, drawX + drawWidth, currentY + drawHeight), null)
                                currentY += drawHeight + 15f
                                bitmap.recycle()
                            }
                        }
                    }
                    "columns" -> {
                        val colWidth = (width - 15f) / 2f
                        
                        // Временно сохраняем Y-координату
                        val startY = currentY
                        
                        // Рендерим левую колонку на виртуальном холсте или высчитываем высоту заранее.
                        // Для простоты: рендерим левую, определяем высоту, затем рендерим правую.
                        // Чтобы не накладывалось на холст, мы можем временно рисовать левую колонку и запоминать высоту, 
                        // но в PdfDocument проще нарисовать одну колонку, вернуть Y, нарисовать вторую.
                        // Для этого сперва измерим высоту левой колонки
                        val savedY = currentY
                        
                        // Рендерим левую
                        renderBlocks(block.leftColumn, xStart, colWidth)
                        val leftHeight = currentY - savedY
                        
                        // Рендерим правую
                        currentY = savedY
                        renderBlocks(block.rightColumn, xStart + colWidth + 15f, colWidth)
                        val rightHeight = currentY - savedY
                        
                        // Устанавливаем Y как максимум высот
                        currentY = startY + maxOf(leftHeight, rightHeight) + 15f
                    }
                    else -> { // h1, h2, h3, paragraph
                        val textPaint = TextPaint().apply {
                            color = Color.parseColor(block.style.color)
                            textSize = when (block.type) {
                                "h1" -> 24f
                                "h2" -> 20f
                                "h3" -> 18f
                                else -> block.style.fontSize
                            }
                            isUnderlineText = block.style.isUnderline
                            isStrikeThruText = block.style.isStrikethrough
                            typeface = getTypeface(block.style.fontFamily, block.style.isBold, block.style.isItalic)
                            
                            if (block.style.letterSpacing != 0f) {
                                letterSpacing = block.style.letterSpacing / textSize
                            }
                        }

                        val alignment = when (block.style.alignment.uppercase()) {
                            "CENTER" -> Layout.Alignment.ALIGN_CENTER
                            "RIGHT" -> Layout.Alignment.ALIGN_OPPOSITE
                            else -> Layout.Alignment.ALIGN_NORMAL
                        }

                        // Настройка отступа
                        val blockX = xStart + block.style.indent
                        val blockWidth = width - block.style.indent

                        // lineSpacingMultiplier = lineHeight / fontSize
                        val spacingMult = block.style.lineHeight / block.style.fontSize
                        
                        val layoutBuilder = StaticLayout.Builder.obtain(block.text, 0, block.text.length, textPaint, blockWidth.toInt())
                            .setAlignment(alignment)
                            .setLineSpacing(0f, spacingMult)
                            
                        // Выравнивание по ширине (Justify) на Android 26+
                        if (block.style.alignment.uppercase() == "JUSTIFY") {
                            layoutBuilder.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD)
                        }

                        val staticLayout = layoutBuilder.build()
                        val height = staticLayout.height.toFloat()

                        checkNewPage(height + 10f)

                        canvas.save()
                        canvas.translate(blockX, currentY)
                        staticLayout.draw(canvas)
                        canvas.restore()

                        currentY += height + 12f
                    }
                }
            }
        }

        // Рендерим основные блоки документа
        renderBlocks(doc.blocks, margin, printableWidth)

        // Рисуем номер последней страницы перед завершением
        drawPageNumber(canvas, pageNum, doc.metadata.pageNumberConfig, pageWidth, pageHeight, margin)
        
        pdfDoc.finishPage(page)

        FileOutputStream(outputFile).use { fos ->
            pdfDoc.writeTo(fos)
        }
        
        pdfDoc.close()
    }

    private fun drawPageNumber(
        canvas: Canvas, 
        pageNum: Int, 
        config: PageNumberConfig, 
        pageWidth: Int, 
        pageHeight: Int, 
        margin: Float
    ) {
        if (!config.enabled || config.position == "NONE") return
        
        val paint = Paint().apply {
            color = Color.parseColor(config.color)
            textSize = config.fontSize
            typeface = getTypeface(config.fontFamily, config.isBold, config.isItalic)
            isAntiAlias = true
        }

        val text = pageNum.toString()
        val bounds = android.graphics.Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val textWidth = bounds.width().toFloat()
        val textHeight = bounds.height().toFloat()

        var x = margin
        var y = margin

        when (config.position.uppercase()) {
            "TOP_LEFT" -> {
                x = margin
                y = margin + textHeight
            }
            "TOP_RIGHT" -> {
                x = pageWidth - margin - textWidth
                y = margin + textHeight
            }
            "BOTTOM_LEFT" -> {
                x = margin
                y = pageHeight - margin
            }
            "BOTTOM_RIGHT" -> {
                x = pageWidth - margin - textWidth
                y = pageHeight - margin
            }
        }

        canvas.drawText(text, x, y, paint)
    }
}
