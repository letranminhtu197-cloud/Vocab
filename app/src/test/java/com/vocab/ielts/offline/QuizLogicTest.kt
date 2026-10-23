package com.vocab.ielts.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizLogicTest {
    @Test fun clozeReplacesTermCaseInsensitively() {
        assertEquals("We need to _____ more resources.", QuizLogic.clozeSentence("We need to Allocate more resources.", "allocate"))
    }

    @Test fun clozeDoesNotMatchTermInsideAnotherWord() {
        assertNull(QuizLogic.clozeSentence("The artist joined the party.", "art"))
        assertEquals("The _____ is useful.", QuizLogic.clozeSentence("The tool is useful.", "tool"))
    }

    @Test fun clozeReturnsNullWhenExampleDoesNotContainTerm() {
        assertNull(QuizLogic.clozeSentence("The plan was successful.", "allocate"))
    }

    @Test fun clozeHidesEveryOccurrenceOfTheTargetTerm() {
        assertEquals(
            "We must _____ resources and _____ budgets.",
            QuizLogic.clozeSentence("We must allocate resources and allocate budgets.", "allocate")
        )
    }

    @Test fun retryDeckPreservesCapturedOrderAndIgnoresMissingIds() {
        val first = WordEntity(id = 1, term = "allocate", meaning = "phân bổ")
        val second = WordEntity(id = 2, term = "sustainable", meaning = "bền vững")
        val third = WordEntity(id = 3, term = "mitigate", meaning = "giảm nhẹ")

        val replay = QuizLogic.retryWords(listOf(first, second, third), listOf(3, 1, 3, 99))

        assertEquals(listOf(3L, 1L), replay.map { it.id })
    }

    @Test fun blankAnswersAreNeverAccepted() {
        assertFalse(QuizLogic.isCorrectAnswer("", "sustainable"))
        assertFalse(QuizLogic.isCorrectAnswer("   ", "sustainable"))
    }


    @Test fun answerComparisonIgnoresCaseAndWhitespaceButPreservesMeaningfulPunctuation() {
        assertTrue(QuizLogic.isCorrectAnswer("  SUSTAINABLE  ", "sustainable"))
        assertFalse(QuizLogic.isCorrectAnswer("  SUSTAINABLE! ", "sustainable"))
        assertFalse(QuizLogic.isCorrectAnswer("well-being", "well being"))
        assertFalse(QuizLogic.isCorrectAnswer("sustain", "sustainable"))
    }
    @Test fun dueFlashcardIndexResetsToStart() {
        assertEquals(0, FlashcardLogic.nextIndexAfterRating(2, 3, true))
    }
    @Test fun mixedStudyQueuePrioritizesNewAndDueCardsAheadOfFutureMasteredCards() {
        val now = 1_000_000L
        val futureMastered = WordEntity(
            term = "abundant", meaning = "plentiful", srsLevel = 5,
            nextReviewAtMillis = now + 7 * 24 * 60 * 60 * 1000L, reviewCount = 12
        )
        val newCard = WordEntity(term = "mitigate", meaning = "reduce severity")
        val dueCard = WordEntity(
            term = "allocate", meaning = "distribute", srsLevel = 2,
            nextReviewAtMillis = now - 60_000L, reviewCount = 3
        )

        val result = StudyQueueLogic.prioritize(listOf(futureMastered, dueCard, newCard), now)

        assertEquals(listOf("mitigate", "allocate", "abundant"), result.map { it.term })
    }

    @Test fun repeatedlyForgottenDueCardGetsRaisedPriority() {
        val now = 1_000_000L
        val newCard = WordEntity(term = "baseline", meaning = "starting point")
        val weakCard = WordEntity(
            term = "deteriorate", meaning = "become worse", srsLevel = 1,
            nextReviewAtMillis = now - 1, reviewCount = 4, lapseCount = 2
        )

        val result = StudyQueueLogic.prioritize(listOf(newCard, weakCard), now)

        assertEquals("deteriorate", result.first().term)
    }

    @Test fun mixedStudyQueueUsesTermAsDeterministicTieBreaker() {
        val alpha = WordEntity(term = "alpha", meaning = "first")
        val beta = WordEntity(term = "beta", meaning = "second")

        assertEquals(
            listOf("alpha", "beta"),
            StudyQueueLogic.prioritize(listOf(beta, alpha), 1_000_000L).map { it.term }
        )
    }

}
