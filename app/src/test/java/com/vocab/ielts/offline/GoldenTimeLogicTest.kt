package com.vocab.ielts.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class GoldenTimeLogicTest {
    private val now = 1_800_000_000_000L
    private val zone = ZoneId.of("Asia/Bangkok")

    private fun word(term: String, nextReviewAtMillis: Long, reviewCount: Int = 1) =
        WordEntity(term = term, meaning = "m", nextReviewAtMillis = nextReviewAtMillis, reviewCount = reviewCount)

    @Test fun dueNowCountIncludesNeverScheduledAndOverdueWords() {
        val words = listOf(
            word("new", 0L, reviewCount = 0),
            word("overdue", now - 10),
            word("future", now + 3_600_000L)
        )

        assertEquals(2, GoldenTimeLogic.dueNowCount(words, now))
    }

    @Test fun soonestWordPrefersTheOldestOverdueCard() {
        val words = listOf(
            word("future", now + 5_000_000L),
            word("new", 0L),
            word("overdue", now - 5_000L)
        )

        assertEquals("overdue", GoldenTimeLogic.soonestWord(words, now)?.term)
    }

    @Test fun nextReminderSkipsPastAndNearTimes() {
        val words = listOf(
            word("now", 0L),
            word("soon", now + 30_000L),
            word("later", now + 7_200_000L)
        )

        assertEquals(now + 7_200_000L, GoldenTimeLogic.nextReminderAt(words, now))
    }

    @Test fun nextReminderReturnsNullWhenNothingIsScheduled() {
        assertNull(GoldenTimeLogic.nextReminderAt(emptyList(), now))
        assertNull(GoldenTimeLogic.nextReminderAt(listOf(word("due", 0L)), now))
    }

    @Test fun labelSaysTodayTomorrowOrDate() {
        val today = now
        val tomorrow = now + 86_400_000L

        assertTrue(GoldenTimeLogic.label(today, now, zone).startsWith("hôm nay"))
        assertTrue(GoldenTimeLogic.label(tomorrow, now, zone).startsWith("mai"))
        assertTrue(GoldenTimeLogic.label(now + 10 * 86_400_000L, now, zone).contains("/"))
    }

    @Test fun pausedWordsAreIgnoredByGoldenTime() {
        val paused = WordEntity(
            term = "paused",
            meaning = "m",
            paused = true,
            nextReviewAtMillis = now - 1
        )

        assertEquals(0, GoldenTimeLogic.dueNowCount(listOf(paused), now))
        assertNull(GoldenTimeLogic.soonestWord(listOf(paused), now))
        assertNull(GoldenTimeLogic.nextReminderAt(listOf(paused), now))
    }

    @Test fun notificationMessageMentionsDueCount() {
        val message = GoldenTimeLogic.notificationMessage(
            listOf(word("a", now - 1), word("b", now - 2)),
            now
        )

        assertTrue(message.contains("2 từ"))
        assertTrue(GoldenTimeLogic.notificationMessage(emptyList(), now).contains("trống"))
    }
}
