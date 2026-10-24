package com.vocab.ielts.offline

import org.junit.Assert.*
import org.junit.Test

class LearningSessionEntityTest {
    @Test fun unfinishedSessionDoesNotPretendToHaveCompletionMetrics() {
        val session = LearningSessionEntity(
            sessionId = "session-1",
            startedAtMillis = 1_800_000_000_000L,
            isSrsReview = true
        )
        assertFalse(session.completed)
        assertEquals(0L, session.endedAtMillis)
        assertEquals(0, session.questionCount)
        assertEquals(0, session.uniqueWordCount)
    }

    @Test fun completedSessionKeepsQuestionsDistinctFromUniqueWords() {
        val session = LearningSessionEntity(
            sessionId = "session-2",
            startedAtMillis = 1_800_000_000_000L,
            endedAtMillis = 1_800_000_060_000L,
            questionCount = 5,
            uniqueWordCount = 4,
            isSrsReview = false,
            completed = true
        )
        assertTrue(session.completed)
        assertEquals(5, session.questionCount)
        assertEquals(4, session.uniqueWordCount)
        assertEquals(60_000L, session.endedAtMillis - session.startedAtMillis)
    }
}
