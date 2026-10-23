package com.vocab.ielts.offline

/** Cách sắp xếp sổ từ; thuần Kotlin để kiểm thử không cần Android. */
enum class LibrarySort(val label: String) {
    AZ("A → Z"),
    NEWEST("Mới thêm"),
    DUE_SOON("Đến hạn trước")
}

object LibrarySorting {
    fun sort(words: List<WordEntity>, mode: LibrarySort): List<WordEntity> = when (mode) {
        LibrarySort.AZ -> words.sortedWith(
            compareBy<WordEntity> { it.term.lowercase() }.thenBy { it.id }
        )

        LibrarySort.NEWEST -> words.sortedWith(
            compareByDescending<WordEntity> { it.createdAtMillis }.thenBy { it.term.lowercase() }
        )

        LibrarySort.DUE_SOON -> words.sortedWith(
            // nextReviewAtMillis = 0 nghĩa là chưa có lịch và luôn đến hạn trước.
            compareBy<WordEntity> { if (it.nextReviewAtMillis <= 0L) Long.MIN_VALUE else it.nextReviewAtMillis }
                .thenBy { it.term.lowercase() }
        )
    }
}
