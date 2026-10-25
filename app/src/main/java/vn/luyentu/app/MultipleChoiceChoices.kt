package vn.luyentu.app

/**
 * Builds offline multiple-choice answers without duplicate meanings.
 * The correct answer is included exactly once after trimming; distractors are
 * unique after case/whitespace normalization. Fewer than four choices are
 * returned when the vocabulary set does not contain enough distinct meanings.
 */
internal object MultipleChoiceChoices {
    fun create(correctMeaning: String, meanings: List<String>): List<String> {
        val correct = correctMeaning.trim()
        if (correct.isBlank()) return emptyList()

        val correctKey = meaningKey(correct)
        val distractors = meanings
            .map(String::trim)
            .filter { it.isNotBlank() && meaningKey(it) != correctKey }
            .distinctBy(::meaningKey)
            .shuffled()
            .take(3)
        return (distractors + correct).shuffled()
    }

    private fun meaningKey(value: String): String =
        value.trim().lowercase().replace(Regex("\\s+"), " ")
}
