package com.vocab.ielts.offline

/**
 * Nhập từ hàng loạt từ nhiều định dạng văn bản khác nhau, tất cả đều offline:
 *
 * - CSV có tiêu đề (term/meaning hoặc word/nghĩa …) như trước đây.
 * - TSV (dán trực tiếp từ Excel/Google Sheets): cột phân tách bằng tab.
 * - CSV kiểu châu Âu (dấu `;`).
 * - Danh sách đơn giản mỗi dòng một từ: `word - nghĩa`, `word: nghĩa`,
 *   `word | nghĩa`, `word = nghĩa`, `word, nghĩa`; có thể thêm câu ví dụ ở phần thứ ba:
 *   `word - nghĩa - câu ví dụ`.
 *
 * Không cần mạng, không cần đăng nhập, giới hạn kích thước dùng chung với [CsvCodec].
 */
object ImportCodec {
    private val separators = listOf(" - ", " – ", " — ", "\t", " | ", ": ", "=", ",", ";")

    fun parse(text: String): CsvCodec.ImportResult {
        val clean = text.removePrefix("\uFEFF").trim()
        if (clean.isBlank()) return CsvCodec.ImportResult(emptyList(), 0, "File đang trống.")
        val firstLine = clean.lineSequence().first { it.isNotBlank() }
        val delimiter = when {
            firstLine.contains('\t') -> '\t'
            countChar(firstLine, ';') > countChar(firstLine, ',') -> ';'
            else -> ','
        }
        return if (CsvCodec.looksLikeHeader(firstLine, delimiter)) {
            CsvCodec.parse(clean, delimiter)
        } else {
            parsePlain(clean)
        }
    }

    fun parsePlain(text: String): CsvCodec.ImportResult {
        val words = mutableListOf<WordEntity>()
        var invalid = 0
        text.removePrefix("\uFEFF").lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#") || line.startsWith("//")) return@forEach
            val pair = splitByFirstSeparator(line) ?: run {
                invalid++
                return@forEach
            }
            val term = pair.first
            val rest = pair.second
            val meaningAndExample = splitMeaningAndExample(rest)
            val meaning = meaningAndExample.first
            val example = meaningAndExample.second
            if (term.isBlank() || meaning.isBlank()) {
                invalid++
            } else {
                words += WordEntity(term = term, meaning = meaning, example = example)
            }
        }
        if (words.isEmpty()) {
            return CsvCodec.ImportResult(
                emptyList(),
                invalid,
                "Không đọc được cặp từ–nghĩa nào. Mỗi dòng cần dạng: từ - nghĩa (có thể thêm - câu ví dụ)."
            )
        }
        return CsvCodec.ImportResult(words, invalid)
    }

    private fun splitByFirstSeparator(line: String): Pair<String, String>? {
        var bestIndex = -1
        var bestSeparator: String? = null
        separators.forEach { separator ->
            val index = line.indexOf(separator)
            if (index > 0 && (bestIndex < 0 || index < bestIndex ||
                    (index == bestIndex && separator.length > (bestSeparator?.length ?: 0)))
            ) {
                bestIndex = index
                bestSeparator = separator
            }
        }
        val separator = bestSeparator ?: return null
        return line.substring(0, bestIndex).trim() to line.substring(bestIndex + separator.length).trim()
    }

    /** Phần thứ ba (nếu có) được hiểu là câu ví dụ. */
    private fun splitMeaningAndExample(rest: String): Pair<String, String> {
        val exampleSeparators = listOf(" - ", " – ", " — ", " | ")
        var bestIndex = -1
        var bestSeparator: String? = null
        exampleSeparators.forEach { separator ->
            val index = rest.indexOf(separator)
            if (index > 0 && (bestIndex < 0 || index < bestIndex)) {
                bestIndex = index
                bestSeparator = separator
            }
        }
        val separator = bestSeparator ?: return rest to ""
        return rest.substring(0, bestIndex).trim() to rest.substring(bestIndex + separator.length).trim()
    }

    private fun countChar(text: String, target: Char): Int = text.count { it == target }
}
