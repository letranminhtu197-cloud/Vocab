package com.vocab.ielts.offline

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.text.Normalizer
import java.util.Locale

/** Domain boundary for local vocabulary CRUD and safe bulk import. No network dependency. */
class WordLibraryRepository(private val database: VocabDatabase) {
    private val words = database.wordDao()
    private val categories = database.categoryDao()

    fun observeWords(): Flow<List<WordEntity>> = words.observeAll()
    fun observeCategories(): Flow<List<CategoryEntity>> = categories.observeAll()
    fun observeDueWords(nowMillis: Long = System.currentTimeMillis()): Flow<List<WordEntity>> =
        words.observeDue(nowMillis)

    suspend fun addWord(word: WordEntity): Long {
        validateWord(word)
        val trimmed = word.term.trim()
        val duplicate = words.getAllSnapshot().firstOrNull { normalizeTerm(it.term) == normalizeTerm(trimmed) }
        require(duplicate == null) { "Từ '${trimmed}' đã tồn tại." }
        return words.insert(word.copy(term = trimmed))
    }

    suspend fun updateWord(word: WordEntity) {
        validateWord(word)
        require(words.findById(word.id) != null) { "Không tìm thấy từ cần cập nhật." }
        val duplicate = words.getAllSnapshot().firstOrNull {
            normalizeTerm(it.term) == normalizeTerm(word.term) && it.id != word.id
        }
        require(duplicate == null) {
            "Một từ khác có cùng cách viết đã tồn tại."
        }
        words.update(word.copy(term = word.term.trim()))
    }

    suspend fun deleteWord(word: WordEntity) = words.delete(word)

    suspend fun createCategory(name: String, description: String = "", colorArgb: Long? = null): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Tên danh mục không được để trống." }
        val normalized = normalizeTerm(trimmed)
        require(categories.findByNormalizedName(normalized) == null) {
            "Danh mục '${trimmed}' đã tồn tại."
        }
        val now = System.currentTimeMillis()
        return categories.insert(CategoryEntity(id = 0, name = trimmed, normalizedName = normalized, description = description.trim(), colorArgb = colorArgb, createdAtMillis = now, updatedAtMillis = now))
    }

    suspend fun updateCategory(category: CategoryEntity, name: String, description: String, colorArgb: Long?) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Tên danh mục không được để trống." }
        require(categories.findById(category.id) != null) { "Không tìm thấy danh mục." }
        val normalized = normalizeTerm(trimmed)
        val duplicate = categories.findByNormalizedName(normalized)
        require(duplicate == null || duplicate.id == category.id) { "Tên danh mục đã tồn tại." }
        categories.update(category.copy(
            name = trimmed,
            normalizedName = normalized,
            description = description.trim(),
            colorArgb = colorArgb,
            updatedAtMillis = System.currentTimeMillis()
        ))
    }

    suspend fun deleteCategory(category: CategoryEntity) = categories.delete(category)

    /** Inserts a batch atomically; duplicate rows inside the file are rejected. */
    suspend fun importWordsAtomically(imported: List<WordEntity>): ImportResult {
        require(imported.isNotEmpty()) { "File không có từ hợp lệ để nhập." }
        val seen = mutableSetOf<String>()
        imported.forEachIndexed { index, word ->
            try {
                validateWord(word)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Dòng ${index + 1}: ${e.message}", e)
            }
            require(seen.add(normalizeTerm(word.term))) {
                "File có từ trùng lặp: '${word.term.trim()}'."
            }
        }
        return database.withTransaction {
            val existing = words.getAllSnapshot()
            val existingKeys = existing.map { normalizeTerm(it.term) }.toSet()
            val fresh = imported.filter { normalizeTerm(it.term) !in existingKeys }
            if (fresh.isNotEmpty()) words.insertAll(fresh.map { it.copy(id = 0, term = it.term.trim()) })
            ImportResult(inserted = fresh.size, skippedExisting = imported.size - fresh.size)
        }
    }

    suspend fun wordCount(): Int = words.count()
    suspend fun categoryCount(): Int = categories.count()

    private fun validateWord(word: WordEntity) {
        require(word.term.isNotBlank()) { "Từ vựng không được để trống." }
        require(word.meaning.isNotBlank()) { "Nghĩa của từ không được để trống." }
        require(word.srsLevel in 0..5) { "Cấp độ SRS phải nằm trong khoảng 0–5." }
        require(word.reviewCount >= 0 && word.lapseCount >= 0) { "Số lần ôn không hợp lệ." }
    }

    companion object {
        fun normalizeTerm(value: String): String =
            Normalizer.normalize(value.trim(), Normalizer.Form.NFKC)
                .lowercase(Locale.ROOT)
                .replace(Regex("\\s+"), " ")
    }
}

data class ImportResult(val inserted: Int, val skippedExisting: Int)
