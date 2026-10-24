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
    val learningStep: Int = 0,
    val deckId: Long? = null
) {
    val learningLevel: Int
        get() = when {
            repetitions >= 8 && intervalDays >= 60 -> 5
            repetitions >= 5 && intervalDays >= 30 -> 4
            repetitions >= 3 && intervalDays >= 7 -> 3
            repetitions >= 2 && intervalDays >= 3 -> 2
            repetitions >= 1 && intervalDays >= 1 -> 1
            else -> 0
        }
}

data class VocabularyDeck(
    val id: Long,
    val name: String,
    val folderId: Long?,
    val wordCount: Int,
    val dueCount: Int
)

data class VocabularyFolder(val id: Long, val name: String)

data class ReviewHistoryItem(val term: String, val grade: Int, val reviewedAt: Long)

class VocabularyDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        createStudyContainers(db)
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
                learning_step INTEGER NOT NULL DEFAULT 0,
                deck_id INTEGER REFERENCES decks(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )
        createReviewLog(db)
        ensureDefaultDeck(db)
    }

    private fun createStudyContainers(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS folders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL COLLATE NOCASE UNIQUE,
                created_at INTEGER NOT NULL
            )""".trimIndent()
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS decks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL COLLATE NOCASE,
                folder_id INTEGER,
                created_at INTEGER NOT NULL,
                FOREIGN KEY(folder_id) REFERENCES folders(id) ON DELETE SET NULL
            )""".trimIndent()
        )
    }

    private fun ensureDefaultDeck(db: SQLiteDatabase): Long {
        db.rawQuery("SELECT id FROM decks ORDER BY id LIMIT 1", null).use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }
        return db.insertOrThrow("decks", null, ContentValues().apply {
            put("name", "Bộ từ vựng của tôi")
            put("created_at", System.currentTimeMillis())
        })
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
        if (oldVersion < 4) {
            createStudyContainers(db)
            db.execSQL("ALTER TABLE vocabulary ADD COLUMN deck_id INTEGER REFERENCES decks(id) ON DELETE SET NULL")
            val defaultDeck = ensureDefaultDeck(db)
            db.execSQL("UPDATE vocabulary SET deck_id = ? WHERE deck_id IS NULL", arrayOf(defaultDeck))
        }
    }

    /**
     * Imports a complete file in one SQLite transaction to avoid a commit per word.
     * Returns Pair(added, skipped); existing terms are never overwritten.
     */
    @Synchronized
    fun importWords(rows: List<CsvTools.CsvWord>, deckId: Long? = null): Pair<Int, Int> {
        val db = writableDatabase
        var added = 0
        var skipped = 0
        db.beginTransaction()
        try {
            val targetDeck = deckId ?: ensureDefaultDeck(db)
            rows.forEach { row ->
                val term = row.term.trim()
                val meaning = row.meaning.trim()
                if (term.isBlank() || meaning.isBlank()) {
                    skipped++
                } else {
                    val values = ContentValues().apply {
                        put("term", term)
                        put("meaning", meaning)
                        put("example", row.example.trim())
                        put("ipa", row.ipa.trim())
                        put("topic", row.topic.trim())
                        val createdAt = System.currentTimeMillis()
                        put("created_at", createdAt)
                        put("next_review_at", createdAt)
                        put("deck_id", targetDeck)
                    }
                    val inserted = db.insertWithOnConflict(
                        "vocabulary", null, values, SQLiteDatabase.CONFLICT_IGNORE
                    )
                    if (inserted == -1L) skipped++ else added++
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return added to skipped
    }

    @Synchronized
    fun addWord(term: String, meaning: String, example: String = "", ipa: String = "", topic: String = "", deckId: Long? = null): Boolean {
        val cleanTerm = term.trim()
        val cleanMeaning = meaning.trim()
        if (cleanTerm.isEmpty() || cleanMeaning.isEmpty()) return false
        val values = ContentValues().apply {
            put("term", cleanTerm)
            put("meaning", cleanMeaning)
            put("example", example.trim())
            put("ipa", ipa.trim())
            put("topic", topic.trim())
            val createdAt = System.currentTimeMillis()
            put("created_at", createdAt)
            put("next_review_at", createdAt)
            put("deck_id", deckId ?: ensureDefaultDeck(writableDatabase))
        }
        return try {
            writableDatabase.insertOrThrow("vocabulary", null, values) != -1L
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            false
        }
    }

    @Synchronized
    fun getAllWords(query: String = "", deckId: Long? = null): List<VocabularyWord> {
        val result = mutableListOf<VocabularyWord>()
        val cleanQuery = query.trim()
        val clauses = mutableListOf<String>()
        val args = mutableListOf<String>()
        if (cleanQuery.isNotEmpty()) {
            clauses += "(term LIKE ? OR meaning LIKE ? OR topic LIKE ?)"
            repeat(3) { args += "%$cleanQuery%" }
        }
        if (deckId != null) {
            clauses += "deck_id = ?"
            args += deckId.toString()
        }
        val selection = clauses.takeIf { it.isNotEmpty() }?.joinToString(" AND ")
        readableDatabase.query(
            "vocabulary",
            arrayOf("id", "term", "meaning", "example", "ipa", "topic", "next_review_at", "interval_days", "repetitions", "ease_factor", "lapses", "learning_step", "deck_id"),
            selection, args.takeIf { it.isNotEmpty() }?.toTypedArray(), null, null, "term COLLATE NOCASE ASC"
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
                    learningStep = cursor.getInt(11),
                    deckId = if (cursor.isNull(12)) null else cursor.getLong(12)
                )
            }
        }
        return result
    }

    @Synchronized
    fun getDueWords(limit: Int = Int.MAX_VALUE): List<VocabularyWord> {
        val now = System.currentTimeMillis()
        return getAllWords().asSequence().filter { it.nextReviewAt <= now }.sortedBy { it.nextReviewAt }.take(limit).toList()
    }

    @Synchronized
    fun reviewWord(wordId: Long, grade: Int) {
        val word = getAllWords().firstOrNull { it.id == wordId } ?: return
        val schedule = SrsScheduler.calculate(word, grade, System.currentTimeMillis())
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.update("vocabulary", ContentValues().apply {
                put("next_review_at", schedule.nextReviewAt)
                put("interval_days", schedule.intervalDays)
                put("repetitions", schedule.repetitions)
                put("ease_factor", schedule.easeFactor)
                put("last_reviewed_at", schedule.reviewedAt)
                put("lapses", schedule.lapses)
                put("learning_step", schedule.learningStep)
            }, "id = ?", arrayOf(wordId.toString()))
            db.insert("review_log", null, ContentValues().apply {
                put("word_id", wordId)
                put("grade", schedule.grade)
                put("reviewed_at", schedule.reviewedAt)
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

    @Synchronized
    fun getDecks(): List<VocabularyDeck> {
        val result = mutableListOf<VocabularyDeck>()
        val now = System.currentTimeMillis()
        readableDatabase.rawQuery(
            """SELECT d.id, d.name, d.folder_id, COUNT(v.id),
                SUM(CASE WHEN v.id IS NOT NULL AND v.next_review_at <= $now THEN 1 ELSE 0 END)
                FROM decks d LEFT JOIN vocabulary v ON v.deck_id = d.id
                GROUP BY d.id, d.name, d.folder_id ORDER BY d.id""".trimIndent(), null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += VocabularyDeck(cursor.getLong(0), cursor.getString(1),
                    if (cursor.isNull(2)) null else cursor.getLong(2),
                    cursor.getInt(3), cursor.getInt(4))
            }
        }
        return result
    }

    @Synchronized
    fun getFolders(): List<VocabularyFolder> {
        val result = mutableListOf<VocabularyFolder>()
        readableDatabase.query("folders", arrayOf("id", "name"), null, null, null, null, "name COLLATE NOCASE ASC").use { c ->
            while (c.moveToNext()) result += VocabularyFolder(c.getLong(0), c.getString(1))
        }
        return result
    }

    @Synchronized
    fun createFolder(name: String): Long? {
        val clean = name.trim()
        if (clean.isBlank()) return null
        return try {
            writableDatabase.insertOrThrow("folders", null, ContentValues().apply {
                put("name", clean); put("created_at", System.currentTimeMillis())
            }).takeIf { it > 0 }
        } catch (_: android.database.sqlite.SQLiteConstraintException) { null }
    }

    @Synchronized
    fun renameFolder(id: Long, name: String): Boolean {
        val clean = name.trim()
        if (clean.isBlank()) return false
        return try {
            writableDatabase.update("folders", ContentValues().apply { put("name", clean) }, "id = ?", arrayOf(id.toString())) > 0
        } catch (_: android.database.sqlite.SQLiteConstraintException) { false }
    }

    @Synchronized
    fun deleteFolder(id: Long) { writableDatabase.delete("folders", "id = ?", arrayOf(id.toString())) }

    @Synchronized
    fun createDeck(name: String, folderId: Long? = null): Long? {
        val clean = name.trim()
        if (clean.isBlank()) return null
        return try {
            writableDatabase.insertOrThrow("decks", null, ContentValues().apply {
                put("name", clean)
                if (folderId == null) putNull("folder_id") else put("folder_id", folderId)
                put("created_at", System.currentTimeMillis())
            }).takeIf { it > 0 }
        } catch (_: android.database.sqlite.SQLiteConstraintException) { null }
    }

    @Synchronized
    fun renameDeck(id: Long, name: String): Boolean {
        val clean = name.trim()
        if (clean.isBlank()) return false
        return try {
            writableDatabase.update("decks", ContentValues().apply { put("name", clean) }, "id = ?", arrayOf(id.toString())) > 0
        } catch (_: android.database.sqlite.SQLiteConstraintException) { false }
    }

    @Synchronized
    fun moveDeckToFolder(id: Long, folderId: Long?): Boolean {
        val values = ContentValues().apply { if (folderId == null) putNull("folder_id") else put("folder_id", folderId) }
        return writableDatabase.update("decks", values, "id = ?", arrayOf(id.toString())) > 0
    }

    @Synchronized
    fun deleteDeck(id: Long): Boolean {
        val db = writableDatabase
        val count = db.rawQuery("SELECT COUNT(*) FROM decks", null).use { if (it.moveToFirst()) it.getInt(0) else 0 }
        if (count <= 1) return false
        return db.delete("decks", "id = ?", arrayOf(id.toString())) > 0
    }

    @Synchronized
    fun assignWordToDeck(wordId: Long, deckId: Long): Boolean =
        writableDatabase.update("vocabulary", ContentValues().apply { put("deck_id", deckId) },
            "id = ?", arrayOf(wordId.toString())) > 0

    fun deckName(deckId: Long?): String {
        if (deckId == null) return "Chưa phân bộ"
        return readableDatabase.rawQuery("SELECT name FROM decks WHERE id = ?", arrayOf(deckId.toString())).use {
            if (it.moveToFirst()) it.getString(0) else "Bộ từ đã xoá"
        }
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
        ).use { cursor ->
            while (cursor.moveToNext()) cursor.getString(0)?.let { reviewedDays += it }
        }
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendar = Calendar.getInstance()
        // If today has no reviews yet, a streak may still continue from yesterday.
        if (format.format(calendar.time) !in reviewedDays) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        if (format.format(calendar.time) !in reviewedDays) return 0

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
        root.put("schemaVersion", 2)
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
        val folders = JSONArray()
        readableDatabase.rawQuery("SELECT id, name, created_at FROM folders ORDER BY id", null).use { c ->
            while (c.moveToNext()) folders.put(JSONObject().put("id", c.getLong(0)).put("name", c.getString(1)).put("created_at", c.getLong(2)))
        }
        val decks = JSONArray()
        readableDatabase.rawQuery("SELECT id, name, folder_id, created_at FROM decks ORDER BY id", null).use { c ->
            while (c.moveToNext()) {
                val d = JSONObject().put("id", c.getLong(0)).put("name", c.getString(1)).put("created_at", c.getLong(3))
                if (c.isNull(2)) d.put("folder_id", JSONObject.NULL) else d.put("folder_id", c.getLong(2))
                decks.put(d)
            }
        }
        root.put("folders", folders)
        root.put("decks", decks)
        root.put("vocabulary", vocabulary)
        root.put("review_log", reviews)
        return root.toString(2)
    }

    @Synchronized
    fun restoreBackupJson(json: String): Pair<Int, Int> {
        val root = JSONObject(json)
        require(root.optString("format") == "luyentu-backup") { "Tệp không phải bản sao lưu LuyenTu." }
        val backupVersion = root.optInt("schemaVersion", 1)
        require(backupVersion in 1..2) { "Phiên bản bản sao lưu không được hỗ trợ." }
        val vocabulary = root.getJSONArray("vocabulary")
        val reviews = root.getJSONArray("review_log")
        val folders = root.optJSONArray("folders") ?: JSONArray()
        val decks = root.optJSONArray("decks") ?: JSONArray()
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("review_log", null, null)
            db.delete("vocabulary", null, null)
            db.delete("decks", null, null)
            db.delete("folders", null, null)
            db.execSQL("DELETE FROM sqlite_sequence WHERE name IN ('vocabulary', 'review_log', 'decks', 'folders')")
            for (i in 0 until folders.length()) {
                val f = folders.getJSONObject(i)
                db.insertOrThrow("folders", null, ContentValues().apply {
                    put("id", f.getLong("id")); put("name", f.getString("name"))
                    put("created_at", f.optLong("created_at", System.currentTimeMillis()))
                })
            }
            for (i in 0 until decks.length()) {
                val d = decks.getJSONObject(i)
                val values = ContentValues().apply {
                    put("id", d.getLong("id")); put("name", d.getString("name"))
                    if (d.isNull("folder_id")) putNull("folder_id") else put("folder_id", d.getLong("folder_id"))
                    put("created_at", d.optLong("created_at", System.currentTimeMillis()))
                }
                db.insertOrThrow("decks", null, values)
            }
            val defaultDeck = ensureDefaultDeck(db)
            val existingDeckIds = mutableSetOf<Long>()
            db.rawQuery("SELECT id FROM decks", null).use { c -> while (c.moveToNext()) existingDeckIds += c.getLong(0) }
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
                    val requestedDeck = if (item.isNull("deck_id")) defaultDeck else item.optLong("deck_id", defaultDeck)
                    put("deck_id", if (requestedDeck in existingDeckIds) requestedDeck else defaultDeck)
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
        private const val DATABASE_VERSION = 4
        private const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
