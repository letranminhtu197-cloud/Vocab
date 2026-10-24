package vn.luyentu.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

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
    val easeFactor: Double = 2.5,
    val lapses: Int = 0,
    val learningStep: Int = 0
)

data class ReviewHistoryItem(val term: String, val grade: Int, val reviewedAt: Long)

class VocabularyDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

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
                last_reviewed_at INTEGER NOT NULL DEFAULT 0,
                lapses INTEGER NOT NULL DEFAULT 0,
                learning_step INTEGER NOT NULL DEFAULT 0
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
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE vocabulary ADD COLUMN lapses INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE vocabulary ADD COLUMN learning_step INTEGER NOT NULL DEFAULT 0")
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
            arrayOf("id", "term", "meaning", "example", "ipa", "topic", "next_review_at", "interval_days", "repetitions", "ease_factor", "lapses", "learning_step"),
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
                    easeFactor = cursor.getDouble(9),
                    lapses = cursor.getInt(10),
                    learningStep = cursor.getInt(11)
                )
            }
        }
        return result
    }

    @Synchronized
    fun getDueWords(limit: Int = 30): List<VocabularyWord> {
        val now = System.currentTimeMillis()
        return getAllWords().asSequence().filter { it.nextReviewAt <= now }.sortedBy { it.nextReviewAt }.take(limit).toList()
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
        val nextLapses = word.lapses + if (safeGrade == 0) 1 else 0
        val nextStep: Int
        val delayMs: Long

        when (safeGrade) {
            0 -> {
                nextRepetitions = 0
                nextInterval = 0
                nextEase = (word.easeFactor - 0.2).coerceAtLeast(1.3)
                nextStep = 0
                delayMs = 10 * 60 * 1000L
            }
            1 -> {
                nextRepetitions = word.repetitions
                nextInterval = if (word.intervalDays <= 1) 1 else (word.intervalDays * 1.2).toInt().coerceAtLeast(1)
                nextEase = (word.easeFactor - 0.15).coerceAtLeast(1.3)
                nextStep = 1
                delayMs = if (word.repetitions == 0) 12 * 60 * 60 * 1000L else nextInterval * DAY_MS
            }
            2, 3 -> {
                nextRepetitions = word.repetitions + 1
                nextEase = if (safeGrade == 3) (word.easeFactor + 0.15).coerceAtMost(3.0) else word.easeFactor
                nextInterval = when {
                    word.repetitions == 0 -> if (safeGrade == 3) 4 else 1
                    word.repetitions == 1 -> if (safeGrade == 3) 7 else 3
                    else -> (word.intervalDays * nextEase * if (safeGrade == 3) 1.3 else 1.0).toInt().coerceAtLeast(1)
                }
                nextStep = 0
                delayMs = nextInterval * DAY_MS
            }
            else -> return
        }

        val db = writableDatabase
        db.beginTransaction()
        try {
            db.update("vocabulary", ContentValues().apply {
                put("next_review_at", now + delayMs)
                put("interval_days", nextInterval)
                put("repetitions", nextRepetitions)
                put("ease_factor", nextEase)
                put("last_reviewed_at", now)
                put("lapses", nextLapses)
                put("learning_step", nextStep)
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
    fun updateWord(id: Long, term: String, meaning: String, example: String = "", ipa: String = "", topic: String = ""): Boolean {
        val cleanTerm = term.trim()
        val cleanMeaning = meaning.trim()
        if (cleanTerm.isBlank() || cleanMeaning.isBlank()) return false
        return try {
            val values = ContentValues().apply {
                put("term", cleanTerm)
                put("meaning", cleanMeaning)
                put("example", example.trim())
                put("ipa", ipa.trim())
                put("topic", topic.trim())
            }
            writableDatabase.update("vocabulary", values, "id = ?", arrayOf(id.toString())) > 0
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            false
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

    fun masteredWordCount(): Int = readableDatabase.rawQuery(
        "SELECT COUNT(*) FROM vocabulary WHERE repetitions >= 3 AND interval_days >= 7", null
    ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    fun totalLapses(): Int = readableDatabase.rawQuery(
        "SELECT COALESCE(SUM(lapses), 0) FROM vocabulary", null
    ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    fun currentStreakDays(): Int {
        val reviewedDays = mutableSetOf<String>()
        readableDatabase.rawQuery(
            "SELECT DISTINCT date(reviewed_at / 1000, 'unixepoch', 'localtime') FROM review_log",
            null
        ).use { cursor -> while (cursor.moveToNext()) reviewedDays += cursor.getString(0) }
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendar = Calendar.getInstance()
        val today = format.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = format.format(calendar.time)
        if (today !in reviewedDays && yesterday !in reviewedDays) return 0
        if (today !in reviewedDays) calendar.timeInMillis = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
        var streak = 0
        while (format.format(calendar.time) in reviewedDays) {
            streak++
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    fun reviewedToday(): Int {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        return readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM review_log WHERE reviewed_at >= ?",
            arrayOf(start.toString())
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    fun dailyReviewCounts(): List<Pair<String, Int>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val labelFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val result = mutableListOf<Pair<String, Int>>()
        repeat(7) {
            val start = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val end = calendar.timeInMillis
            val count = readableDatabase.rawQuery(
                "SELECT COUNT(*) FROM review_log WHERE reviewed_at >= ? AND reviewed_at < ?",
                arrayOf(start.toString(), end.toString())
            ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
            result += labelFormat.format(Date(start)) to count
        }
        return result
    }

    fun recentReviewHistory(limit: Int = 30): List<ReviewHistoryItem> {
        val result = mutableListOf<ReviewHistoryItem>()
        readableDatabase.rawQuery(
            "SELECT vocabulary.term, review_log.grade, review_log.reviewed_at " +
                "FROM review_log INNER JOIN vocabulary ON vocabulary.id = review_log.word_id " +
                "ORDER BY review_log.reviewed_at DESC LIMIT ?",
            arrayOf(limit.coerceIn(1, 200).toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += ReviewHistoryItem(cursor.getString(0), cursor.getInt(1), cursor.getLong(2))
            }
        }
        return result
    }

    @Synchronized
    fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("format", "luyentu-backup")
        root.put("schemaVersion", 1)
        root.put("exportedAt", System.currentTimeMillis())
        val vocabulary = JSONArray()
        readableDatabase.rawQuery("SELECT * FROM vocabulary ORDER BY id", null).use { cursor ->
            while (cursor.moveToNext()) {
                val item = JSONObject()
                for (i in 0 until cursor.columnCount) {
                    val name = cursor.getColumnName(i)
                    when (cursor.getType(i)) {
                        android.database.Cursor.FIELD_TYPE_INTEGER -> item.put(name, cursor.getLong(i))
                        android.database.Cursor.FIELD_TYPE_FLOAT -> item.put(name, cursor.getDouble(i))
                        android.database.Cursor.FIELD_TYPE_STRING -> item.put(name, cursor.getString(i))
                        android.database.Cursor.FIELD_TYPE_NULL -> item.put(name, JSONObject.NULL)
                    }
                }
                vocabulary.put(item)
            }
        }
        val reviews = JSONArray()
        readableDatabase.rawQuery("SELECT id, word_id, grade, reviewed_at FROM review_log ORDER BY id", null).use { cursor ->
            while (cursor.moveToNext()) {
                reviews.put(JSONObject().put("id", cursor.getLong(0)).put("word_id", cursor.getLong(1))
                    .put("grade", cursor.getInt(2)).put("reviewed_at", cursor.getLong(3)))
            }
        }
        root.put("vocabulary", vocabulary)
        root.put("review_log", reviews)
        return root.toString(2)
    }

    @Synchronized
    fun restoreBackupJson(json: String): Pair<Int, Int> {
        val root = JSONObject(json)
        require(root.optString("format") == "luyentu-backup") { "Tệp không phải bản sao lưu LuyenTu." }
        require(root.optInt("schemaVersion") == 1) { "Phiên bản bản sao lưu không được hỗ trợ." }
        val vocabulary = root.getJSONArray("vocabulary")
        val reviews = root.getJSONArray("review_log")
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("review_log", null, null)
            db.delete("vocabulary", null, null)
            db.execSQL("DELETE FROM sqlite_sequence WHERE name IN ('vocabulary', 'review_log')")
            for (i in 0 until vocabulary.length()) {
                val item = vocabulary.getJSONObject(i)
                val term = item.optString("term").trim()
                val meaning = item.optString("meaning").trim()
                require(term.isNotBlank() && meaning.isNotBlank()) { "Bản sao lưu có từ hoặc nghĩa rỗng." }
                val values = ContentValues().apply {
                    put("id", item.getLong("id")); put("term", term); put("meaning", meaning)
                    put("example", item.optString("example")); put("ipa", item.optString("ipa"))
                    put("topic", item.optString("topic")); put("created_at", item.optLong("created_at"))
                    put("next_review_at", item.optLong("next_review_at")); put("interval_days", item.optInt("interval_days"))
                    put("repetitions", item.optInt("repetitions")); put("ease_factor", item.optDouble("ease_factor", 2.5))
                    put("last_reviewed_at", item.optLong("last_reviewed_at")); put("lapses", item.optInt("lapses"))
                    put("learning_step", item.optInt("learning_step"))
                }
                db.insertOrThrow("vocabulary", null, values)
            }
            for (i in 0 until reviews.length()) {
                val item = reviews.getJSONObject(i)
                val values = ContentValues().apply {
                    put("id", item.getLong("id")); put("word_id", item.getLong("word_id"))
                    put("grade", item.getInt("grade").coerceIn(0, 3)); put("reviewed_at", item.getLong("reviewed_at"))
                }
                db.insertOrThrow("review_log", null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return vocabulary.length() to reviews.length()
    }

    companion object {
        private const val DATABASE_NAME = "luyentu_personal.db"
        private const val DATABASE_VERSION = 3
        private const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
