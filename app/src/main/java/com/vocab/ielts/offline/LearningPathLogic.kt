package com.vocab.ielts.offline

data class TopicProgress(
    val topic: String,
    val total: Int,
    val learned: Int,
    val due: Int,
    val weak: Int
) {
    val percent: Int get() = if (total == 0) 0 else learned * 100 / total
}

object LearningPathLogic {
    fun topics(words: List<WordEntity>, nowMillis: Long): List<TopicProgress> =
        words.groupBy { it.topic.trim().ifEmpty { "Chưa phân loại" } }
            .map { (topic, group) ->
                TopicProgress(
                    topic = topic,
                    total = group.size,
                    learned = group.count { it.reviewCount > 0 },
                    due = group.count { it.nextReviewAtMillis == 0L || it.nextReviewAtMillis <= nowMillis },
                    weak = group.count { it.lapseCount > 0 }
                )
            }
            .sortedWith(compareBy<TopicProgress> { it.topic == "Chưa phân loại" }.thenBy { it.topic.lowercase() })

    fun overall(words: List<WordEntity>, nowMillis: Long): TopicProgress {
        val topics = topics(words, nowMillis)
        return TopicProgress(
            topic = "Tất cả từ",
            total = topics.sumOf { it.total },
            learned = topics.sumOf { it.learned },
            due = topics.sumOf { it.due },
            weak = topics.sumOf { it.weak }
        )
    }
}
