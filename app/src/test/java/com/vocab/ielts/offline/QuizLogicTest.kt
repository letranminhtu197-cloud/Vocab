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

    @Test fun blankAnswersAreNeverAccepted() {
        assertFalse(QuizLogic.isCorrectAnswer("", "sustainable"))
        assertFalse(QuizLogic.isCorrectAnswer("   ", "sustainable"))
    }


    @Test fun answerComparisonIgnoresCaseAndPunctuation() {
        assertTrue(QuizLogic.isCorrectAnswer("  SUSTAINABLE! ", "sustainable"))
        assertFalse(QuizLogic.isCorrectAnswer("sustain", "sustainable"))
    }
}