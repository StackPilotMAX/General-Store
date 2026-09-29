package com.stackpilotmax.rameshvegetableshop

import java.text.Normalizer
import java.util.Locale
import kotlin.math.max

internal data class VegetableSearchResult(
    val vegetable: VegetableOption,
    val score: Double,
    val matchedTerm: String
)

internal object VegetableSearchEngine {
    private val aliases = linkedMapOf(
        "Methi" to listOf("methi", "meti", "मेथी"),
        "Palak" to listOf("palak", "paalak", "पालक", "spinach"),
        "Soya" to listOf("soya", "soya bhaji", "shepu", "shepu bhaji", "शेपू", "dill"),
        "Hara Kanda" to listOf("hara kanda", "hara pyaz", "hirva kanda", "green onion", "spring onion"),
        "China Kothmir" to listOf("china kothmir", "china kothimbir", "china dhaniya", "china coriander"),
        "Pudina" to listOf("pudina", "podina", "mint"),
        "Chaulai" to listOf("chaulai", "cholai", "lal math", "math"),
        "Mooli ke Patte" to listOf("mooli ke patte", "muli ke patte", "radish leaves"),
        "Desi Kothmir" to listOf("desi kothmir", "desi kothimbir", "dhaniya", "kothimbir", "coriander"),
        "Mooli" to listOf("mooli", "muli", "mula", "radish"),
        "Hari Mirch" to listOf("hari mirch", "hari mirchi", "hirvi mirchi", "green chilli", "green chili"),
        "Naya Adrak" to listOf("naya adrak", "naya ginger", "fresh ginger", "navin adrak"),
        "Purana Adrak" to listOf("purana adrak", "old ginger", "juna adrak"),
        "Kadi Patta" to listOf("kadi patta", "kadhi patta", "curry patta", "curry leaves"),
        "Nimbu" to listOf("nimbu", "limbu", "lemon"),
        "Gobhi" to listOf("gobhi", "gobi", "phool gobhi", "cauliflower"),
        "Shimla Mirch" to listOf("shimla mirch", "shimla mirchi", "dhobli mirchi", "capsicum", "bell pepper"),
        "Chota Lahsun" to listOf("chota lahsun", "chota lasun", "small garlic"),
        "Bada Lahsun" to listOf("bada lahsun", "bada lasun", "big garlic"),
        "Kakdi" to listOf("kakdi", "kakadi", "kheera", "khira", "cucumber")
    )

    fun search(
        query: String,
        vegetables: List<VegetableOption>,
        limit: Int = 5
    ): List<VegetableSearchResult> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isBlank()) return emptyList()

        return vegetables.map { vegetable ->
            val terms = buildList {
                add(vegetable.name)
                addAll(aliases[vegetable.name].orEmpty())
            }.distinct()

            val best = terms
                .map { term -> term to score(normalizedQuery, normalize(term)) }
                .maxByOrNull { it.second }
                ?: (vegetable.name to 0.0)

            VegetableSearchResult(
                vegetable = vegetable,
                score = best.second,
                matchedTerm = best.first
            )
        }
            .sortedWith(
                compareByDescending<VegetableSearchResult> { it.score }
                    .thenBy { it.vegetable.name }
            )
            .take(limit.coerceAtLeast(1))
    }

    private fun score(query: String, candidate: String): Double {
        if (candidate.isBlank()) return 0.0
        if (query == candidate) return 1.0

        val queryCompact = query.replace(" ", "")
        val candidateCompact = candidate.replace(" ", "")
        if (queryCompact == candidateCompact) return 0.99

        val containment = when {
            candidate.startsWith(query) -> 0.94
            query.startsWith(candidate) -> 0.92
            candidate.contains(query) -> 0.90
            query.contains(candidate) -> 0.88
            candidateCompact.contains(queryCompact) -> 0.87
            queryCompact.contains(candidateCompact) -> 0.85
            else -> 0.0
        }

        val distance = levenshtein(queryCompact, candidateCompact)
        val denominator = max(queryCompact.length, candidateCompact.length).coerceAtLeast(1)
        val similarity = 1.0 - distance.toDouble() / denominator.toDouble()

        val queryTokens = query.split(' ').filter { it.isNotBlank() }
        val candidateTokens = candidate.split(' ').filter { it.isNotBlank() }
        val tokenSimilarity = queryTokens.maxOfOrNull { q ->
            candidateTokens.maxOfOrNull { c ->
                val tokenDistance = levenshtein(q, c)
                1.0 - tokenDistance.toDouble() / max(q.length, c.length).coerceAtLeast(1)
            } ?: 0.0
        } ?: 0.0

        return maxOf(containment, similarity * 0.92, tokenSimilarity * 0.84)
            .coerceIn(0.0, 1.0)
    }

    private fun normalize(value: String): String {
        val normalized = Normalizer.normalize(
            value.lowercase(Locale.ROOT),
            Normalizer.Form.NFKC
        )
        return buildString(normalized.length) {
            normalized.forEach { character ->
                append(if (character.isLetterOrDigit()) character else ' ')
            }
        }.trim().replace(Regex("\\s+"), " ")
    }

    private fun levenshtein(left: String, right: String): Int {
        if (left == right) return 0
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length

        var previous = IntArray(right.length + 1) { it }
        var current = IntArray(right.length + 1)
        for (leftIndex in left.indices) {
            current[0] = leftIndex + 1
            for (rightIndex in right.indices) {
                val substitution = if (left[leftIndex] == right[rightIndex]) 0 else 1
                current[rightIndex + 1] = minOf(
                    current[rightIndex] + 1,
                    previous[rightIndex + 1] + 1,
                    previous[rightIndex] + substitution
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[right.length]
    }
}
