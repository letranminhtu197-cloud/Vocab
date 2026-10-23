package com.vocab.ielts.offline

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "learning_sessions", indices = [Index("startedAtMillis")])
data class LearningSessionEntity(
    @PrimaryKey val sessionId: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long = 0L,
    val questionCount: Int = 0,
    val uniqueWordCount: Int = 0,
    val isSrsReview: Boolean,
    val completed: Boolean = false
)
