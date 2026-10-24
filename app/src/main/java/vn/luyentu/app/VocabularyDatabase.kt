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
    val topic: String
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
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Schema migrations will be added here before the database version changes.
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
            arrayOf("id", "term", "meaning", "example", "ipa", "topic"),
            selection,
            args,
            null,
            null,
            "term COLLATE NOCASE ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += VocabularyWord(
                    id = cursor.getLong(0),
                    term = cursor.getString(1),
                    meaning = cursor.getString(2),
                    example = cursor.getString(3),
                    ipa = cursor.getString(4),
                    topic = cursor.getString(5)
                )
            }
        }
        return result
    }

    @Synchronized
    fun deleteWord(id: Long) {
        writableDatabase.delete("vocabulary", "id = ?", arrayOf(id.toString()))
    }

    fun wordCount(): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM vocabulary", null).use {
        if (it.moveToFirst()) it.getInt(0) else 0
    }

    companion object {
        private const val DATABASE_NAME = "luyentu_personal.db"
        private const val DATABASE_VERSION = 1
    }
}
