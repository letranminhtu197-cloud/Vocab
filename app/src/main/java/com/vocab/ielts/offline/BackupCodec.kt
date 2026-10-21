package com.vocab.ielts.offline

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Portable, versioned local archive. No network or external library required. */
object BackupCodec {
    private const val MANIFEST = "vocab-backup.txt"
    private const val WORDS = "words.csv"
    private const val MAX_BYTES = 20 * 1024 * 1024

    fun create(words: List<WordEntity>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST))
            zip.write("format=VocabOfflineBackup\nversion=1\n".toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(WORDS))
            zip.write(CsvCodec.export(words).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return output.toByteArray()
    }

    fun restore(bytes: ByteArray): CsvCodec.ImportResult {
        if (bytes.size > MAX_BYTES) return CsvCodec.ImportResult(emptyList(), 0, "File sao lưu vượt giới hạn 20 MB.")
        var manifest: String? = null
        var csv: String? = null
        var expanded = 0
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory && (entry.name == MANIFEST || entry.name == WORDS)) {
                        if ((entry.name == MANIFEST && manifest != null) || (entry.name == WORDS && csv != null)) {
                            return CsvCodec.ImportResult(emptyList(), 0, "File sao lưu có mục dữ liệu bị lặp.")
                        }
                        val out = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = zip.read(buffer)
                            if (count < 0) break
                            expanded += count
                            if (expanded > MAX_BYTES) return CsvCodec.ImportResult(emptyList(), 0, "Dữ liệu giải nén vượt giới hạn an toàn.")
                            out.write(buffer, 0, count)
                        }
                        val value = decodeUtf8(out.toByteArray())
                        if (entry.name == MANIFEST) manifest = value
                        if (entry.name == WORDS) csv = value
                    }
                    zip.closeEntry()
                }
            }
        } catch (_: IOException) {
            return CsvCodec.ImportResult(emptyList(), 0, "File sao lưu bị hỏng, chứa văn bản không hợp lệ hoặc không phải ZIP hợp lệ.")
        }
        val metadata = manifest ?: return CsvCodec.ImportResult(emptyList(), 0, "Không nhận diện được file sao lưu Vocab.")
        if (metadata.lineSequence().none { it.trim() == "format=VocabOfflineBackup" } ||
            metadata.lineSequence().none { it.trim() == "version=1" }) {
            return CsvCodec.ImportResult(emptyList(), 0, "Định dạng hoặc phiên bản file sao lưu không được hỗ trợ.")
        }
        val payload = csv ?: return CsvCodec.ImportResult(emptyList(), 0, "File sao lưu không có dữ liệu từ vựng.")
        return CsvCodec.parse(payload)
    }

    private fun decodeUtf8(bytes: ByteArray): String =
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
}
