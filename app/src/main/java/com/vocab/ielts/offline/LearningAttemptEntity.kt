package com.vocab.ielts.offline

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Durable idempotency ledger for every explicitly submitted vocabulary answer.
 * Game results are tracked separately and are never inserted here.
 */
@Entity(tableName = "learning_attempts", indices = [Index("wordId"), Index("answeredAtMillis")])
data class LearningAttemptEntity(
    @PrimaryKey val attemptId: String,
    val wordId: Long,
    val exerciseType: String,
    val isCorrect: Boolean,
    val isSrsReview: Boolean,
    val answeredAtMillis: Long
)
