package com.vocab.ielts.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrambleLogicTest {
    @Test fun scrambleKeepsEveryLetterAndLength() {
        val term = "sustainable"
        val scrambled = ScrambleLogic.scramble(term, 42L)

        assertEquals(term.length, scrambled.length)
        assertEquals(term.toList().sorted(), scrambled.toList().sorted())
    }

    @Test fun scrambleIsDeterministicForTheSameSeed() {
        assertEquals(
            ScrambleLogic.scramble("allocate", 7L),
            ScrambleLogic.scramble("allocate", 7L)
        )
    }

    @Test fun scrambleUsuallyChangesLetterOrder() {
        assertNotEquals("allocate", ScrambleLogic.scramble("allocate", 3L))
    }

    @Test fun scrambleKeepsWordBoundariesForPhrases() {
        val scrambled = ScrambleLogic.scramble("ice cream", 11L)
        val parts = scrambled.split(" ")

        assertEquals(2, parts.size)
        assertTrue(parts.all { it.isNotBlank() })
        assertEquals("ice cream".replace(" ", "").toList().sorted(), scrambled.replace(" ", "").toList().sorted())
    }

    @Test fun veryShortWordsAreLeftUntouched() {
        assertEquals("a", ScrambleLogic.scramble("a", 1L))
        assertEquals("go", ScrambleLogic.scramble("go", 1L))
    }

    @Test fun answersUseTheSharedAnswerNormalization() {
        assertTrue(ScrambleLogic.isAnswerCorrect("wellbeing", "well-being"))
        assertTrue(!ScrambleLogic.isAnswerCorrect("well", "well-being"))
    }
}
