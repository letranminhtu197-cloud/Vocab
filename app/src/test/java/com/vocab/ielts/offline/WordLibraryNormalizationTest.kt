package com.vocab.ielts.offline

import org.junit.Assert.assertEquals
import org.junit.Test

class WordLibraryNormalizationTest {
    @Test
    fun normalizationTrimsAndCollapsesWhitespace() {
        assertEquals("take into account", WordLibraryRepository.normalizeTerm("  TAKE   into account  "))
    }

    @Test
    fun normalizationUsesUnicodeCompatibilityNormalization() {
        assertEquals(
            WordLibraryRepository.normalizeTerm("café"),
            WordLibraryRepository.normalizeTerm("cafe\u0301")
        )
    }

    @Test
    fun blankTermIsNotNormalizedIntoAValidWord() {
        assertEquals("", WordLibraryRepository.normalizeTerm("   "))
    }
}
