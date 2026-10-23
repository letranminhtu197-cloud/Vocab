package com.vocab.ielts.offline

import org.junit.Assert.assertEquals
import org.junit.Test

class LibrarySortingTest {
    private val alpha = WordEntity(id = 1, term = "alpha", meaning = "a", createdAtMillis = 30L, nextReviewAtMillis = 500L)
    private val beta = WordEntity(id = 2, term = "Beta", meaning = "b", createdAtMillis = 10L, nextReviewAtMillis = 0L)
    private val gamma = WordEntity(id = 3, term = "gamma", meaning = "c", createdAtMillis = 20L, nextReviewAtMillis = 100L)

    @Test fun alphabeticalSortIgnoresCase() {
        val sorted = LibrarySorting.sort(listOf(gamma, beta, alpha), LibrarySort.AZ)

        assertEquals(listOf("alpha", "Beta", "gamma"), sorted.map { it.term })
    }

    @Test fun newestSortPutsMostRecentlyCreatedFirst() {
        val sorted = LibrarySorting.sort(listOf(beta, gamma, alpha), LibrarySort.NEWEST)

        assertEquals(listOf(1L, 3L, 2L), sorted.map { it.id })
    }

    @Test fun dueSoonSortPutsNeverScheduledWordFirstThenEarliestReview() {
        val sorted = LibrarySorting.sort(listOf(alpha, gamma, beta), LibrarySort.DUE_SOON)

        assertEquals(listOf(2L, 3L, 1L), sorted.map { it.id })
    }
}
