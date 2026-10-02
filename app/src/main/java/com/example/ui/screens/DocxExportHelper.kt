package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocxExportHelper {

    /**
     * Converts a raw transcribed LaTeX document containing formulas and Bangla/English text
     * into a fully qualified, standards-compliant OpenXML Microsoft Word Document (.docx)
     */
    fun exportToDocx(context: Context, title: String, content: String): File? {
        val cacheDir = context.cacheDir
        val safeTitle = title.replace("[^a-zA-Z0-9_\\-\\s]".toRegex(), "").trim().replace("\\s+".toRegex(), "_")
        val docFile = File(cacheDir, "${if (safeTitle.isBlank()) "ScribeEdu_Doc" else safeTitle}.docx")
        
        try {
            ZipOutputStream(FileOutputStream(docFile)).use { zos ->
                // 1. Write [Content_Types].xml
                zos.putNextEntry(ZipEntry("[Content_Types].xml"))
                zos.write(getContentTypesXml().toByteArray())
                zos.closeEntry()

                // 2. Write _rels/.rels
                zos.putNextEntry(ZipEntry("_rels/.rels"))
                zos.write(getRelsXml().toByteArray())
                zos.closeEntry()

                // 3. Write word/_rels/document.xml.rels
                zos.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
                zos.write(getDocumentRelsXml().toByteArray())
                zos.closeEntry()

                // 4. Write word/document.xml (contains styled paragraphs and native OMML math tags!)
                zos.putNextEntry(ZipEntry("word/document.xml"))
                zos.write(getDocumentXml(title, content).toByteArray())
                zos.closeEntry()
            }
            return docFile
        } catch (e: Exception) {
            android.util.Log.e("DocxExportHelper", "Failed to compile DOCX zipped XML structure", e)
            return null
        }
    }

    private fun getContentTypesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""
    }

    private fun getRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
    }

    private fun getDocumentRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
</Relationships>"""
    }

    private fun getDocumentXml(title: String, markdownContent: String): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" 
            xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math" 
            xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <w:body>""")

        // Add Document Title Header block
        sb.append("""
    <w:p>
      <w:pPr>
        <w:jc w:val="center"/>
        <w:pBdr>
          <w:bottom w:val="single" w:sz="6" w:space="1" w:color="4F81BD"/>
        </w:pBdr>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/>
          <w:b/>
          <w:sz w:val="36"/>
          <w:color w:val="1F497D"/>
        </w:rPr>
        <w:t>${escapeXml(title)}</w:t>
      </w:r>
    </w:p>
""")

        // Process lines and identify math blocks or inline LaTeX segments
        val lines = markdownContent.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                sb.append("<w:p/>\n")
                continue
            }

            // Block Math notation ($$...$$)
            if (trimmed.startsWith("$$") && trimmed.endsWith("$$")) {
                val mathText = trimmed.removePrefix("$$").removeSuffix("$$").trim()
                sb.append(generateMathParagraphXml(mathText))
            } else if (trimmed.contains("$")) {
                // Inline math notation containing '$'
                sb.append(generateMixedParagraphXml(line))
            } else {
                // Standard text paragraph (English or Bengali!)
                sb.append("""
    <w:p>
      <w:pPr>
        <w:spacing w:line="240" w:lineRule="auto" w:after="120"/>
      </w:pPr>
      <w:r>
        <w:rPr>
          <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="Calibri"/>
          <w:sz w:val="24"/>
        </w:rPr>
        <w:t>${escapeXml(line)}</w:t>
      </w:r>
    </w:p>
""")
            }
        }

        sb.append("""
  </w:body>
</w:document>""")
        return sb.toString()
    }

    private fun generateMathParagraphXml(mathText: String): String {
        val escapedMath = escapeXml(mathText)
        return """
    <w:p>
      <w:pPr>
        <w:jc w:val="center"/>
      </w:pPr>
      <m:oMathPara>
        <m:oMath>
          <m:r>
            <w:rPr>
              <w:rFonts w:ascii="Cambria Math" w:hAnsi="Cambria Math"/>
            </w:rPr>
            <m:t>$escapedMath</m:t>
          </m:r>
        </m:oMath>
      </m:oMathPara>
    </w:p>
"""
    }

    private fun generateMixedParagraphXml(line: String): String {
        val sb = java.lang.StringBuilder()
        sb.append("""
    <w:p>
      <w:pPr>
        <w:spacing w:line="240" w:lineRule="auto" w:after="120"/>
      </w:pPr>""")

        val parts = line.split("$")
        var isMath = false
        for (part in parts) {
            if (isMath) {
                // Native Math rendering run
                sb.append("""
      <m:oMath>
        <m:r>
          <w:rPr>
            <w:rFonts w:ascii="Cambria Math" w:hAnsi="Cambria Math"/>
          </w:rPr>
          <m:t>${escapeXml(part)}</m:t>
        </m:r>
      </m:oMath>""")
            } else {
                if (part.isNotEmpty()) {
                    sb.append("""
      <w:r>
        <w:rPr>
          <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="Calibri"/>
          <w:sz w:val="24"/>
        </w:rPr>
        <w:t>${escapeXml(part)}</w:t>
      </w:r>""")
                }
            }
            isMath = !isMath
        }

        sb.append("\n    </w:p>\n")
        return sb.toString()
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
