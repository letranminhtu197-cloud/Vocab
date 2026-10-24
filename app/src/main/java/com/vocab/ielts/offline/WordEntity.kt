package com.vocab.ielts.offline

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val term: String,
    val ipa: String = "",
    val partOfSpeech: String = "",
    val meaning: String,
    val example: String = "",
    val topic: String = "",
    @ColumnInfo(defaultValue = "''") val collocations: String = "",
    @ColumnInfo(defaultValue = "''") val synonyms: String = "",
    @ColumnInfo(defaultValue = "''") val wordFamily: String = "",
    @ColumnInfo(defaultValue = "''") val notes: String = "",
    @ColumnInfo(defaultValue = "''") val tags: String = "",
    val srsLevel: Int = 0,
    val nextReviewAtMillis: Long = 0L,
    val reviewCount: Int = 0,
    val lapseCount: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val lastReviewedAtMillis: Long = 0L,
    @ColumnInfo(defaultValue = "0") val excludedFromReview: Boolean = false,
    @ColumnInfo(name = "is_new", defaultValue = "1") val isNew: Boolean = reviewCount == 0 && srsLevel == 0
)
