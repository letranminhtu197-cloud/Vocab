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
    fun parsesPlainTextPairs() {
        val rows = CsvTools.parse("resilient - kiên cường\nmeticulous: tỉ mỉ")
        assertEquals(2, rows.size)
        assertEquals("resilient", rows[0].term)
        assertEquals("tỉ mỉ", rows[1].meaning)
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
