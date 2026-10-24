package vn.luyentu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvToolsTest {
    @Test
    fun parsesCsvHeaderAndQuotedFields() {
        val rows = CsvTools.parse(
            "term,meaning,ipa,topic,example\n" +
                "resilient,kiên cường,/rɪˈzɪliənt/,Personality,\"She stayed calm, even under pressure.\""
        )
        assertEquals(1, rows.size)
        assertEquals("resilient", rows[0].term)
        assertEquals("kiên cường", rows[0].meaning)
        assertEquals("She stayed calm, even under pressure.", rows[0].example)
    }

    @Test
    fun parsesVietnameseHeadersAndQuotedCommas() {
        val csv = "từ tiếng anh,nghĩa tiếng việt,câu ví dụ,ipa,chủ đề\\n" +
            "\\"break down\\",\\"phân tích, chia nhỏ\\",\\"The plan broke down.\\",/breɪk daʊn/,\\"IELTS, Work\\""
        val words = CsvTools.parse(csv)
        assertEquals(1, words.size)
        assertEquals("break down", words[0].term)
        assertEquals("phân tích, chia nhỏ", words[0].meaning)
        assertEquals("The plan broke down.", words[0].example)
        assertEquals("IELTS, Work", words[0].topic)
    }

    @Test
    fun parsesPlainTextPairs() {
        val rows = CsvTools.parse("resilient - kiên cường\nmeticulous: tỉ mỉ")
        assertEquals(2, rows.size)
        assertEquals("resilient", rows[0].term)
        assertEquals("tỉ mỉ", rows[1].meaning)
    }

    @Test
    fun csvExportWithQuotedCommasCanBeImportedAgain() {
        val original = listOf(
            VocabularyWord(1, "break down", "phân tích, chia nhỏ", "She broke down the task.", "/breɪk daʊn/", "IELTS, Work")
        )
        val imported = CsvTools.parse(CsvTools.export(original))
        assertEquals(1, imported.size)
        assertEquals(original[0].term, imported[0].term)
        assertEquals(original[0].meaning, imported[0].meaning)
        assertEquals(original[0].example, imported[0].example)
        assertEquals(original[0].ipa, imported[0].ipa)
        assertEquals(original[0].topic, imported[0].topic)
    }

    @Test
    fun plainTextExportCanBeImportedAgain() {
        val original = listOf(
            VocabularyWord(1, "take into account", "tính đến", "We must take it into account.", "/...", "Academic")
        )
        val imported = CsvTools.parse(CsvTools.exportPlainText(original))
        assertEquals(1, imported.size)
        assertEquals(original[0].term, imported[0].term)
        assertEquals(original[0].meaning, imported[0].meaning)
        assertEquals(original[0].example, imported[0].example)
        assertEquals(original[0].ipa, imported[0].ipa)
        assertEquals(original[0].topic, imported[0].topic)
        assertTrue(imported[0].term.isNotBlank())
    }
}
