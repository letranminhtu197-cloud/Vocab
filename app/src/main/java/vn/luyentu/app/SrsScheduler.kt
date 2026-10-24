package vn.luyentu.app

/**
 * Deterministic, offline spaced-repetition scheduling.
 * Grades: 0 = Again, 1 = Hard, 2 = Good, 3 = Easy.
 */
data class SrsSchedule(
    val grade: Int,
    val reviewedAt: Long,
    val nextReviewAt: Long,
    val intervalDays: Int,
    val repetitions: Int,
    val easeFactor: Double,
    val lapses: Int,
    val learningStep: Int
)

object SrsScheduler {
    private const val MINUTE_MS = 60 * 1000L
    private const val HOUR_MS = 60 * MINUTE_MS
    private const val DAY_MS = 24 * HOUR_MS

    fun calculate(word: VocabularyWord, grade: Int, nowMs: Long): SrsSchedule {
        val safeGrade = grade.coerceIn(0, 3)
        val nextLapses = word.lapses + if (safeGrade == 0) 1 else 0
        val nextRepetitions: Int
        val nextInterval: Int
        val nextEase: Double
        val nextStep: Int
        val delayMs: Long

        when (safeGrade) {
            0 -> {
                nextRepetitions = 0
                nextInterval = 0
                nextEase = (word.easeFactor - 0.2).coerceAtLeast(1.3)
                nextStep = 0
                delayMs = 10 * MINUTE_MS
            }
            1 -> {
                nextRepetitions = word.repetitions
                nextInterval = if (word.intervalDays <= 1) 1 else (word.intervalDays * 1.2).toInt().coerceAtLeast(1)
                nextEase = (word.easeFactor - 0.15).coerceAtLeast(1.3)
                nextStep = 1
                delayMs = if (word.repetitions == 0) 12 * HOUR_MS else nextInterval * DAY_MS
            }
            else -> {
                nextRepetitions = word.repetitions + 1
                nextEase = if (safeGrade == 3) (word.easeFactor + 0.15).coerceAtMost(3.0) else word.easeFactor
                nextInterval = when {
                    word.repetitions == 0 -> if (safeGrade == 3) 4 else 1
                    word.repetitions == 1 -> if (safeGrade == 3) 7 else 3
                    else -> (word.intervalDays * nextEase * if (safeGrade == 3) 1.3 else 1.0).toInt().coerceAtLeast(1)
                }
                nextStep = 0
                delayMs = nextInterval * DAY_MS
            }
        }

        return SrsSchedule(
            grade = safeGrade,
            reviewedAt = nowMs,
            nextReviewAt = nowMs + delayMs,
            intervalDays = nextInterval,
            repetitions = nextRepetitions,
            easeFactor = nextEase,
            lapses = nextLapses,
            learningStep = nextStep
        )
    }
}
