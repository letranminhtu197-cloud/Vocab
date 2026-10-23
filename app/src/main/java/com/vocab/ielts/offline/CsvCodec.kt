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
        val error: String? = null
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

    fun parse(text: String): ImportResult = parse(text, ',')

    /**
     * Như [parse] nhưng cho phép chọn dấu phân cách (`,` cho CSV, tab cho TSV/TSV copy
     * từ Excel, `;` cho CSV kiểu châu Âu).
     */
    fun parse(text: String, delimiter: Char): ImportResult {
        val rows = try { readRows(text.removePrefix("\uFEFF"), delimiter) } catch (e: IllegalArgumentException) {
            return ImportResult(emptyList(), 0, e.message ?: "CSV không hợp lệ")
        }
        val nonEmpty = rows.filter { row -> row.any { it.isNotBlank() } }
        if (nonEmpty.isEmpty()) return ImportResult(emptyList(), 0, "File CSV đang trống.")
        val header = nonEmpty.first().map { it.trim().lowercase().replace(" ", "").replace("_", "") }
        val termIndex = header.indexOfFirst { it in termAliases }
        val meaningIndex = header.indexOfFirst { it in meaningAliases }
        if (termIndex < 0 || meaningIndex < 0) {
            return ImportResult(emptyList(), 0, "Không tìm thấy cột bắt buộc. File phải có tiêu đề term/word và meaning/nghĩa.")
        }
        fun index(vararg aliases: String) = header.indexOfFirst { it in aliases.toSet() }
        val ipaIndex = index("ipa", "phonetic", "pronunciation", "phiênâm")
        val posIndex = index("partofspeech", "pos", "type", "từloại")
        val exampleIndex = index("example", "sentence", "vídụ", "câuvídụ")
        val exampleMeaningIndex = index("examplemeaning", "meaningofexample", "nghĩacâuvídụ", "dịchnghĩavídụ")
        val pausedIndex = index("paused", "suspended", "tạmdừng", "tamdung")
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
        val result = mutableListOf<WordEntity>()
        var invalid = 0
        var metadataError: String? = null
        nonEmpty.drop(1).forEach { row ->
            fun value(i: Int): String = if (i >= 0 && i < row.size) row[i].trim() else ""
            val term = value(termIndex)
            val meaning = value(meaningIndex)
            if (term.isBlank() || meaning.isBlank()) {
                invalid++
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
                fun booleanField(index: Int): Boolean {
                    val raw = value(index).lowercase()
                    return raw == "1" || raw == "true" || raw == "yes" || raw == "có" || raw == "co"
                }
                result += WordEntity(
                    term = term,
                    meaning = meaning,
                    ipa = value(ipaIndex),
                    partOfSpeech = value(posIndex),
                    example = value(exampleIndex),
                    exampleMeaning = value(exampleMeaningIndex),
                    topic = value(topicIndex),
                    collocations = value(collocationsIndex),
                    synonyms = value(synonymsIndex),
                    wordFamily = value(wordFamilyIndex),
                    notes = value(notesIndex),
                    tags = value(tagsIndex),
                    paused = booleanField(pausedIndex),
                    srsLevel = level!!,
                    nextReviewAtMillis = nextReview!!,
                    reviewCount = reviews!!,
                    lapseCount = lapses!!,
                    createdAtMillis = created!!,
                    lastReviewedAtMillis = lastReviewed!!
                )
            }
        }
        if (metadataError != null) return ImportResult(emptyList(), 0, metadataError)
        return ImportResult(result, invalid)
    }

    fun export(words: List<WordEntity>): String {
        val rows = mutableListOf(
            listOf("term", "ipa", "partOfSpeech", "meaning", "example", "exampleMeaning", "topic", "collocations", "synonyms", "wordFamily", "notes", "tags", "paused", "srsLevel", "nextReviewAtMillis", "reviewCount", "lapseCount", "createdAtMillis", "lastReviewedAtMillis")
        )
        words.forEach { w ->
            rows += listOf(w.term, w.ipa, w.partOfSpeech, w.meaning, w.example, w.exampleMeaning, w.topic, w.collocations, w.synonyms, w.wordFamily, w.notes, w.tags,
                if (w.paused) "1" else "0",
                w.srsLevel.toString(), w.nextReviewAtMillis.toString(), w.reviewCount.toString(), w.lapseCount.toString(), w.createdAtMillis.toString(), w.lastReviewedAtMillis.toString())
        }
        return rows.joinToString("\r\n") { row -> row.joinToString(",") { escape(it) } } + "\r\n"
    }

    /** Nhận diện dòng tiêu đề để phân biệt CSV/TSV có tiêu đề với file danh sách đơn giản. */
    fun looksLikeHeader(line: String, delimiter: Char = ','): Boolean {
        val cells = try {
            readRows(line, delimiter).firstOrNull().orEmpty()
        } catch (_: IllegalArgumentException) {
            return false
        }
        val normalized = cells.map { it.trim().lowercase().replace(" ", "").replace("_", "") }
        return normalized.any { it in termAliases } && normalized.any { it in meaningAliases }
    }

    private val termAliases = setOf("term", "word", "vocabulary", "từ", "từvựng", "từ/cụmtừ")
    private val meaningAliases = setOf("meaning", "definition", "translation", "nghĩa", "nghĩatiếngviệt")

    private fun escape(value: String): String =
        "\"" + value.replace("\"", "\"\"") + "\""

    private fun readRows(text: String, delimiter: Char = ','): List<List<String>> {
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
                closedQuote && ch == delimiter -> finishCell()
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
                ch == delimiter -> finishCell()
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
