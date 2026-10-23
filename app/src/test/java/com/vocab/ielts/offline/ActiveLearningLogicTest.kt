package com.vocab.ielts.offline

import org.junit.Assert.*
import org.junit.Test

class ActiveLearningLogicTest {
    @Test fun selectorNeverChoosesInvalidExercise() {
        val chosen = ActiveLearningLogic.chooseNext(
            listOf(
                ExerciseEvidence(ActiveExerciseType.CLOZE, valid = false),
                ExerciseEvidence(ActiveExerciseType.MULTIPLE_CHOICE, valid = true)
            ), nowMillis = 1000L
        )
        assertEquals(ActiveExerciseType.MULTIPLE_CHOICE, chosen)
    }

    @Test fun selectorReturnsNullWhenNoExerciseIsValid() {
        assertNull(ActiveLearningLogic.chooseNext(emptyList(), 1000L))
    }

    @Test fun fourChoiceValidationRequiresFourUniqueOptionsAndOneTarget() {
        assertTrue(ActiveLearningLogic.hasFourValidChoices(
            listOf("allocate", "assign", "reduce", "maintain"), "allocate"
        ))
        assertFalse(ActiveLearningLogic.hasFourValidChoices(
            listOf("allocate", "assign", "assign", "maintain"), "allocate"
        ))
        assertFalse(ActiveLearningLogic.hasFourValidChoices(
            listOf("allocate", "assign", "reduce"), "allocate"
        ))
    }

    @Test fun clozeRequiresExactlyOneWholeTermOccurrence() {
        assertEquals("We must _____ resources.", ActiveLearningLogic.validatedCloze(
            "We must allocate resources.", "allocate"
        ))
        assertNull(ActiveLearningLogic.validatedCloze(
            "We allocate resources and allocate budgets.", "allocate"
        ))
        assertNull(ActiveLearningLogic.validatedCloze(
            "The artist arrived.", "art"
        ))
    }

    @Test fun matchPairsSkipMissingAndDuplicateData() {
        val words = listOf(
            WordEntity(id = 1, term = "allocate", meaning = "phân bổ"),
            WordEntity(id = 2, term = "ALLOCATE", meaning = "phân công"),
            WordEntity(id = 3, term = "reduce", meaning = "giảm"),
            WordEntity(id = 4, term = "empty", meaning = "")
        )
        val pairs = ActiveLearningLogic.matchPairs(words)
        assertEquals(listOf(1L, 3L), pairs.map { it.wordId })
    }
}
