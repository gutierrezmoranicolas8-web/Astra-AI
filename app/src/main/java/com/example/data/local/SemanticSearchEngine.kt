package com.example.data.local

import com.example.data.model.AiModel
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

data class SearchResultItem(
    val conversationId: Long,
    val conversationTitle: String,
    val conversationPrompt: String,
    val responseId: Long,
    val model: AiModel,
    val modelVersion: String,
    val fullResponse: String,
    val snippet: String,
    val relevanceScore: Int, // 0 - 100%
    val detectedIntent: String,
    val matchedKeywords: List<String>,
    val timestamp: Long,
    val isStarred: Boolean,
    val isWinner: Boolean
)

enum class QueryIntent(val displayName: String) {
    ALL("Todo"),
    CODE_DEV("Código y Desarrollo"),
    EXPLANATION("Explicaciones y Teoría"),
    CREATIVE("Creatividad y Redacción"),
    BUSINESS("Negocios y Productividad"),
    SCIENCE_MATH("Ciencia y Matemáticas"),
    GENERAL("General")
}

object SemanticSearchEngine {

    // Semantic conceptual clusters
    private val CONCEPT_CLUSTERS: Map<String, Set<String>> = mapOf(
        "programming" to setOf(
            "codigo", "code", "script", "programar", "desarrollo", "dev", "bug", "error", "fallo",
            "exception", "crash", "syntax", "compilar", "funcion", "clase", "metodo", "variable",
            "python", "kotlin", "java", "javascript", "typescript", "c++", "rust", "sql", "api",
            "endpoint", "backend", "frontend", "git", "github", "refactor", "algoritmo", "room",
            "database", "json", "rest", "nullpointer", "stack", "debug"
        ),
        "business" to setOf(
            "negocio", "empresa", "startup", "ventas", "marketing", "inversion", "dinero", "finanzas",
            "budget", "presupuesto", "roi", "kpi", "estrategia", "ejecutivo", "copilot", "informe",
            "reunion", "resumen", "cliente", "producto", "mercado", "ganancia", "ingresos", "costo"
        ),
        "creative" to setOf(
            "historia", "cuento", "novela", "poesia", "verso", "guion", "narrativa", "personaje",
            "trama", "ficcion", "metafora", "escritura", "literatura", "dialogo", "creativo", "claude",
            "fantasia", "prosa", "estilo", "redaccion"
        ),
        "science_math" to setOf(
            "fisica", "quimica", "biologia", "matematica", "calculo", "algebra", "ecuacion", "formula",
            "teoria", "hipotesis", "experimento", "cosmos", "universo", "cuantico", "estadistica",
            "probabilidad", "logica", "ciencia", "astronomia", "genetica"
        ),
        "wit_humor" to setOf(
            "humor", "chiste", "broma", "sarcasmo", "ironia", "gracioso", "divertido", "meme",
            "grok", "audaz", "rebelde", "parodia", "comedia", "picante"
        ),
        "synthesis" to setOf(
            "resumen", "sintesis", "conclusion", "puntos", "bullet", "clave", "esquema", "abstract",
            "resumir", "destacado", "general", "lecciones", "balance"
        )
    )

    fun detectIntent(query: String): QueryIntent {
        val tokens = tokenize(query)
        val codeScore = tokens.count { it in (CONCEPT_CLUSTERS["programming"] ?: emptySet()) }
        val bizScore = tokens.count { it in (CONCEPT_CLUSTERS["business"] ?: emptySet()) }
        val creativeScore = tokens.count { it in (CONCEPT_CLUSTERS["creative"] ?: emptySet()) }
        val sciScore = tokens.count { it in (CONCEPT_CLUSTERS["science_math"] ?: emptySet()) }

        val max = maxOf(codeScore, bizScore, creativeScore, sciScore)
        if (max == 0) return QueryIntent.GENERAL

        return when (max) {
            codeScore -> QueryIntent.CODE_DEV
            bizScore -> QueryIntent.BUSINESS
            creativeScore -> QueryIntent.CREATIVE
            sciScore -> QueryIntent.SCIENCE_MATH
            else -> QueryIntent.GENERAL
        }
    }

    /**
     * Executes advanced semantic search across all conversations and multi-model responses.
     */
    fun search(
        query: String,
        conversations: List<ConversationWithResponses>,
        selectedModelIdFilter: String? = null,
        intentFilter: QueryIntent = QueryIntent.ALL,
        minRelevanceThreshold: Int = 15
    ): List<SearchResultItem> {
        if (query.isBlank()) return emptyList()

        val normalizedQuery = query.lowercase(Locale.ROOT)
        val queryTokens = tokenize(normalizedQuery)
        if (queryTokens.isEmpty()) return emptyList()

        val queryConceptScores = calculateConceptWeights(queryTokens)
        val queryIntent = detectIntent(normalizedQuery)

        val results = mutableListOf<SearchResultItem>()

        for (item in conversations) {
            val conv = item.conversation
            val promptTokens = tokenize(conv.prompt.lowercase(Locale.ROOT))
            val titleTokens = tokenize(conv.title.lowercase(Locale.ROOT))

            for (resp in item.responses) {
                // Filter by model if requested
                if (selectedModelIdFilter != null && !resp.modelId.equals(selectedModelIdFilter, ignoreCase = true)) {
                    continue
                }

                val responseLower = resp.responseText.lowercase(Locale.ROOT)
                val respTokens = tokenize(responseLower)
                val respConceptScores = calculateConceptWeights(respTokens)

                // 1. Direct Keyword Match score (weight 35%)
                val matchedWords = mutableSetOf<String>()
                var directMatches = 0
                for (qToken in queryTokens) {
                    if (responseLower.contains(qToken)) {
                        directMatches++
                        matchedWords.add(qToken)
                    } else if (promptTokens.contains(qToken) || titleTokens.contains(qToken)) {
                        directMatches++
                        matchedWords.add(qToken)
                    } else {
                        // Stemming prefix match (e.g. "program" matches "programacion", "programador")
                        val prefixMatch = respTokens.any { it.startsWith(qToken) || qToken.startsWith(it) && min(it.length, qToken.length) >= 4 }
                        if (prefixMatch) {
                            directMatches++
                            matchedWords.add(qToken)
                        }
                    }
                }
                val directMatchScore = (directMatches.toDouble() / queryTokens.size.coerceAtLeast(1)) * 35.0

                // 2. Semantic Conceptual Similarity score (weight 45%)
                // Dot product of semantic concept weights
                var conceptualDotProduct = 0.0
                var sharedConceptsCount = 0
                for ((concept, qWeight) in queryConceptScores) {
                    val respWeight = respConceptScores[concept] ?: 0.0
                    if (qWeight > 0 && respWeight > 0) {
                        conceptualDotProduct += (qWeight * respWeight)
                        sharedConceptsCount++
                        matchedWords.add(concept)
                    }
                }
                val semanticScore = (conceptualDotProduct * 45.0).coerceIn(0.0, 45.0)

                // 3. Intent alignment bonus (up to 15%)
                var intentScore = 0.0
                if (queryIntent == QueryIntent.CODE_DEV && (resp.responseText.contains("```") || respTokens.any { it in (CONCEPT_CLUSTERS["programming"] ?: emptySet()) })) {
                    intentScore = 15.0
                } else if (queryIntent == QueryIntent.CREATIVE && respTokens.any { it in (CONCEPT_CLUSTERS["creative"] ?: emptySet()) }) {
                    intentScore = 15.0
                } else if (queryIntent == QueryIntent.BUSINESS && respTokens.any { it in (CONCEPT_CLUSTERS["business"] ?: emptySet()) }) {
                    intentScore = 15.0
                } else if (queryIntent == QueryIntent.SCIENCE_MATH && respTokens.any { it in (CONCEPT_CLUSTERS["science_math"] ?: emptySet()) }) {
                    intentScore = 15.0
                } else {
                    intentScore = 5.0
                }

                // 4. Quality boost (Starred / Winner) (up to 5%)
                var qualityBonus = 0.0
                if (resp.isWinner) qualityBonus += 3.0
                if (resp.isStarred) qualityBonus += 2.0

                val totalScore = (directMatchScore + semanticScore + intentScore + qualityBonus).roundToInt().coerceIn(0, 100)

                // Check intent filter
                if (intentFilter != QueryIntent.ALL && queryIntent != intentFilter) {
                    // Skip if specific intent was explicitly selected and didn't match
                    if (intentScore < 10.0) continue
                }

                if (totalScore >= minRelevanceThreshold) {
                    val snippet = extractBestSnippet(resp.responseText, matchedWords)
                    val model = AiModel.fromId(resp.modelId)

                    results.add(
                        SearchResultItem(
                            conversationId = conv.id,
                            conversationTitle = conv.title,
                            conversationPrompt = conv.prompt,
                            responseId = resp.id,
                            model = model,
                            modelVersion = resp.modelVersion,
                            fullResponse = resp.responseText,
                            snippet = snippet,
                            relevanceScore = totalScore,
                            detectedIntent = queryIntent.displayName,
                            matchedKeywords = matchedWords.toList(),
                            timestamp = resp.createdAt,
                            isStarred = resp.isStarred,
                            isWinner = resp.isWinner
                        )
                    )
                }
            }
        }

        // Sort descending by relevance score, then timestamp
        return results.sortedWith(
            compareByDescending<SearchResultItem> { it.relevanceScore }
                .thenByDescending { it.isWinner }
                .thenByDescending { it.timestamp }
        )
    }

    private fun extractBestSnippet(text: String, keywords: Set<String>): String {
        val clean = text.replace(Regex("\\s+"), " ").trim()
        if (clean.length <= 160) return clean

        // Find the index of the first keyword match
        var bestIndex = -1
        val lower = clean.lowercase(Locale.ROOT)
        for (kw in keywords) {
            val idx = lower.indexOf(kw.lowercase(Locale.ROOT))
            if (idx != -1 && (bestIndex == -1 || idx < bestIndex)) {
                bestIndex = idx
            }
        }

        if (bestIndex == -1) {
            return clean.take(160) + "..."
        }

        val start = (bestIndex - 40).coerceAtLeast(0)
        val end = (start + 160).coerceAtMost(clean.length)
        val prefix = if (start > 0) "..." else ""
        val suffix = if (end < clean.length) "..." else ""

        return prefix + clean.substring(start, end).trim() + suffix
    }

    private fun calculateConceptWeights(tokens: List<String>): Map<String, Double> {
        val weights = mutableMapOf<String, Double>()
        for ((concept, cluster) in CONCEPT_CLUSTERS) {
            val count = tokens.count { it in cluster }
            if (count > 0) {
                weights[concept] = (count.toDouble() / tokens.size.coerceAtLeast(1) * 3.0).coerceAtMost(1.0)
            }
        }
        return weights
    }

    private fun tokenize(text: String): List<String> {
        val stopWords = setOf(
            "de", "la", "el", "los", "las", "un", "una", "unos", "unas", "y", "o", "pero",
            "en", "con", "por", "para", "a", "del", "al", "que", "es", "son", "como", "su",
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "with", "is"
        )
        return text.lowercase(Locale.ROOT)
            .split(Regex("[^\\p{L}\\p{Nd}]+"))
            .filter { it.length > 2 && it !in stopWords }
    }
}
