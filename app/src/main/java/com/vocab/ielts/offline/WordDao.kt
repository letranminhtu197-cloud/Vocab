package com.vocab.ielts.offline

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words ORDER BY term COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE nextReviewAtMillis = 0 OR nextReviewAtMillis <= :now ORDER BY nextReviewAtMillis ASC, term COLLATE NOCASE ASC")
    fun observeDue(now: Long): Flow<List<WordEntity>>

    @Query("SELECT COUNT(*) FROM words")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(word: WordEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(words: List<WordEntity>)

    @Update
    suspend fun update(word: WordEntity)

    @Delete
    suspend fun delete(word: WordEntity)

    @Query("SELECT * FROM words WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): WordEntity?

    /** Returns -1 when this stable attempt ID has already been recorded. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAttempt(attempt: LearningAttemptEntity): Long

    @Query("SELECT COUNT(*) FROM learning_attempts")
    suspend fun countLearningAttempts(): Int

    @Query("SELECT COUNT(*) FROM learning_attempts WHERE isSrsReview = 1")
    suspend fun countSrsAttempts(): Int

    @Query("SELECT COUNT(*) FROM learning_attempts WHERE exerciseType = :type")
    suspend fun countAttemptsForType(type: String): Int

    @Query("SELECT COUNT(*) FROM learning_attempts WHERE exerciseType = :type AND isCorrect = 1")
    suspend fun countCorrectForType(type: String): Int
}
