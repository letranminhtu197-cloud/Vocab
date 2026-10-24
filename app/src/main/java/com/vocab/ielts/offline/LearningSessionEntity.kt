package com.vocab.ielts.offline

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "learning_sessions", indices = [Index("startedAtMillis")])
data class LearningSessionEntity(
    @PrimaryKey val sessionId: String,
    val startedAtMillis: Long,
    @ColumnInfo(defaultValue = "0") val endedAtMillis: Long = 0L,
    @ColumnInfo(defaultValue = "0") val questionCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val uniqueWordCount: Int = 0,
    val isSrsReview: Boolean,
    @ColumnInfo(defaultValue = "0") val completed: Boolean = false
)
