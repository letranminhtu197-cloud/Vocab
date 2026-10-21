package com.vocab.ielts.offline

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [WordEntity::class], version = 2, exportSchema = false)
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

        @Volatile private var INSTANCE: VocabDatabase? = null

        fun getInstance(context: Context): VocabDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VocabDatabase::class.java,
                    "vocab-local.db"
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
    }
}
