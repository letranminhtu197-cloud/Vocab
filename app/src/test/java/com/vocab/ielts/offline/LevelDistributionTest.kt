package com.vocab.ielts.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelDistributionTest {
    private fun word(level: Int) = WordEntity(term = "w$level", meaning = "m", srsLevel = level)

    @Test fun countsEveryLevelIncludingNewWords() {
        val words = listOf(word(0), word(1), word(1), word(3), word(5), word(5))

        assertEquals(listOf(1, 2, 0, 1, 0, 2), LevelDistribution.counts(words))
    }

    @Test fun countsAlwaysSumToWordCount() {
        val words = (0..5).map { word(it) }

        assertEquals(words.size, LevelDistribution.counts(words).sum())
    }

    @Test fun outOfRangeLevelsAreClamped() {
        val words = listOf(word(-2), word(9))

        assertEquals(listOf(1, 0, 0, 0, 0, 1), LevelDistribution.counts(words))
    }

    @Test fun emptyNotebookHasZeroShares() {
        assertEquals(0f, LevelDistribution.masteredShare(emptyList()), 0.0001f)
    }

    @Test fun masteredShareCountsLevelFourAndFive() {
        val words = listOf(word(5), word(4), word(1), word(0))

        assertEquals(0.5f, LevelDistribution.masteredShare(words), 0.0001f)
    }

    @Test fun labelsAreShort() {
        assertTrue(LevelDistribution.label(0) == "Mới")
        assertTrue(LevelDistribution.label(3) == "C3")
    }
}
