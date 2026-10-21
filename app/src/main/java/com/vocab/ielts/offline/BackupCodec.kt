package com.vocab.ielts.offline

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Local ZIP backup format: a small manifest plus a UTF-8 CSV payload. */
object BackupCodec {
    private const val MANIFEST = "vocab-backup.txt"
    private const val WORDS = "words.csv"
    private const val MAX_UNCOMPRESSED_BYTES = 20 * 1024 * 1024

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
        if (bytes.size > MAX_UNCOMPRESSED_BYTES) {
            return CsvCodec.ImportResult(emptyList(), 0, "File sao lưu quá lớn.")
        }
        var manifest: String? = null
        var csv: String? = null
        var totalBytes = 0
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory && (entry.name == MANIFEST || entry.name == WORDS)) {
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = zip.read(buffer)
                            if (count < 0) break
                            totalBytes += count
                            if (totalBytes > MAX_UNCOMPRESSED_BYTES) {
                                return CsvCodec.ImportResult(emptyList(), 0, "Dữ liệu giải nén vượt giới hạn an toàn.")
                            }
                            output.write(buffer, 0, count)
                        }
                        val content = output.toByteArray().toString(Charsets.UTF_8)
                        if (entry.name == MANIFEST) manifest = content
                        if (entry.name == WORDS) csv = content
                    }
                    zip.closeEntry()
                }
            }
        } catch (e: IOException) {
            return CsvCodec.ImportResult(emptyList(), 0, "File sao lưu bị hỏng hoặc không phải ZIP hợp lệ.")
        }
        val manifestText = manifest
            ?: return CsvCodec.ImportResult(emptyList(), 0, "Không nhận diện được định dạng file sao lưu Vocab.")
        if (manifestText.lineSequence().none { it.trim() == "format=VocabOfflineBackup" } ||
            manifestText.lineSequence().none { it.trim() == "version=1" }) {
            return CsvCodec.ImportResult(emptyList(), 0, "Không nhận diện được định dạng file sao lưu Vocab.")
        }
        if (csv == null) return CsvCodec.ImportResult(emptyList(), 0, "File sao lưu không có dữ liệu từ vựng.")
        return CsvCodec.parse(csv)
    }
}
