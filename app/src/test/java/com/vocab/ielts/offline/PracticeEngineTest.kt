package com.vocab.ielts.offline

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class PracticeEngineTest {
    private val target = WordEntity(
        id = 1, term = "sustainable", meaning = "bền vững",
        partOfSpeech = "adjective", topic = "Environment",
        example = "A sustainable city reduces waste."
    )
    private val pool = listOf(
        target,
        WordEntity(id = 2, term = "scarce", meaning = "khan hiếm"),
        WordEntity(id = 3, term = "resilient", meaning = "kiên cường"),
        WordEntity(id = 4, term = "mitigate", meaning = "giảm nhẹ"),
        WordEntity(id = 5, term = "abundant", meaning = "dồi dào")
    )

    @Test fun multipleChoiceHasUniqueOptionsAndCorrectAnswer() {
        val question = PracticeEngine.createQuestion(target, pool, PracticeMode.MULTIPLE_CHOICE, Random(7))!!
        assertEquals(4, question.options.size)
        assertEquals(4, question.options.map(PracticeEngine::normalizeAnswer).distinct().size)
        assertTrue(question.options.contains(target.meaning))
        assertTrue(PracticeEngine.grade(question, target.meaning).correct)
    }

    @Test fun multipleChoiceRequiresAtLeastOneDistractor() {
        assertNull(PracticeEngine.createQuestion(target, listOf(target), PracticeMode.MULTIPLE_CHOICE))
    }

    @Test fun typingIgnoresCaseAndExtraWhitespace() {
        val question = PracticeEngine.createQuestion(target, pool, PracticeMode.LISTEN_AND_TYPE)!!
        assertTrue(PracticeEngine.grade(question, "  SUSTAINABLE  ").correct)
        assertFalse(PracticeEngine.grade(question, "sustainability").correct)
    }

    @Test fun contextualFillBlanksTargetTermCaseInsensitively() {
        val question = PracticeEngine.createQuestion(target, pool, PracticeMode.CONTEXTUAL_FILL)!!
        assertEquals("A ________ city reduces waste.", question.prompt)
        assertEquals("sustainable", question.expectedAnswer)
        assertTrue(PracticeEngine.grade(question, "Sustainable").correct)
    }

    @Test fun contextualFillUnavailableWithoutMatchingExample() {
        val withoutMatch = target.copy(example = "The city reduces waste.")
        assertNull(PracticeEngine.createQuestion(withoutMatch, pool, PracticeMode.CONTEXTUAL_FILL))
    }

    @Test fun emptyAnswerIsIncorrectAndProvidesExpectedAnswer() {
        val question = PracticeEngine.createQuestion(target, pool, PracticeMode.LISTEN_AND_TYPE)!!
        val result = PracticeEngine.grade(question, " ")
        assertFalse(result.correct)
        assertTrue(result.feedback.contains("sustainable"))
    }
}
