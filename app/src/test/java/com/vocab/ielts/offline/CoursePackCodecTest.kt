package com.vocab.ielts.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleJsonTest {
    @Test fun parsesNestedObjectsAndArrays() {
        val value = SimpleJson.parse("""{"a":{"b":[1,2,"x"]},"ok":true}""") as Map<*, *>

        assertTrue(value["ok"] as Boolean)
        val inner = value["a"] as Map<*, *>
        val list = inner["b"] as List<*>
        assertEquals(3, list.size)
        assertEquals("x", list[2])
    }

    @Test fun handlesEscapesInStrings() {
        val value = SimpleJson.parse("""{"text":"dòng 1\n\"trích dẫn\" \u0041"}""") as Map<*, *>

        assertEquals("dòng 1\n\"trích dẫn\" A", value["text"])
    }

    @Test fun malformedJsonThrows() {
        var thrown = false
        try {
            SimpleJson.parse("""{"a": }""")
        } catch (_: IllegalArgumentException) {
            thrown = true
        }
        assertTrue(thrown)
    }
}

class CoursePackCodecTest {
    @Test fun parsesLessonsWithTopicFromLessonTitle() {
        val json = """
            {
              "course": "IELTS Foundation",
              "lessons": [
                { "title": "Education", "words": [
                    { "term": "curriculum", "meaning": "chương trình học" },
                    { "term": "tuition", "meaning": "học phí", "example": "Tuition is rising." }
                ]},
                { "title": "Health", "words": [
                    { "word": "immunity", "nghĩa": "miễn dịch" }
                ]}
              ]
            }
        """.trimIndent()

        val course = CoursePackCodec.parse(json)

        assertNull(course.error)
        assertEquals("IELTS Foundation", course.name)
        assertEquals(2, course.lessonCount)
        assertEquals(3, course.words.size)
        assertEquals("Education", course.words.first().topic)
        assertEquals("Health", course.words.last().topic)
    }

    @Test fun acceptsFlatWordListAndRootArray() {
        val flat = CoursePackCodec.parse("""{"words":[{"term":"a","meaning":"x"},{"term":"b","meaning":"y"}]}""")
        val rootArray = CoursePackCodec.parse("""[{"term":"c","meaning":"z"}]""")

        assertNull(flat.error)
        assertEquals(2, flat.words.size)
        assertNull(rootArray.error)
        assertEquals(1, rootArray.words.size)
    }

    @Test fun invalidEntriesAreCountedNotFatal() {
        val course = CoursePackCodec.parse(
            """{"lessons":[{"title":"T","words":[{"term":"ok","meaning":"được"},{"term":"","meaning":"x"}]}]}"""
        )

        assertNull(course.error)
        assertEquals(1, course.words.size)
        assertEquals(1, course.invalidRows)
    }

    @Test fun malformedJsonReturnsError() {
        val course = CoursePackCodec.parse("{ khong phai json")

        assertTrue(course.error != null)
        assertTrue(course.words.isEmpty())
    }

    @Test fun wordFieldsAreMappedIncludingTranslationAndPaused() {
        val course = CoursePackCodec.parse(
            """
            {"lessons":[{"title":"Work","words":[
              {"term":"wage","meaning":"tiền công","exampleMeaning":"Tiền công tăng.","paused":true,
               "partOfSpeech":"n","collocations":"minimum wage"}
            ]}]}
            """.trimIndent()
        )

        val word = course.words.single()
        assertEquals("n", word.partOfSpeech)
        assertEquals("Tiền công tăng.", word.exampleMeaning)
        assertEquals("minimum wage", word.collocations)
        assertTrue(word.paused)
    }

    @Test fun emptyCourseReturnsActionableError() {
        val course = CoursePackCodec.parse("""{"course":"Empty","lessons":[]}""")

        assertTrue(course.error != null)
        assertTrue(course.error!!.contains("Không đọc được"))
    }
}
