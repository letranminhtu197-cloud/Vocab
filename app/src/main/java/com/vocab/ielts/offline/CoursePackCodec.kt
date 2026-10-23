package com.vocab.ielts.offline

/**
 * Nhập "gói khóa học" JSON — một tệp có thể chứa nhiều bài, mỗi bài nhiều từ.
 * Toàn bộ xử lý offline, không cần tài khoản; nội dung do người dùng tự soạn.
 *
 * Định dạng hỗ trợ:
 * ```json
 * {
 *   "course": "IELTS Foundation",
 *   "lessons": [
 *     { "title": "Education", "words": [ { "term": "curriculum", "meaning": "chương trình học" } ] }
 *   ]
 * }
 * ```
 * Ngoài ra nhận `{"words": [...]}` (một danh sách phẳng) hoặc một array gốc.
 */
data class ParsedCourse(
    val name: String,
    val lessonCount: Int,
    val words: List<WordEntity>,
    val invalidRows: Int,
    val error: String? = null
)

object CoursePackCodec {
    fun parse(text: String): ParsedCourse {
        val root = try {
            SimpleJson.parse(text.removePrefix("\uFEFF"))
        } catch (e: IllegalArgumentException) {
            return ParsedCourse("", 0, emptyList(), 0, "JSON không hợp lệ: ${e.message}")
        }
        val words = mutableListOf<WordEntity>()
        var invalid = 0
        var lessonCount = 0
        var name = ""

        fun addWord(raw: Any?) {
            val map = raw as? Map<*, *> ?: run { invalid++; return }
            val word = toWord(map) ?: run { invalid++; return }
            words += word
        }

        when (root) {
            is Map<*, *> -> {
                name = stringOf(root, "course", "name", "title", "tên", "khóa").orEmpty()
                val lessons = root["lessons"] ?: root["bài"] ?: root["bai"]
                val flatWords = root["words"] ?: root["vocabulary"] ?: root["từ"] ?: root["tu"]
                when {
                    lessons is List<*> -> {
                        lessonCount = lessons.size
                        lessons.forEach { lesson ->
                            val lessonMap = lesson as? Map<*, *> ?: run { invalid++; return@forEach }
                            val lessonTopic = stringOf(lessonMap, "title", "topic", "name", "chủđề", "chude")
                            val lessonWords = lessonMap["words"] ?: lessonMap["vocabulary"]
                            if (lessonWords is List<*>) {
                                lessonWords.forEach { raw ->
                                    val map = raw as? Map<*, *> ?: run { invalid++; return@forEach }
                                    val word = toWord(map, fallbackTopic = lessonTopic)
                                    if (word == null) invalid++ else words += word
                                }
                            } else {
                                invalid++
                            }
                        }
                    }
                    flatWords is List<*> -> {
                        lessonCount = 1
                        flatWords.forEach { addWord(it) }
                    }
                    else -> return ParsedCourse("", 0, emptyList(), 0, "Không tìm thấy danh sách từ hoặc bài học trong JSON.")
                }
            }
            is List<*> -> {
                lessonCount = 1
                root.forEach { addWord(it) }
            }
            else -> return ParsedCourse("", 0, emptyList(), 0, "JSON gốc phải là object hoặc array.")
        }

        if (words.isEmpty()) {
            return ParsedCourse(
                name, lessonCount, emptyList(), invalid,
                "Không đọc được từ nào. Mỗi từ cần có \"term\" và \"meaning\"."
            )
        }
        return ParsedCourse(
            name = name.ifBlank { "Khóa học nhập từ JSON" },
            lessonCount = lessonCount,
            words = words,
            invalidRows = invalid
        )
    }

    private fun toWord(map: Map<*, *>, fallbackTopic: String? = null): WordEntity? {
        val term = stringOf(map, "term", "word", "từ", "tu")?.trim().orEmpty()
        val meaning = stringOf(map, "meaning", "nghĩa", "nghia", "translation")?.trim().orEmpty()
        if (term.isBlank() || meaning.isBlank()) return null
        return WordEntity(
            term = term,
            ipa = stringOf(map, "ipa", "phonetic").orEmpty(),
            partOfSpeech = stringOf(map, "partOfSpeech", "pos", "type", "từloại").orEmpty(),
            meaning = meaning,
            example = stringOf(map, "example", "sentence", "vídụ").orEmpty(),
            exampleMeaning = stringOf(map, "exampleMeaning", "dịchvídụ", "nghĩacâuvídụ").orEmpty(),
            topic = stringOf(map, "topic", "chủđề", "chude") ?: fallbackTopic.orEmpty(),
            collocations = stringOf(map, "collocations", "cụmtừ").orEmpty(),
            synonyms = stringOf(map, "synonyms", "đồngnghĩa").orEmpty(),
            wordFamily = stringOf(map, "wordFamily", "họtừ").orEmpty(),
            notes = stringOf(map, "notes", "ghichú").orEmpty(),
            tags = stringOf(map, "tags", "thẻ").orEmpty(),
            paused = booleanOf(map, "paused", "tạmdừng")
        )
    }

    private fun stringOf(map: Map<*, *>, vararg keys: String): String? {
        keys.forEach { key ->
            val value = map[key] ?: map.entries.firstOrNull { it.key.toString().equals(key, true) }?.value
            if (value is String && value.isNotBlank()) return value
            if (value is Number) return value.toString()
        }
        return null
    }

    private fun booleanOf(map: Map<*, *>, vararg keys: String): Boolean {
        keys.forEach { key ->
            when (val value = map[key]) {
                is Boolean -> return value
                is String -> if (value.equals("true", true) || value == "1" || value.equals("có", true)) return true
                is Number -> if (value.toInt() != 0) return true
            }
        }
        return false
    }
}
