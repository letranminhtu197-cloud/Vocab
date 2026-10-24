package vn.luyentu.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerNormalizerTest {
    @Test
    fun ignoresCaseAndRepeatedWhitespace() {
        assertTrue(AnswerNormalizer.matches("  Take   into account ", "take into account"))
    }

    @Test
    fun ignoresTrailingSentencePunctuation() {
        assertTrue(AnswerNormalizer.matches("Resilient!", "resilient"))
    }

    @Test
    fun doesNotAcceptDifferentWords() {
        assertFalse(AnswerNormalizer.matches("affect", "effect"))
    }
}
