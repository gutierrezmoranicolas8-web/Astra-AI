package com.example.data.repository

import com.example.data.local.ConversationDao
import com.example.data.local.ConversationEntity
import com.example.data.local.ConversationWithResponses
import com.example.data.local.ModelResponseEntity
import com.example.data.model.AiModel
import com.example.data.model.ModelResult
import com.example.data.preferences.AstraSettings
import com.example.data.preferences.AstraSettingsRepository
import com.example.data.remote.AstraEngineManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class ConversationRepository(
    private val conversationDao: ConversationDao,
    private val settingsRepository: AstraSettingsRepository
) {
    val allConversations: Flow<List<ConversationWithResponses>> =
        conversationDao.getAllConversationsWithResponses()

    val settings: StateFlow<AstraSettings> = settingsRepository.settingsFlow

    fun getConversation(id: Long): Flow<ConversationWithResponses?> =
        conversationDao.getConversationWithResponses(id)

    suspend fun getAllConversationsSnapshot(): List<ConversationWithResponses> =
        conversationDao.getAllConversationsSnapshot()

    /**
     * Executes queries across all requested models simultaneously.
     */
    suspend fun executeMultiModelQuery(
        prompt: String,
        selectedModels: List<AiModel>,
        onProgress: (ModelResult) -> Unit
    ): Pair<Long, List<ModelResult>> = coroutineScope {
        val currentSettings = settings.value

        // Execute all queries in parallel coroutines
        val deferredList = selectedModels.map { model ->
            async {
                onProgress(
                    ModelResult(
                        model = model,
                        modelVersion = model.defaultVersion,
                        isGenerating = true
                    )
                )
                val result = AstraEngineManager.queryModel(model, prompt, currentSettings)
                onProgress(result)
                result
            }
        }

        val completedResults = deferredList.awaitAll()

        // Track tokens
        val totalTokens = completedResults.sumOf { it.tokensEstimate }
        settingsRepository.trackUsage(totalTokens)

        // Synthesize consensus
        val consensusText = if (currentSettings.autoSynthesizeConsensus && completedResults.size > 1) {
            AstraEngineManager.synthesizeConsensus(prompt, completedResults)
        } else null

        // Auto-generate title and tags
        val title = generateTitleFromPrompt(prompt)
        val tags = generateTagsFromPrompt(prompt)

        val convEntity = ConversationEntity(
            title = title,
            prompt = prompt,
            consensusSummary = consensusText,
            selectedModelIds = selectedModels.joinToString(",") { it.id },
            tags = tags
        )

        val convId = conversationDao.insertConversation(convEntity)

        val responseEntities = completedResults.map { res ->
            ModelResponseEntity(
                conversationId = convId,
                modelId = res.model.id,
                modelVersion = res.modelVersion,
                responseText = res.responseText,
                latencyMs = res.latencyMs,
                tokensEstimate = res.tokensEstimate,
                isWinner = false,
                isStarred = false
            )
        }

        conversationDao.insertResponses(responseEntities)

        Pair(convId, completedResults)
    }

    suspend fun toggleStar(responseId: Long, isStarred: Boolean) {
        conversationDao.setResponseStarred(responseId, isStarred)
    }

    suspend fun togglePin(conversationId: Long, isPinned: Boolean) {
        conversationDao.setConversationPinned(conversationId, isPinned)
    }

    suspend fun setWinner(conversationId: Long, responseId: Long) {
        conversationDao.setWinnerForConversation(conversationId, responseId)
    }

    suspend fun deleteConversation(id: Long) {
        conversationDao.deleteConversationById(id)
    }

    suspend fun clearHistory() {
        conversationDao.clearAll()
    }

    private fun generateTitleFromPrompt(prompt: String): String {
        val clean = prompt.trim().lines().firstOrNull() ?: "Consulta Astra"
        return if (clean.length > 40) clean.take(37) + "..." else clean
    }

    private fun generateTagsFromPrompt(prompt: String): String {
        val lower = prompt.lowercase()
        val tags = mutableListOf<String>()
        if (lower.contains("codigo") || lower.contains("python") || lower.contains("kotlin") || lower.contains("bug")) tags.add("código")
        if (lower.contains("historia") || lower.contains("poema") || lower.contains("escribe")) tags.add("creatividad")
        if (lower.contains("negocio") || lower.contains("ventas") || lower.contains("dinero")) tags.add("negocios")
        if (lower.contains("compara") || lower.contains("diferencia")) tags.add("comparación")
        if (tags.isEmpty()) tags.add("general")
        return tags.joinToString(", ")
    }
}
