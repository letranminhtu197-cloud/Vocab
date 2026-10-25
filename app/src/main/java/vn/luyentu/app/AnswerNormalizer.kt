package vn.luyentu.app

/** Normalizes typed answers and meanings for offline answer checking. */
internal object AnswerNormalizer {
    fun normalize(value: String): String =
        value.trim().lowercase().replace(Regex("\\s+"), " ").trimEnd('.', '!', '?')

    fun matches(answer: String, expected: String): Boolean =
        normalize(answer) == normalize(expected)
}
