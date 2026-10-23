package com.vocab.ielts.offline

/**
 * Dạng bài "Xáo chữ" lấy cảm hứng từ các game viết từ của ứng dụng tham chiếu,
 * nhưng tự triển khai bằng dữ liệu người dùng và không cần mạng.
 */
object ScrambleLogic {
    /**
     * Đảo thứ tự chữ cái của từ theo một seed cố định để cùng một từ luôn ra cùng
     * một đề bài trong phiên. Giữ nguyên ranh giới giữa các từ trong cụm từ.
     */
    fun scramble(term: String, seed: Long): String {
        val trimmed = term.trim()
        if (trimmed.length <= 2) return trimmed
        val random = java.util.Random(seed)
        return trimmed.split(Regex("\\s+")).joinToString(" ") { word ->
            if (word.length <= 2) {
                word
            } else {
                val chars = word.toCharArray().toMutableList()
                var attempt = 0
                do {
                    java.util.Collections.shuffle(chars, random)
                    attempt++
                } while (attempt < 6 && chars.joinToString("") == word)
                chars.joinToString("")
            }
        }
    }

    /** So khớp đáp án dùng chung quy tắc với các dạng gõ từ khác. */
    fun isAnswerCorrect(answer: String, term: String): Boolean = QuizLogic.isCorrectAnswer(answer, term)

    /** Gợi ý chữ cái đầu để người học không bị kẹt khi từ quá dài hoặc quá khó. */
    fun firstLetterHint(term: String): String = term.trim().take(1).lowercase()
}
