package vn.luyentu.app

/**
 * CSV helpers for offline vocabulary import/export.
 * Supported columns: term/word, meaning/translation, example, ipa, topic.
 */
object CsvTools {
    private val aliases = mapOf(
        "term" to setOf("term", "word", "vocabulary", "english", "từ", "từ vựng", "từ tiếng anh"),
        "meaning" to setOf("meaning", "translation", "definition", "nghĩa", "nghĩa tiếng việt", "dịch"),
        "example" to setOf("example", "sentence", "example sentence", "ví dụ", "câu ví dụ"),
        "ipa" to setOf("ipa", "phonetic", "pronunciation", "phiên âm"),
        "topic" to setOf("topic", "category", "tag", "chủ đề", "danh mục")
    )

    data class CsvWord(val term: String, val meaning: String, val example: String = "", val ipa: String = "", val topic: String = "")

    fun parse(text: String): List<CsvWord> {
        val rows = parseRows(text.removePrefix("\uFEFF"))
            .filter { row -> row.any { it.isNotBlank() } }
        if (rows.isEmpty()) return emptyList()

        val first = rows.first().map { it.trim().lowercase() }
        val headerMap = mutableMapOf<String, Int>()
        aliases.forEach { (field, names) ->
            val index = first.indexOfFirst { it in names }
            if (index >= 0) headerMap[field] = index
        }
        val hasHeader = "term" in headerMap && "meaning" in headerMap
        val dataRows = if (hasHeader) rows.drop(1) else rows
        val columns = if (hasHeader) headerMap else mapOf("term" to 0, "meaning" to 1, "example" to 2, "ipa" to 3, "topic" to 4)

        fun value(row: List<String>, key: String): String =
            columns[key]?.let { row.getOrNull(it)?.trim().orEmpty() }.orEmpty()

        return dataRows.mapNotNull { row ->
            val term = value(row, "term")
            val meaning = value(row, "meaning")
            if (term.isBlank() || meaning.isBlank()) null
            else CsvWord(term, meaning, value(row, "example"), value(row, "ipa"), value(row, "topic"))
        }
    }

    fun export(words: List<VocabularyWord>): String = buildString {
        appendLine("term,meaning,ipa,topic,example")
        words.forEach { word ->
            appendLine(listOf(word.term, word.meaning, word.ipa, word.topic, word.example).joinToString(",") { escape(it) })
        }
    }

    private fun escape(value: String): String {
        val normalized = value.replace("\r\n", "\n").replace("\r", "\n")
        return if (normalized.any { it == ',' || it == '"' || it == '\n' }) {
            "\"" + normalized.replace("\"", "\"\"") + "\""
        } else normalized
    }

    private fun parseRows(input: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            when {
                ch == '"' && quoted && i + 1 < input.length && input[i + 1] == '"' -> {
                    field.append('"'); i++
                }
                ch == '"' -> quoted = !quoted
                ch == ',' && !quoted -> {
                    row.add(field.toString()); field.setLength(0)
                }
                (ch == '\n' || ch == '\r') && !quoted -> {
                    if (ch == '\r' && i + 1 < input.length && input[i + 1] == '\n') i++
                    row.add(field.toString()); field.setLength(0)
                    rows.add(row); row = mutableListOf()
                }
                else -> field.append(ch)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row)
        }
        return rows
    }
}
