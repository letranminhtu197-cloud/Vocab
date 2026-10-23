package com.vocab.ielts.offline

/** Validates exercises using only vocabulary data supplied by the learner. */
enum class ActiveExerciseType { MULTIPLE_CHOICE, LISTEN_AND_TYPE, CLOZE }

data class ExerciseEvidence(
    val type: ActiveExerciseType,
    val valid: Boolean,
    val attempts: Int = 0,
    val errors: Int = 0,
    val lastUsedAtMillis: Long = 0L
)

data class MatchPair(val wordId: Long, val term: String, val meaning: String)

object ActiveLearningLogic {
    private val targetShare = mapOf(
        ActiveExerciseType.MULTIPLE_CHOICE to 25,
        ActiveExerciseType.LISTEN_AND_TYPE to 35,
        ActiveExerciseType.CLOZE to 40
    )

    /** Picks among valid exercises; invalid/missing source data is never replaced with invented data. */
    fun chooseNext(
        evidence: List<ExerciseEvidence>,
        nowMillis: Long,
        sessionCounts: Map<ActiveExerciseType, Int> = emptyMap()
    ): ActiveExerciseType? {
        val candidates = evidence.filter { it.valid && it.type in targetShare.keys }
        if (candidates.isEmpty()) return null
        val total = sessionCounts.values.sum().coerceAtLeast(0)
        return candidates.maxWithOrNull(
            compareBy<ExerciseEvidence> { item ->
                val observed = if (total == 0) 0 else (sessionCounts[item.type] ?: 0) * 100 / total
                val mixNeed = (targetShare[item.type] ?: 0) - observed
                val errorRate = if (item.attempts < 2) 0
                    else (item.errors.coerceIn(0, item.attempts) * 100) / item.attempts
                val elapsedMinutes = if (item.lastUsedAtMillis <= 0L) 60
                    else ((nowMillis - item.lastUsedAtMillis).coerceAtLeast(0L) / 60_000L).coerceAtMost(60L).toInt()
                mixNeed + errorRate + elapsedMinutes
            }.thenBy { it.type.ordinal }
        )?.type
    }

    fun normalize(value: String): String =
        value.trim().lowercase().replace(Regex("\\s+"), " ")

    /** Requires four non-empty, visibly distinct labels and exactly one target label. */
    fun hasFourValidChoices(labels: List<String>, targetLabel: String): Boolean {
        val target = normalize(targetLabel)
        val normalized = labels.map(::normalize)
        return target.isNotBlank() && normalized.size == 4 &&
            normalized.all { it.isNotBlank() } && normalized.distinct().size == 4 &&
            normalized.count { it == target } == 1
    }

    /** Accept a cloze only when the target occurs exactly once as a whole word/phrase. */
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

    /** Up to six pairs with distinct terms and meanings; incomplete entries are excluded. */
    fun matchPairs(words: List<WordEntity>, limit: Int = 6): List<MatchPair> {
        if (limit < 2) return emptyList()
        val result = mutableListOf<MatchPair>()
        val seenTerms = mutableSetOf<String>()
        val seenMeanings = mutableSetOf<String>()
        for (word in words) {
            val term = word.term.trim()
            val meaning = word.meaning.trim()
            val termKey = normalize(term)
            val meaningKey = normalize(meaning)
            if (termKey.isBlank() || meaningKey.isBlank()) continue
            if (termKey in seenTerms || meaningKey in seenMeanings) continue
            seenTerms += termKey
            seenMeanings += meaningKey
            result += MatchPair(word.id, term, meaning)
            if (result.size == limit) break
        }
        return result.takeIf { it.size >= 2 } ?: emptyList()
    }
}
