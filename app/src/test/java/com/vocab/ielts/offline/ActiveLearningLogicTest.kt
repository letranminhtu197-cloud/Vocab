package com.vocab.ielts.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveLearningLogicTest {
    @Test fun multipleChoiceRequiresFourDistinctNonBlankLabelsAndOneTarget() {
        assertTrue(ActiveLearningLogic.hasFourValidChoices(
            listOf("allocate", "assign", "sustainable", "reduce"), "allocate"
        ))
        assertFalse(ActiveLearningLogic.hasFourValidChoices(
            listOf("allocate", "assign", "assign", "reduce"), "allocate"
        ))
        assertFalse(ActiveLearningLogic.hasFourValidChoices(
            listOf("allocate", "assign", "", "reduce"), "allocate"
        ))
        assertFalse(ActiveLearningLogic.hasFourValidChoices(
            listOf("allocate", "assign", "sustainable", "reduce"), "missing"
        ))
    }

    @Test fun multipleChoicePrefersPlausibleDistinctDistractorsAndAvoidsKnownSynonyms() {
        val target = WordEntity(id = 1, term = "allocate", meaning = "phân bổ", partOfSpeech = "verb", synonyms = "assign; allot")
        val pool = listOf(
            target,
            WordEntity(id = 2, term = "assign", meaning = "giao; phân công", partOfSpeech = "verb"),
            WordEntity(id = 3, term = "allot", meaning = "chia phần", partOfSpeech = "verb"),
            WordEntity(id = 4, term = "reduce", meaning = "giảm", partOfSpeech = "verb"),
            WordEntity(id = 5, term = "sustainable", meaning = "bền vững", partOfSpeech = "adjective"),
            WordEntity(id = 6, term = "consume", meaning = "tiêu thụ", partOfSpeech = "verb")
        )
        val options = QuizLogic.multipleChoiceOptions(target, pool, "Chọn nghĩa")
        assertEquals(4, options.size)
        assertEquals(1, options.count { it.id == target.id })
        assertTrue(options.none { it.term == "assign" || it.term == "allot" })
        assertEquals(4, options.map { QuizLogic.normalizeAnswer(it.meaning) }.distinct().size)
    }

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
                    ExerciseEvidence(ActiveExerciseType.MULTIPLE_CHOICE, false),
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
