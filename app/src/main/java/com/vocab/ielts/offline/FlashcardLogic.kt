package com.vocab.ielts.offline

/** Keeps the due-only flashcard queue aligned when rating removes the current word from it. */
object FlashcardLogic {
    fun nextIndexAfterRating(currentIndex: Int, deckSize: Int, dueOnly: Boolean): Int {
        if (dueOnly || deckSize <= 1) return 0
        return (currentIndex + 1) % deckSize
    }
}
