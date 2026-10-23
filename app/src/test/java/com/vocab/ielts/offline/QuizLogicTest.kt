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

    @Test fun multipleChoiceOptionsHaveUniqueVisibleAnswersAndIncludeTarget() {
        val target = WordEntity(id = 1, term = "allocate", meaning = "phân bổ")
        val duplicate = WordEntity(id = 2, term = "assign", meaning = "phân bổ")
        val other = WordEntity(id = 3, term = "sustainable", meaning = "bền vững")

        val options = QuizLogic.multipleChoiceOptions(target, listOf(target, duplicate, other), "Chọn nghĩa")

        assertTrue(options.any { it.id == target.id })
        assertEquals(options.size, options.map { QuizLogic.normalizeAnswer(QuizLogic.choiceLabel(it, "Chọn nghĩa")) }.distinct().size)
    }

    @Test fun duplicateVisibleLabelsCannotProduceMeaningfulDistractors() {
        val target = WordEntity(id = 1, term = "allocate", meaning = "phân bổ")
        val duplicateOne = WordEntity(id = 2, term = "assign", meaning = "phân bổ")
        val duplicateTwo = WordEntity(id = 3, term = "distribute", meaning = "phân bổ")

        val options = QuizLogic.multipleChoiceOptions(
            target,
            listOf(target, duplicateOne, duplicateTwo),
            "Chọn nghĩa"
        )

        assertEquals(listOf(target.id), options.map { it.id })
    }

    @Test fun multipleChoiceOptionsRespectLimitAndIgnoreBlankDistractors() {
        val target = WordEntity(id = 1, term = "allocate", meaning = "phân bổ")
        val blank = WordEntity(id = 2, term = "assign", meaning = "")
        val other = WordEntity(id = 3, term = "sustainable", meaning = "bền vững")

        val options = QuizLogic.multipleChoiceOptions(target, listOf(target, blank, other), "Chọn nghĩa", limit = 2)

        assertEquals(2, options.size)
        assertTrue(options.any { it.id == target.id })
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


    @Test fun answerComparisonIgnoresCaseAndPunctuation() {
        assertTrue(QuizLogic.isCorrectAnswer("  SUSTAINABLE! ", "sustainable"))
        assertFalse(QuizLogic.isCorrectAnswer("sustain", "sustainable"))
    }
    @Test fun hyphenAndSpaceVariantsAreAccepted() {
        assertTrue(QuizLogic.isCorrectAnswer("wellbeing", "well-being"))
        assertTrue(QuizLogic.isCorrectAnswer("well being", "well-being"))
        assertTrue(QuizLogic.isCorrectAnswer("ice-cream", "ice cream"))
        assertTrue(QuizLogic.isCorrectAnswer("dont", "don't"))
    }

    @Test fun separatorInsensitivityDoesNotAcceptDifferentWords() {
        assertFalse(QuizLogic.isCorrectAnswer("ice", "ice cream"))
        assertFalse(QuizLogic.isCorrectAnswer("well", "well-being"))
    }

    @Test fun dueFlashcardIndexResetsToStart() {
        assertEquals(0, FlashcardLogic.nextIndexAfterRating(2, 3, true))
    }
    @Test fun synonymModeUsesSynonymFieldForVisibleChoices() {
        val target = WordEntity(id = 1, term = "allocate", meaning = "phân bổ", synonyms = "assign; distribute")
        val distractor = WordEntity(id = 2, term = "reduce", meaning = "giảm", synonyms = "decrease; lessen")
        assertEquals("assign; distribute", QuizLogic.choiceLabel(target, "Từ đồng nghĩa"))
        val options = QuizLogic.multipleChoiceOptions(target, listOf(target, distractor), "Từ đồng nghĩa")
        assertEquals(2, options.size)
        assertTrue(options.any { it.id == target.id })
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
