package com.vocab.ielts.offline

import org.junit.Assert.*
import org.junit.Test

class LearningAttemptEntityTest {
    @Test fun attemptIdentityIsStableAndCapturesModeSeparately() {
        val attempt = LearningAttemptEntity(
            attemptId = "attempt-123",
            wordId = 42L,
            sessionId = "session-456",
            exerciseType = ActiveExerciseType.CLOZE.name,
            isCorrect = true,
            isSrsReview = false,
            answeredAtMillis = 1_800_000_000_000L
        )
        assertEquals("attempt-123", attempt.attemptId)
        assertEquals(42L, attempt.wordId)
        assertFalse(attempt.isSrsReview)
        assertTrue(attempt.isCorrect)
    }
}
