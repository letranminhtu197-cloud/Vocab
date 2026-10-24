package com.vocab.ielts.offline

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/** RFC-4180-style CSV support for user-owned vocabulary files. */
object CsvCodec {
    data class ImportResult(
        val words: List<WordEntity>,
        val invalidRows: Int,
        val error: String? = null,
        val invalidRowNumbers: List<Int> = emptyList()
    )

    const val MAX_IMPORT_BYTES: Int = 20 * 1024 * 1024

    /** Read a CSV stream with a strict size cap before decoding UTF-8. */
    fun readUtf8(input: InputStream, maxBytes: Int = MAX_IMPORT_BYTES): String {
        require(maxBytes > 0) { "Giới hạn kích thước CSV không hợp lệ." }
        val output = ByteArrayOutputStream(minOf(maxBytes, 8192))
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= maxBytes) { "File CSV vượt giới hạn ${maxBytes / (1024 * 1024)} MB." }
            output.write(buffer, 0, count)
        }
        return decodeUtf8(output.toByteArray())
    }

    /** Decode imported CSV without silently replacing malformed UTF-8 bytes. */
    fun decodeUtf8(bytes: ByteArray): String =
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()

    fun parse(text: String): ImportResult {
        val rows = try { readRows(text.removePrefix("\uFEFF")) } catch (e: IllegalArgumentException) {
            return ImportResult(emptyList(), 0, e.message ?: "CSV không hợp lệ")
        }
        val nonEmpty = rows.filter { row -> row.any { it.isNotBlank() } }
        if (nonEmpty.isEmpty()) return ImportResult(emptyList(), 0, "File CSV đang trống.")
        val headerRowIndex = rows.indexOfFirst { row -> row.any { it.isNotBlank() } }
        val header = nonEmpty.first().map { it.trim().lowercase().replace(" ", "").replace("_", "") }
        val termIndex = header.indexOfFirst { it in setOf("term", "word", "vocabulary", "từ", "từvựng", "từ/cụmtừ") }
        val meaningIndex = header.indexOfFirst { it in setOf("meaning", "definition", "translation", "nghĩa", "nghĩatiếngviệt") }
        if (termIndex < 0 || meaningIndex < 0) {
            return ImportResult(emptyList(), 0, "Không tìm thấy cột bắt buộc. File phải có tiêu đề term/word và meaning/nghĩa.")
        }
        fun index(vararg aliases: String) = header.indexOfFirst { it in aliases.toSet() }
        val ipaIndex = index("ipa", "phonetic", "pronunciation", "phiênâm")
        val posIndex = index("partofspeech", "pos", "type", "từloại")
        val exampleIndex = index("example", "sentence", "vídụ", "câuvídụ")
        val topicIndex = index("topic", "category", "chủđề")
        val collocationsIndex = index("collocations", "collocation", "cụmtừ", "cụmtừđi kèm", "cụmtừđikèm")
        val synonymsIndex = index("synonyms", "synonym", "syn", "đồngnghĩa", "từđồngnghĩa")
        val wordFamilyIndex = index("wordfamily", "wordfamilies", "họtừ", "họtừvựng")
        val notesIndex = index("notes", "note", "ghichú", "ghichucánhân")
        val tagsIndex = index("tags", "tag", "thẻ", "the")
        val srsIndex = index("srslevel", "level")
        val nextIndex = index("nextreviewatmillis", "nextreview")
        val reviewIndex = index("reviewcount", "reviews")
        val lapseIndex = index("lapsecount", "lapses")
        val createdIndex = index("createdatmillis", "createdat")
        val lastReviewedIndex = index("lastreviewedatmillis", "lastreviewed")
        val excludedIndex = index("excludedfromreview", "skipreview", "khongon", "khôngôn")
        val result = mutableListOf<WordEntity>()
        var invalid = 0
        val invalidRowNumbers = mutableListOf<Int>()
        var metadataError: String? = null
        rows.withIndex().filter { indexed -> indexed.index > headerRowIndex && indexed.value.any { it.isNotBlank() } }.forEach { indexed ->
            val row = indexed.value
            val lineNumber = indexed.index + 1
            fun value(i: Int): String = if (i >= 0 && i < row.size) row[i].trim() else ""
            val term = value(termIndex)
            val meaning = value(meaningIndex)
            if (term.isBlank() || meaning.isBlank()) {
                invalid++
                invalidRowNumbers += lineNumber
            } else {
                fun intField(index: Int, name: String, default: Int, range: IntRange): Int? {
                    val raw = value(index)
                    if (raw.isBlank()) return default
                    val parsed = raw.toIntOrNull()
                    if (parsed == null || parsed !in range) {
                        metadataError = "Giá trị ${name} không hợp lệ ở dòng chứa từ \"${term}\"."
                        return null
                    }
                    return parsed
                }
                fun longField(index: Int, name: String, default: Long): Long? {
                    val raw = value(index)
                    if (raw.isBlank()) return default
                    val parsed = raw.toLongOrNull()
                    if (parsed == null || parsed < 0L) {
                        metadataError = "Giá trị ${name} không hợp lệ ở dòng chứa từ \"${term}\"."
                        return null
                    }
                    return parsed
                }
                val level = intField(srsIndex, "srsLevel", 0, 0..5)
                val reviews = intField(reviewIndex, "reviewCount", 0, 0..Int.MAX_VALUE)
                val lapses = intField(lapseIndex, "lapseCount", 0, 0..Int.MAX_VALUE)
                val nextReview = longField(nextIndex, "nextReviewAtMillis", 0L)
                val created = longField(createdIndex, "createdAtMillis", System.currentTimeMillis())
                val lastReviewed = longField(lastReviewedIndex, "lastReviewedAtMillis", 0L)
                if (listOf(level, reviews, lapses, nextReview, created, lastReviewed).any { it == null }) {
                    return@forEach
                }
                result += WordEntity(
                    term = term,
                    meaning = meaning,
                    ipa = value(ipaIndex),
                    partOfSpeech = value(posIndex),
                    example = value(exampleIndex),
                    topic = value(topicIndex),
                    collocations = value(collocationsIndex),
                    synonyms = value(synonymsIndex),
                    wordFamily = value(wordFamilyIndex),
                    notes = value(notesIndex),
                    tags = value(tagsIndex),
                    srsLevel = level!!,
                    nextReviewAtMillis = nextReview!!,
                    reviewCount = reviews!!,
                    lapseCount = lapses!!,
                    createdAtMillis = created!!,
                    lastReviewedAtMillis = lastReviewed!!,
                    excludedFromReview = value(excludedIndex).let { it == "1" || it.equals("true", ignoreCase = true) || it.equals("yes", ignoreCase = true) }
                )
            }
        }
        if (metadataError != null) return ImportResult(emptyList(), 0, metadataError)
        return ImportResult(result, invalid, invalidRowNumbers = invalidRowNumbers)
    }

    fun export(words: List<WordEntity>): String {
        val rows = mutableListOf(
            listOf("term", "ipa", "partOfSpeech", "meaning", "example", "topic", "collocations", "synonyms", "wordFamily", "notes", "tags", "srsLevel", "nextReviewAtMillis", "reviewCount", "lapseCount", "createdAtMillis", "lastReviewedAtMillis", "excludedFromReview")
        )
        words.forEach { w ->
            rows += listOf(w.term, w.ipa, w.partOfSpeech, w.meaning, w.example, w.topic, w.collocations, w.synonyms, w.wordFamily, w.notes, w.tags,
                w.srsLevel.toString(), w.nextReviewAtMillis.toString(), w.reviewCount.toString(), w.lapseCount.toString(), w.createdAtMillis.toString(), w.lastReviewedAtMillis.toString(), if (w.excludedFromReview) "true" else "false")
        }
        return rows.joinToString("\r\n") { row -> row.joinToString(",") { escape(it) } } + "\r\n"
    }

    private fun escape(value: String): String =
        "\"" + value.replace("\"", "\"\"") + "\""

    private fun readRows(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var closedQuote = false
        var i = 0

        fun finishCell() {
            row.add(cell.toString())
            cell.setLength(0)
            closedQuote = false
        }
        fun finishRow() {
            finishCell()
            rows.add(row)
            row = mutableListOf()
        }

        while (i < text.length) {
            val ch = text[i]
            when {
                quoted && ch == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                    cell.append('"')
                    i++
                }
                quoted && ch == '"' -> {
                    quoted = false
                    closedQuote = true
                }
                quoted -> cell.append(ch)
                closedQuote && ch == ',' -> finishCell()
                closedQuote && (ch == '\n' || ch == '\r') -> {
                    if (ch == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    finishRow()
                }
                closedQuote -> throw IllegalArgumentException(
                    "File CSV có ký tự không hợp lệ sau dấu ngoặc kép đóng."
                )
                ch == '"' && cell.isEmpty() -> quoted = true
                ch == '"' -> throw IllegalArgumentException(
                    "File CSV có dấu ngoặc kép ở vị trí không hợp lệ."
                )
                ch == ',' -> finishCell()
                ch == '\n' || ch == '\r' -> {
                    if (ch == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    finishRow()
                }
                else -> cell.append(ch)
            }
            i++
        }

        require(!quoted) { "File CSV có dấu ngoặc kép chưa đóng." }
        if (closedQuote || cell.isNotEmpty() || row.isNotEmpty()) {
            finishCell()
            rows.add(row)
        }
        return rows
    }
}
