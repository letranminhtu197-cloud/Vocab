package com.vocab.ielts.offline

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentMatchers.startsWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class VocabDatabaseMigrationTest {
    @Test
    fun migrationFourToFiveCreatesCategoryTableAndUniqueNormalizedNameIndex() {
        val database = mock(SupportSQLiteDatabase::class.java)

        VocabDatabase.MIGRATION_4_5.migrate(database)

        verify(database).execSQL(startsWith("CREATE TABLE IF NOT EXISTS categories"))
        verify(database).execSQL(startsWith("CREATE UNIQUE INDEX IF NOT EXISTS index_categories_normalizedName"))
        assertEquals(4, VocabDatabase.MIGRATION_4_5.startVersion)
        assertEquals(5, VocabDatabase.MIGRATION_4_5.endVersion)
    }
}
