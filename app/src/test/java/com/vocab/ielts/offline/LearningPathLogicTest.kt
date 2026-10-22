package com.vocab.ielts.offline

import org.junit.Assert.assertEquals
import org.junit.Test

class LearningPathLogicTest {
    @Test fun groupsWordsByTopicAndCountsProgressDueAndWeakWords() {
        val now = 1_000_000L
        val words = listOf(
            WordEntity(term = "adapt", meaning = "thích nghi", topic = "Environment", reviewCount = 2, nextReviewAtMillis = now + 10, lapseCount = 0),
            WordEntity(term = "emission", meaning = "khí thải", topic = "Environment", reviewCount = 1, nextReviewAtMillis = now - 10, lapseCount = 1),
            WordEntity(term = "allocate", meaning = "phân bổ", topic = "Education", reviewCount = 0, nextReviewAtMillis = 0, lapseCount = 0),
            WordEntity(term = "benefit", meaning = "lợi ích", topic = "", reviewCount = 0, nextReviewAtMillis = 0, lapseCount = 2)
        )

        val topics = LearningPathLogic.topics(words, now)
        val environment = topics.first { it.topic == "Environment" }
        assertEquals(2, environment.total)
        assertEquals(2, environment.learned)
        assertEquals(1, environment.due)
        assertEquals(1, environment.weak)
        assertEquals(100, environment.percent)
        assertEquals("Chưa phân loại", topics.last().topic)
    }

    @Test fun overallProgressIsEmptySafeAndAggregatesTopics() {
        val empty = LearningPathLogic.overall(emptyList(), 100L)
        assertEquals(0, empty.total)
        assertEquals(0, empty.percent)
        val words = listOf(
            WordEntity(term = "a", meaning = "A", topic = "One", reviewCount = 1),
            WordEntity(term = "b", meaning = "B", topic = "Two", reviewCount = 0)
        )
        val total = LearningPathLogic.overall(words, 100L)
        assertEquals(2, total.total)
        assertEquals(1, total.learned)
        assertEquals(50, total.percent)
        assertEquals(2, total.due)
    }
}
