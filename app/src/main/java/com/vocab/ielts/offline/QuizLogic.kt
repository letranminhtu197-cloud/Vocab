package com.vocab.ielts.offline

object QuizLogic {
    fun normalizeAnswer(value: String): String = value.trim().lowercase()
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()

    fun isCorrectAnswer(answer: String, term: String): Boolean =
        normalizeAnswer(answer).isNotBlank() && normalizeAnswer(answer) == normalizeAnswer(term)

    fun choiceLabel(word: WordEntity, mode: String): String = when (mode) {
        "Tìm từ" -> word.term
        "Cụm từ" -> word.collocations
        "Họ từ" -> word.wordFamily
        else -> word.meaning
    }

    /** Builds multiple-choice options with distinct visible answers and always includes the target. */
    fun multipleChoiceOptions(
        target: WordEntity,
        pool: List<WordEntity>,
        mode: String,
        limit: Int = 4
    ): List<WordEntity> {
        if (limit < 2) return listOf(target)
        val targetLabel = normalizeAnswer(choiceLabel(target, mode))
        if (targetLabel.isBlank()) return emptyList()
        val distractors = pool.asSequence()
            .filter { it.id != target.id }
            .shuffled()
            .filter { normalizeAnswer(choiceLabel(it, mode)).isNotBlank() }
            .distinctBy { normalizeAnswer(choiceLabel(it, mode)) }
            .filter { normalizeAnswer(choiceLabel(it, mode)) != targetLabel }
            .take(limit - 1)
            .toList()
        return (distractors + target).shuffled()
    }

    /** Resolve a retry deck in its captured order, independent of changing due/weak filters. */
    fun retryWords(words: List<WordEntity>, orderedIds: List<Long>): List<WordEntity> {
        val wordsById = words.associateBy { it.id }
        return orderedIds.distinct().mapNotNull { wordsById[it] }
    }

    fun clozeSentence(example: String, term: String): String? {
        if (example.isBlank() || term.isBlank()) return null
        val pattern = Regex("(?<![\\p{L}\\p{N}])" + Regex.escape(term) + "(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
        return if (pattern.containsMatchIn(example)) pattern.replace(example) { "_____" } else null
    }
}