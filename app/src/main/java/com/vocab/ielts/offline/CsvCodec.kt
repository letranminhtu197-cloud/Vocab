package com.vocab.ielts.offline

/** RFC-4180-style CSV support for user-owned vocabulary files. */
object CsvCodec {
    data class ImportResult(
        val words: List<WordEntity>,
        val invalidRows: Int,
        val error: String? = null
    )

    fun parse(text: String): ImportResult {
        val rows = try { readRows(text.removePrefix("\uFEFF")) } catch (e: IllegalArgumentException) {
            return ImportResult(emptyList(), 0, e.message ?: "CSV không hợp lệ")
        }
        val nonEmpty = rows.filter { row -> row.any { it.isNotBlank() } }
        if (nonEmpty.isEmpty()) return ImportResult(emptyList(), 0, "File CSV đang trống.")
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
        val srsIndex = index("srslevel", "level")
        val nextIndex = index("nextreviewatmillis", "nextreview")
        val reviewIndex = index("reviewcount", "reviews")
        val lapseIndex = index("lapsecount", "lapses")
        val result = mutableListOf<WordEntity>()
        var invalid = 0
        nonEmpty.drop(1).forEach { row ->
            fun value(i: Int): String = if (i >= 0 && i < row.size) row[i].trim() else ""
            val term = value(termIndex)
            val meaning = value(meaningIndex)
            if (term.isBlank() || meaning.isBlank()) {
                invalid++
            } else {
                result += WordEntity(
                    term = term,
                    meaning = meaning,
                    ipa = value(ipaIndex),
                    partOfSpeech = value(posIndex),
                    example = value(exampleIndex),
                    topic = value(topicIndex),
                    srsLevel = value(srsIndex).toIntOrNull()?.coerceIn(0, 5) ?: 0,
                    nextReviewAtMillis = value(nextIndex).toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
                    reviewCount = value(reviewIndex).toIntOrNull()?.coerceAtLeast(0) ?: 0,
                    lapseCount = value(lapseIndex).toIntOrNull()?.coerceAtLeast(0) ?: 0
                )
            }
        }
        return ImportResult(result, invalid)
    }

    fun export(words: List<WordEntity>): String {
        val rows = mutableListOf(
            listOf("term", "ipa", "partOfSpeech", "meaning", "example", "topic", "srsLevel", "nextReviewAtMillis", "reviewCount", "lapseCount")
        )
        words.forEach { w ->
            rows += listOf(w.term, w.ipa, w.partOfSpeech, w.meaning, w.example, w.topic,
                w.srsLevel.toString(), w.nextReviewAtMillis.toString(), w.reviewCount.toString(), w.lapseCount.toString())
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
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                quoted && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> { cell.append('"'); i++ }
                c == '"' && !quoted && cell.isEmpty() -> quoted = true
                c == '"' && quoted -> quoted = false
                c == ',' && !quoted -> { row.add(cell.toString()); cell.setLength(0) }
                (c == '\n' || c == '\r') && !quoted -> {
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    row.add(cell.toString()); cell.setLength(0)
                    rows.add(row); row = mutableListOf()
                }
                else -> cell.append(c)
            }
            i++
        }
        require(!quoted) { "File CSV có dấu ngoặc kép chưa đóng." }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row.add(cell.toString())
            rows.add(row)
        }
        return rows
    }
}
