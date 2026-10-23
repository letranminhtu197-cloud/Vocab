package com.vocab.ielts.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StarterCourseTest {
    @Test fun starterCourseHasSixtyUniqueWords() {
        assertEquals(60, StarterCourse.words.size)
        assertEquals(60, StarterCourse.words.map { it.term.lowercase() }.distinct().size)
    }

    @Test fun everyStarterWordHasRequiredFields() {
        assertTrue(
            StarterCourse.words.all {
                it.term.isNotBlank() && it.meaning.isNotBlank() && it.example.isNotBlank() && it.topic.isNotBlank()
            }
        )
    }

    @Test fun starterCourseCoversSixTopics() {
        val topics = StarterCourse.words.map { it.topic }.distinct()

        assertEquals(6, topics.size)
        topics.forEach { assertTrue(it.isNotBlank()) }
    }

    @Test fun starterWordsStartWithFreshSrsState() {
        assertTrue(StarterCourse.words.all { it.srsLevel == 0 && it.reviewCount == 0 && it.nextReviewAtMillis == 0L })
    }
}
