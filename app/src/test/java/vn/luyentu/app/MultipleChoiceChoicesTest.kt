package vn.luyentu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MultipleChoiceChoicesTest {
    @Test
    fun includesCorrectAnswerOnceAndReturnsAtMostFourChoices() {
        val choices = MultipleChoiceChoices.create(
            "  significant ",
            listOf("important", "meaningful", "essential", "crucial", "significant")
        )
        assertEquals(4, choices.size)
        assertEquals(1, choices.count { it.equals("significant", ignoreCase = true) })
    }

    @Test
    fun removesCaseAndWhitespaceEquivalentMeanings() {
        val choices = MultipleChoiceChoices.create(
            "a great deal",
            listOf("much", "A GREAT   DEAL", " much ", "a lot", "considerably")
        )
        assertEquals(4, choices.size)
        assertEquals(1, choices.count { it.trim().equals("a great deal", ignoreCase = true) })
        assertEquals(choices.size, choices.map { it.trim().lowercase().replace(Regex("\\s+"), " ") }.distinct().size)
    }

    @Test
    fun gracefullyReturnsFewerThanFourWhenNotEnoughDistinctMeanings() {
        val choices = MultipleChoiceChoices.create("rapid", listOf("fast", " FAST ", "rapid"))
        assertEquals(2, choices.size)
        assertTrue(choices.any { it == "rapid" })
        assertFalse(choices.any { it.isBlank() })
    }

    @Test
    fun blankCorrectAnswerReturnsNoChoices() {
        assertTrue(MultipleChoiceChoices.create("  ", listOf("fast", "quick")).isEmpty())
    }
}
