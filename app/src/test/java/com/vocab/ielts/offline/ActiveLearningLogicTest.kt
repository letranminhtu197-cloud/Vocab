package com.vocab.ielts.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveLearningLogicTest {
    @Test fun clozeRequiresExactlyOneWholeWordOccurrence() {
        assertEquals(
            "We need to _____ resources.",
            ActiveLearningLogic.validatedCloze("We need to allocate resources.", "allocate")
        )
        assertNull(ActiveLearningLogic.validatedCloze("The artist joined.", "art"))
        assertNull(ActiveLearningLogic.validatedCloze("Allocate funds and allocate time.", "allocate"))
        assertNull(ActiveLearningLogic.validatedCloze("No target here.", "allocate"))
    }

    @Test fun typedAnswerNormalizationPreservesMeaningfulPunctuation() {
        assertTrue(QuizLogic.isCorrectAnswer("  take   off ", "take off"))
        assertFalse(QuizLogic.isCorrectAnswer("well-being", "well being"))
        assertFalse(QuizLogic.isCorrectAnswer("can't", "cant"))
        assertFalse(QuizLogic.isCorrectAnswer("allocate!", "allocate"))
    }

    @Test fun selectorNeverReturnsAnUnavailableExercise() {
        assertNull(ActiveLearningLogic.chooseNext(emptyList(), 100L))
        assertEquals(
            ActiveExerciseType.CLOZE,
            ActiveLearningLogic.chooseNext(
                listOf(
                    ExerciseEvidence(ActiveExerciseType.DEFINITION_TO_TERM, false),
                    ExerciseEvidence(ActiveExerciseType.CLOZE, true)
                ),
                100L
            )
        )
    }

    @Test fun matchPairsRejectDuplicateTermsMeaningsAndIncompleteRows() {
        val words = listOf(
            WordEntity(id = 1, term = "allocate", meaning = "phân bổ"),
            WordEntity(id = 2, term = "allocate", meaning = "chia"),
            WordEntity(id = 3, term = "assign", meaning = "phân bổ"),
            WordEntity(id = 4, term = "sustainable", meaning = "bền vững"),
            WordEntity(id = 5, term = "", meaning = "trống")
        )
        val pairs = ActiveLearningLogic.matchPairs(words, 6)
        assertEquals(listOf(1L, 4L), pairs.map { it.wordId })
    }
}
