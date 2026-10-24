package com.vocab.ielts.offline

object QuizLogic {
    fun normalizeAnswer(value: String): String = value.trim().lowercase()
        .replace(Regex("\\s+"), " ")

    fun isCorrectAnswer(answer: String, term: String): Boolean =
        normalizeAnswer(answer).isNotBlank() && normalizeAnswer(answer) == normalizeAnswer(term)

    /** Resolve a retry deck in its captured order, independent of changing due/weak filters. */
    fun retryWords(words: List<WordEntity>, orderedIds: List<Long>): List<WordEntity> {
        val wordsById = words.associateBy { it.id }
        return orderedIds.distinct().mapNotNull { wordsById[it] }
    }

    /** A cloze hides every whole-word occurrence; callers validate uniqueness before presenting it. */
    fun clozeSentence(example: String, term: String): String? {
        if (example.isBlank() || term.isBlank()) return null
        val pattern = Regex(
            "(?<![\\p{L}\\p{N}])" + Regex.escape(term) + "(?![\\p{L}\\p{N}])",
            RegexOption.IGNORE_CASE
        )
        return if (pattern.containsMatchIn(example)) pattern.replace(example) { "_____" } else null
    }
}