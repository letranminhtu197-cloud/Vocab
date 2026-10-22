package com.vocab.ielts.offline

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupCodecTest {
    @Test fun zipBackupRoundTripPreservesSrsAndTextFields() {
        val original = WordEntity(
            term = "allocate", ipa = "/ˈæləkeɪt/", partOfSpeech = "verb",
            meaning = "phân bổ", example = "Allocate funds, carefully.",
            topic = "Economy", collocations = "allocate resources", synonyms = "assign; distribute", wordFamily = "allocation; allocator", notes = "Use in Task 2", tags = "economy; education", srsLevel = 3, nextReviewAtMillis = 123456789L,
            reviewCount = 7, lapseCount = 1, createdAtMillis = 111L, lastReviewedAtMillis = 222L
        )
        val restored = BackupCodec.restore(BackupCodec.create(listOf(original)))
        assertNull(restored.error)
        val copy = restored.words.single()
        assertEquals(original.term, copy.term)
        assertEquals(original.example, copy.example)
        assertEquals(original.collocations, copy.collocations)
        assertEquals(original.synonyms, copy.synonyms)
        assertEquals(original.wordFamily, copy.wordFamily)
        assertEquals(original.notes, copy.notes)
        assertEquals(original.tags, copy.tags)
        assertEquals(original.srsLevel, copy.srsLevel)
        assertEquals(original.nextReviewAtMillis, copy.nextReviewAtMillis)
        assertEquals(original.reviewCount, copy.reviewCount)
        assertEquals(original.lapseCount, copy.lapseCount)
        assertEquals(original.createdAtMillis, copy.createdAtMillis)
        assertEquals(original.lastReviewedAtMillis, copy.lastReviewedAtMillis)
    }

    @Test fun rejectsInvalidArchive() {
        assertNotNull(BackupCodec.restore("not a zip".toByteArray()).error)
    }

    @Test fun rejectsArchiveWhoseUnknownEntriesExceedExpandedSizeLimit() {
        val oversized = ByteArray(20 * 1024 * 1024 + 1)
        val archive = zipOf(
            "unknown.bin" to oversized,
            "vocab-backup.txt" to "format=VocabOfflineBackup\nversion=1\n".toByteArray(),
            "words.csv" to "term,meaning\nword,nghĩa".toByteArray(Charsets.UTF_8)
        )

        val result = BackupCodec.restore(archive)

        assertTrue(result.error.orEmpty().contains("Dữ liệu giải nén vượt giới hạn an toàn"))
        assertTrue(result.words.isEmpty())
    }

    @Test fun rejectsUnsupportedBackupVersion() {
        val archive = zipOf(
            "vocab-backup.txt" to "format=VocabOfflineBackup\nversion=99\n".toByteArray(),
            "words.csv" to "term,meaning\nword,nghĩa".toByteArray(Charsets.UTF_8)
        )
        assertTrue(BackupCodec.restore(archive).error.orEmpty().contains("không được hỗ trợ"))
    }

    @Test fun rejectsMalformedUtf8InsteadOfSilentlyChangingText() {
        val archive = zipOf(
            "vocab-backup.txt" to "format=VocabOfflineBackup\nversion=1\n".toByteArray(),
            "words.csv" to byteArrayOf(0x74, 0x65, 0x72, 0x6d, 0x2c, 0x6d, 0x65, 0x61, 0x6e, 0x69, 0x6e, 0x67, 0x0a, 0xC3.toByte(), 0x28, 0x2c, 0x78)
        )
        val result = BackupCodec.restore(archive)
        assertNotNull(result.error)
        assertTrue(result.words.isEmpty())
    }

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }
}
