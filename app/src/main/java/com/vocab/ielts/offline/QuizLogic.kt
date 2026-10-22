package com.vocab.ielts.offline

object QuizLogic {
    fun normalizeAnswer(value: String): String = value.trim().lowercase()
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()

    fun isCorrectAnswer(answer: String, term: String): Boolean =
        normalizeAnswer(answer).isNotBlank() && normalizeAnswer(answer) == normalizeAnswer(term)

    fun clozeSentence(example: String, term: String): String? {
        if (example.isBlank() || term.isBlank()) return null
        val pattern = Regex("(?<![\\p{L}\\p{N}])" + Regex.escape(term) + "(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
        return if (pattern.containsMatchIn(example)) pattern.replace(example) { "_____" } else null
    }
}