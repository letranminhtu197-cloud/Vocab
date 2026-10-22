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

}
