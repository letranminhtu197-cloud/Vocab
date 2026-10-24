package com.vocab.ielts.offline

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [WordEntity::class, LearningAttemptEntity::class, LearningSessionEntity::class], version = 6, exportSchema = false)
abstract class VocabDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE words ADD COLUMN collocations TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE words ADD COLUMN wordFamily TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE words ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE words ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE words ADD COLUMN synonyms TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE words ADD COLUMN excludedFromReview INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE words ADD COLUMN is_new INTEGER NOT NULL DEFAULT 1")
                database.execSQL("UPDATE words SET is_new = CASE WHEN reviewCount > 0 OR srsLevel > 0 THEN 0 ELSE 1 END")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS learning_attempts (" +
                        "attemptId TEXT NOT NULL PRIMARY KEY, " +
                        "wordId INTEGER NOT NULL, " +
                        "sessionId TEXT NOT NULL, " +
                        "exerciseType TEXT NOT NULL, " +
                        "isCorrect INTEGER NOT NULL, " +
                        "isSrsReview INTEGER NOT NULL, " +
                        "answeredAtMillis INTEGER NOT NULL)"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_learning_attempts_wordId ON learning_attempts(wordId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_learning_attempts_answeredAtMillis ON learning_attempts(answeredAtMillis)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_learning_attempts_sessionId ON learning_attempts(sessionId)")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS learning_sessions (" +
                        "sessionId TEXT NOT NULL PRIMARY KEY, " +
                        "startedAtMillis INTEGER NOT NULL, " +
                        "endedAtMillis INTEGER NOT NULL DEFAULT 0, " +
                        "questionCount INTEGER NOT NULL DEFAULT 0, " +
                        "uniqueWordCount INTEGER NOT NULL DEFAULT 0, " +
                        "isSrsReview INTEGER NOT NULL, " +
                        "completed INTEGER NOT NULL DEFAULT 0)"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_learning_sessions_startedAtMillis ON learning_sessions(startedAtMillis)")
            }
        }

        @Volatile private var INSTANCE: VocabDatabase? = null

        fun getInstance(context: Context): VocabDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VocabDatabase::class.java,
                    "vocab-local.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build().also { INSTANCE = it }
            }
    }
}
