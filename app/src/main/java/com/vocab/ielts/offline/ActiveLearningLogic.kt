package com.vocab.ielts.offline

/**
 * Pure, deterministic rules for the active-learning layer. It never invents vocabulary data
 * and never changes SRS state; callers decide whether a confirmed attempt belongs to SRS.
 */
enum class ActiveExerciseType { MULTIPLE_CHOICE, LISTEN_AND_TYPE, CLOZE, FLASHCARD, MATCH_GAME }

data class ExerciseEvidence(
    val type: ActiveExerciseType,
    val valid: Boolean,
    val recentAttempts: Int = 0,
    val recentErrors: Int = 0,
    val lastUsedAtMillis: Long = 0L
)

data class MatchPair(val wordId: Long, val term: String, val meaning: String)

object ActiveLearningLogic {
    private val preferredShares = mapOf(
        ActiveExerciseType.MULTIPLE_CHOICE to 20,
        ActiveExerciseType.LISTEN_AND_TYPE to 30,
        ActiveExerciseType.CLOZE to 50
    )

    /**
     * Chooses the next valid exercise type using target mix, recent errors and recency.
     * Missing prerequisites exclude an exercise instead of producing a malformed question.
     */
    fun chooseNext(
        evidence: List<ExerciseEvidence>,
        nowMillis: Long,
        sessionCounts: Map<ActiveExerciseType, Int> = emptyMap()
    ): ActiveExerciseType? {
        val candidates = evidence.filter { it.valid && it.type in preferredShares.keys }
        if (candidates.isEmpty()) return null
        val total = sessionCounts.values.sum().coerceAtLeast(0)
        return candidates.maxWithOrNull(
            compareBy<ExerciseEvidence> { item ->
                val target = preferredShares[item.type] ?: 0
                val observed = if (total == 0) 0 else (sessionCounts[item.type] ?: 0) * 100 / total
                val mixNeed = target - observed
                val errorRate = if (item.recentAttempts < 3) 0
                    else item.recentErrors * 100 / item.recentAttempts.coerceAtLeast(1)
                val recencyPenalty = if (item.lastUsedAtMillis <= 0L) 0
                    else ((nowMillis - item.lastUsedAtMillis).coerceAtLeast(0L) / 60_000L)
                        .coerceAtMost(60L).toInt()
                mixNeed + errorRate + recencyPenalty
            }.thenBy { it.type.ordinal }
        )?.type
    }

    /** Four-choice question validity: exactly one target and three distinct, non-empty distractors. */
    fun hasFourValidChoices(labels: List<String>, targetLabel: String): Boolean {
        val normalize: (String) -> String = { it.trim().lowercase().replace(Regex("\\s+"), " ") }
        val target = normalize(targetLabel)
        val normalized = labels.map(normalize).filter { it.isNotBlank() }
        return target.isNotBlank() && normalized.size == 4 &&
            normalized.distinct().size == 4 && normalized.count { it == target } == 1
    }

    /** Cloze only passes when the target occurs exactly once as a complete word/phrase. */
    fun validatedCloze(example: String, term: String): String? {
        if (example.isBlank() || term.isBlank()) return null
        val pattern = Regex(
            "(?<![\\p{L}\\p{N}])" + Regex.escape(term.trim()) + "(?![\\p{L}\\p{N}])",
            RegexOption.IGNORE_CASE
        )
        val matches = pattern.findAll(example).toList()
        if (matches.size != 1) return null
        return pattern.replace(example) { "_____" }
    }

    /** Creates only distinct pairs with both fields present; never fabricates missing meanings. */
    fun matchPairs(words: List<WordEntity>, limit: Int = 6): List<MatchPair> {
        if (limit < 2) return emptyList()
        return words.asSequence()
            .filter { it.term.isNotBlank() && it.meaning.isNotBlank() }
            .distinctBy { it.term.trim().lowercase() }
            .distinctBy { it.meaning.trim().lowercase() }
            .take(limit)
            .map { MatchPair(it.id, it.term.trim(), it.meaning.trim()) }
            .toList()
            .takeIf { it.size >= 2 } ?: emptyList()
    }
}
