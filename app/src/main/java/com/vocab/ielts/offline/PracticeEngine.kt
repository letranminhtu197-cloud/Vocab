package com.vocab.ielts.offline

import java.text.Normalizer
import java.util.Locale
import kotlin.random.Random

/** Active-recall exercise generation and grading. Pure Kotlin; works without network access. */
enum class PracticeMode {
    MULTIPLE_CHOICE,
    LISTEN_AND_TYPE,
    CONTEXTUAL_FILL
}

data class PracticeQuestion(
    val mode: PracticeMode,
    val wordId: Long,
    val prompt: String,
    val expectedAnswer: String,
    val options: List<String> = emptyList(),
    val hint: String = "",
    val explanation: String = ""
)

data class PracticeResult(
    val correct: Boolean,
    val normalizedExpected: String,
    val normalizedSubmitted: String,
    val feedback: String
)

object PracticeEngine {
    fun createQuestion(
        target: WordEntity,
        pool: List<WordEntity>,
        mode: PracticeMode,
        random: Random = Random.Default
    ): PracticeQuestion? {
        if (target.term.isBlank() || target.meaning.isBlank()) return null
        return when (mode) {
            PracticeMode.MULTIPLE_CHOICE -> {
                val distractors = pool.asSequence()
                    .filter { it.id != target.id && it.meaning.isNotBlank() }
                    .map { it.meaning.trim() }
                    .filter { normalizeAnswer(it) != normalizeAnswer(target.meaning) }
                    .distinctBy(::normalizeAnswer)
                    .shuffled(random)
                    .take(3)
                    .toList()
                if (distractors.size < 1) return null
                PracticeQuestion(
                    mode = mode,
                    wordId = target.id,
                    prompt = target.term,
                    expectedAnswer = target.meaning,
                    options = (distractors + target.meaning.trim()).shuffled(random),
                    hint = listOf(target.partOfSpeech, target.topic).filter { it.isNotBlank() }.joinToString(" · "),
                    explanation = target.example
                )
            }
            PracticeMode.LISTEN_AND_TYPE -> PracticeQuestion(
                mode = mode,
                wordId = target.id,
                prompt = target.meaning,
                expectedAnswer = target.term,
                hint = target.ipa,
                explanation = target.example
            )
            PracticeMode.CONTEXTUAL_FILL -> {
                val example = target.example.trim()
                val term = target.term.trim()
                if (example.isBlank() || term.isBlank()) return null
                val regex = Regex(Regex.escape(term), RegexOption.IGNORE_CASE)
                if (!regex.containsMatchIn(example)) return null
                PracticeQuestion(
                    mode = mode,
                    wordId = target.id,
                    prompt = regex.replace(example) { "________" },
                    expectedAnswer = term,
                    hint = target.meaning,
                    explanation = example
                )
            }
        }
    }

    fun grade(question: PracticeQuestion, submitted: String): PracticeResult {
        val expected = normalizeAnswer(question.expectedAnswer)
        val actual = normalizeAnswer(submitted)
        val correct = expected.isNotEmpty() && actual == expected
        return PracticeResult(
            correct = correct,
            normalizedExpected = expected,
            normalizedSubmitted = actual,
            feedback = when {
                correct -> "Chính xác!"
                submitted.isBlank() -> "Bạn chưa nhập đáp án. Đáp án: ${question.expectedAnswer}"
                else -> "Chưa chính xác. Đáp án: ${question.expectedAnswer}"
            }
        )
    }

    /** NFKC, case-insensitive and whitespace-insensitive grading; punctuation is preserved. */
    fun normalizeAnswer(value: String): String =
        Normalizer.normalize(value.trim(), Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")
}
