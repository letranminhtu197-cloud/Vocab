package vn.luyentu.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class VocabularyWord(
    val id: Long,
    val term: String,
    val meaning: String,
    val example: String,
    val ipa: String,
    val topic: String,
    val nextReviewAt: Long = 0L,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val easeFactor: Double = 2.5
)

class VocabularyDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE vocabulary (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                term TEXT NOT NULL COLLATE NOCASE UNIQUE,
                meaning TEXT NOT NULL,
                example TEXT NOT NULL DEFAULT '',
                ipa TEXT NOT NULL DEFAULT '',
                topic TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL,
                next_review_at INTEGER NOT NULL DEFAULT 0,
                interval_days INTEGER NOT NULL DEFAULT 0,
                repetitions INTEGER NOT NULL DEFAULT 0,
                ease_factor REAL NOT NULL DEFAULT 2.5,
                last_reviewed_at INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        createReviewLog(db)
    }

    private fun createReviewLog(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS review_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                word_id INTEGER NOT NULL,
                grade INTEGER NOT NULL,
                reviewed_at INTEGER NOT NULL,
                FOREIGN KEY(word_id) REFERENCES vocabulary(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE vocabulary ADD COLUMN next_review_at INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE vocabulary ADD COLUMN interval_days INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE vocabulary ADD COLUMN repetitions INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE vocabulary ADD COLUMN ease_factor REAL NOT NULL DEFAULT 2.5")
            db.execSQL("ALTER TABLE vocabulary ADD COLUMN last_reviewed_at INTEGER NOT NULL DEFAULT 0")
            createReviewLog(db)
        }
    }

    @Synchronized
    fun addWord(term: String, meaning: String, example: String = "", ipa: String = "", topic: String = ""): Boolean {
        val cleanTerm = term.trim()
        val cleanMeaning = meaning.trim()
        if (cleanTerm.isEmpty() || cleanMeaning.isEmpty()) return false
        val values = ContentValues().apply {
            put("term", cleanTerm)
            put("meaning", cleanMeaning)
            put("example", example.trim())
            put("ipa", ipa.trim())
            put("topic", topic.trim())
            put("created_at", System.currentTimeMillis())
        }
        return try {
            writableDatabase.insertOrThrow("vocabulary", null, values) != -1L
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            false
        }
    }

    @Synchronized
    fun getAllWords(query: String = ""): List<VocabularyWord> {
        val result = mutableListOf<VocabularyWord>()
        val cleanQuery = query.trim()
        val selection = if (cleanQuery.isEmpty()) null else "term LIKE ? OR meaning LIKE ? OR topic LIKE ?"
        val args = if (cleanQuery.isEmpty()) null else arrayOf("%$cleanQuery%", "%$cleanQuery%", "%$cleanQuery%")
        readableDatabase.query(
            "vocabulary",
            arrayOf("id", "term", "meaning", "example", "ipa", "topic", "next_review_at", "interval_days", "repetitions", "ease_factor"),
            selection, args, null, null, "term COLLATE NOCASE ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += VocabularyWord(
                    id = cursor.getLong(0),
                    term = cursor.getString(1),
                    meaning = cursor.getString(2),
                    example = cursor.getString(3),
                    ipa = cursor.getString(4),
                    topic = cursor.getString(5),
                    nextReviewAt = cursor.getLong(6),
                    intervalDays = cursor.getInt(7),
                    repetitions = cursor.getInt(8),
                    easeFactor = cursor.getDouble(9)
                )
            }
        }
        return result
    }

    @Synchronized
    fun getDueWords(limit: Int = 30): List<VocabularyWord> {
        val now = System.currentTimeMillis()
        return getAllWords().filter { it.nextReviewAt <= now }.take(limit)
    }

    @Synchronized
    fun reviewWord(wordId: Long, grade: Int) {
        // Grade scale: 0 = Again, 1 = Hard, 2 = Good, 3 = Easy.
        val safeGrade = grade.coerceIn(0, 3)
        val word = getAllWords().firstOrNull { it.id == wordId } ?: return
        val now = System.currentTimeMillis()
        val nextRepetitions: Int
        val nextInterval: Int
        val nextEase: Double

        if (safeGrade == 0) {
            nextRepetitions = 0
            nextInterval = 0
            nextEase = (word.easeFactor - 0.2).coerceAtLeast(1.3)
        } else {
            nextRepetitions = word.repetitions + 1
            nextEase = when (safeGrade) {
                1 -> (word.easeFactor - 0.15).coerceAtLeast(1.3)
                3 -> word.easeFactor + 0.15
                else -> word.easeFactor
            }
            nextInterval = when {
                safeGrade == 1 -> 1
                nextRepetitions == 1 -> 1
                nextRepetitions == 2 -> if (safeGrade == 3) 4 else 3
                else -> (word.intervalDays * nextEase * if (safeGrade == 3) 1.3 else 1.0).toInt().coerceAtLeast(1)
            }
        }

        val dueAt = if (safeGrade == 0) now + 10 * 60 * 1000L else now + nextInterval * DAY_MS
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.update("vocabulary", ContentValues().apply {
                put("next_review_at", dueAt)
                put("interval_days", nextInterval)
                put("repetitions", nextRepetitions)
                put("ease_factor", nextEase)
                put("last_reviewed_at", now)
            }, "id = ?", arrayOf(wordId.toString()))
            db.insert("review_log", null, ContentValues().apply {
                put("word_id", wordId)
                put("grade", safeGrade)
                put("reviewed_at", now)
            })
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    @Synchronized
    fun deleteWord(id: Long) {
        writableDatabase.delete("vocabulary", "id = ?", arrayOf(id.toString()))
    }

    fun wordCount(): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM vocabulary", null).use {
        if (it.moveToFirst()) it.getInt(0) else 0
    }

    fun reviewCount(): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM review_log", null).use {
        if (it.moveToFirst()) it.getInt(0) else 0
    }

    companion object {
        private const val DATABASE_NAME = "luyentu_personal.db"
        private const val DATABASE_VERSION = 2
        private const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
