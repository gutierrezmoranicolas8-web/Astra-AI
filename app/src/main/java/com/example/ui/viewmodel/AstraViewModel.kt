package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AstraDatabase
import com.example.data.local.ConversationEntity
import com.example.data.local.ConversationWithResponses
import com.example.data.local.ModelResponseEntity
import com.example.data.local.QueryIntent
import com.example.data.local.SearchResultItem
import com.example.data.local.SemanticSearchEngine
import com.example.data.model.AiModel
import com.example.data.model.ModelResult
import com.example.data.model.SubscriptionTier
import com.example.data.preferences.AstraSettings
import com.example.data.preferences.AstraSettingsRepository
import com.example.data.remote.AstraEngineManager
import com.example.data.repository.ConversationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AstraNavDestination(val label: String) {
    CHAT("Astra Chat"),
    SPLIT_SCREEN("Comparador"),
    SEARCH("Búsqueda IA"),
    MODELS("Modelos y Planes"),
    SETTINGS("Ajustes")
}

data class AstraUiState(
    val currentDestination: AstraNavDestination = AstraNavDestination.CHAT,
    val promptInput: String = "",
    val selectedModels: Set<AiModel> = setOf(AiModel.GEMINI, AiModel.CHATGPT, AiModel.CLAUDE),
    val activeResults: Map<AiModel, ModelResult> = emptyMap(),
    val isGenerating: Boolean = false,
    val activeConsensus: String? = null,
    val activeConversationId: Long? = null,
    val splitScreenLeftModel: AiModel = AiModel.GEMINI,
    val splitScreenRightModel: AiModel = AiModel.CHATGPT,
    // Semantic Search state
    val searchQuery: String = "",
    val searchIntentFilter: QueryIntent = QueryIntent.ALL,
    val searchModelFilter: String? = null,
    val searchResults: List<SearchResultItem> = emptyList(),
    val isSearching: Boolean = false,
    val activeConversationDetail: ConversationWithResponses? = null,
    val infoMessage: String? = null
)

class AstraViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AstraDatabase.getDatabase(application)
    private val settingsRepo = AstraSettingsRepository(application)
    private val repository = ConversationRepository(db.conversationDao(), settingsRepo)

    val settings: StateFlow<AstraSettings> = settingsRepo.settingsFlow
    val conversations: StateFlow<List<ConversationWithResponses>> = repository.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(AstraUiState())
    val uiState: StateFlow<AstraUiState> = _uiState.asStateFlow()

    init {
        // Pre-populate with realistic starter conversations if database is empty
        viewModelScope.launch {
            val count = repository.getAllConversationsSnapshot().size
            if (count == 0) {
                seedInitialConversations()
            }
        }
    }

    fun setDestination(dest: AstraNavDestination) {
        _uiState.value = _uiState.value.copy(currentDestination = dest)
    }

    fun updatePromptInput(input: String) {
        _uiState.value = _uiState.value.copy(promptInput = input)
    }

    fun toggleModelSelection(model: AiModel) {
        val current = _uiState.value.selectedModels.toMutableSet()
        if (current.contains(model)) {
            if (current.size > 1) { // Keep at least one model active
                current.remove(model)
            }
        } else {
            current.add(model)
        }
        _uiState.value = _uiState.value.copy(selectedModels = current)
    }

    fun selectAllModels() {
        _uiState.value = _uiState.value.copy(selectedModels = AiModel.entries.toSet())
    }

    fun selectTopThreeModels() {
        _uiState.value = _uiState.value.copy(
            selectedModels = setOf(AiModel.GEMINI, AiModel.CHATGPT, AiModel.CLAUDE)
        )
    }

    fun setSplitScreenModels(left: AiModel, right: AiModel) {
        _uiState.value = _uiState.value.copy(
            splitScreenLeftModel = left,
            splitScreenRightModel = right
        )
    }

    fun sendPrompt(customPrompt: String? = null) {
        val textToSend = (customPrompt ?: _uiState.value.promptInput).trim()
        if (textToSend.isBlank() || _uiState.value.isGenerating) return

        val modelsToQuery = _uiState.value.selectedModels.toList()
        _uiState.value = _uiState.value.copy(
            isGenerating = true,
            promptInput = "",
            activeConsensus = null,
            activeResults = modelsToQuery.associateWith { model ->
                ModelResult(
                    model = model,
                    modelVersion = model.defaultVersion,
                    isGenerating = true
                )
            }
        )

        viewModelScope.launch {
            try {
                val (convId, results) = repository.executeMultiModelQuery(
                    prompt = textToSend,
                    selectedModels = modelsToQuery,
                    onProgress = { result ->
                        val currentMap = _uiState.value.activeResults.toMutableMap()
                        currentMap[result.model] = result
                        _uiState.value = _uiState.value.copy(activeResults = currentMap)
                    }
                )

                val consensus = if (results.size > 1) {
                    AstraEngineManager.synthesizeConsensus(textToSend, results)
                } else null

                val resultsMap = results.associateBy { it.model }
                _uiState.value = _uiState.value.copy(
                    activeConversationId = convId,
                    activeResults = resultsMap,
                    activeConsensus = consensus,
                    isGenerating = false,
                    infoMessage = "Respuestas generadas simultáneamente por ${results.size} modelos."
                )

                // If currently in Search tab, re-run search to include new results
                if (_uiState.value.searchQuery.isNotBlank()) {
                    executeSearch(_uiState.value.searchQuery)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    infoMessage = "Error al consultar los modelos: ${e.message}"
                )
            }
        }
    }

    fun voteWinner(model: AiModel) {
        val currentConvId = _uiState.value.activeConversationId
        val currentResults = _uiState.value.activeResults.toMutableMap()
        val target = currentResults[model] ?: return

        // Update in-memory
        for ((m, res) in currentResults) {
            currentResults[m] = res.copy(isWinner = (m == model))
        }
        _uiState.value = _uiState.value.copy(
            activeResults = currentResults,
            infoMessage = "¡${model.displayName} seleccionado como respuesta ganadora!"
        )

        // Persist to Room
        if (currentConvId != null) {
            viewModelScope.launch {
                val conv = repository.getAllConversationsSnapshot().firstOrNull { it.conversation.id == currentConvId }
                val respEntity = conv?.responses?.firstOrNull { it.modelId.equals(model.id, ignoreCase = true) }
                if (respEntity != null) {
                    repository.setWinner(currentConvId, respEntity.id)
                }
            }
        }
    }

    fun toggleStarResponse(responseId: Long, isStarred: Boolean) {
        viewModelScope.launch {
            repository.toggleStar(responseId, !isStarred)
            // Refresh detail if open
            val currentDetail = _uiState.value.activeConversationDetail
            if (currentDetail != null) {
                loadConversationDetail(currentDetail.conversation.id)
            }
            if (_uiState.value.searchQuery.isNotBlank()) {
                executeSearch(_uiState.value.searchQuery)
            }
        }
    }

    fun togglePinConversation(conversationId: Long, isPinned: Boolean) {
        viewModelScope.launch {
            repository.togglePin(conversationId, !isPinned)
        }
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            repository.deleteConversation(id)
            if (_uiState.value.activeConversationDetail?.conversation?.id == id) {
                _uiState.value = _uiState.value.copy(activeConversationDetail = null)
            }
            if (_uiState.value.searchQuery.isNotBlank()) {
                executeSearch(_uiState.value.searchQuery)
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _uiState.value = _uiState.value.copy(
                activeConversationId = null,
                activeResults = emptyMap(),
                activeConsensus = null,
                activeConversationDetail = null,
                searchResults = emptyList(),
                infoMessage = "Historial consolidado limpiado."
            )
        }
    }

    fun loadConversationDetail(id: Long) {
        viewModelScope.launch {
            val all = repository.getAllConversationsSnapshot()
            val found = all.firstOrNull { it.conversation.id == id }
            if (found != null) {
                val resultsMap = found.responses.associate { resp ->
                    val model = AiModel.fromId(resp.modelId)
                    model to ModelResult(
                        model = model,
                        modelVersion = resp.modelVersion,
                        responseText = resp.responseText,
                        latencyMs = resp.latencyMs,
                        tokensEstimate = resp.tokensEstimate,
                        isWinner = resp.isWinner,
                        isStarred = resp.isStarred,
                        isGenerating = false
                    )
                }
                _uiState.value = _uiState.value.copy(
                    activeConversationDetail = found,
                    activeConversationId = found.conversation.id,
                    activeResults = resultsMap,
                    activeConsensus = found.conversation.consensusSummary
                )
            }
        }
    }

    fun closeConversationDetail() {
        _uiState.value = _uiState.value.copy(activeConversationDetail = null)
    }

    // --- Semantic Search methods ---
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        executeSearch(query)
    }

    fun setSearchIntentFilter(intent: QueryIntent) {
        _uiState.value = _uiState.value.copy(searchIntentFilter = intent)
        executeSearch(_uiState.value.searchQuery)
    }

    fun setSearchModelFilter(modelId: String?) {
        _uiState.value = _uiState.value.copy(searchModelFilter = modelId)
        executeSearch(_uiState.value.searchQuery)
    }

    private fun executeSearch(query: String) {
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            val allConvs = repository.getAllConversationsSnapshot()
            val results = SemanticSearchEngine.search(
                query = query,
                conversations = allConvs,
                selectedModelIdFilter = _uiState.value.searchModelFilter,
                intentFilter = _uiState.value.searchIntentFilter
            )
            _uiState.value = _uiState.value.copy(
                searchResults = results,
                isSearching = false
            )
        }
    }

    // --- Settings and Subscriptions ---
    fun updateTier(tier: SubscriptionTier) {
        settingsRepo.updateTier(tier)
        _uiState.value = _uiState.value.copy(
            infoMessage = "Plan actualizado a ${tier.label}. Límites ajustados de forma transparente."
        )
    }

    fun saveApiKeys(
        gemini: String,
        openai: String,
        claude: String,
        grok: String,
        copilot: String,
        llama: String
    ) {
        settingsRepo.saveApiKeys(gemini, openai, claude, grok, copilot, llama)
        _uiState.value = _uiState.value.copy(
            infoMessage = "Claves de API guardadas correctamente."
        )
    }

    fun clearInfoMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }

    private suspend fun seedInitialConversations() {
        // Seed 1: Coding & Architecture
        val conv1 = ConversationEntity(
            title = "Arquitectura de Software y Coroutines",
            prompt = "¿Cómo implementar un procesador concurrente de tareas en Kotlin?",
            consensusSummary = "Astra Consensus: Se recomienda estructurar la lógica con Coroutines, Dispatchers.Default y manejo inmutable de estados.",
            selectedModelIds = "gemini,chatgpt,claude",
            tags = "código, kotlin, arquitectura"
        )
        val id1 = db.conversationDao().insertConversation(conv1)
        db.conversationDao().insertResponses(
            listOf(
                ModelResponseEntity(
                    conversationId = id1,
                    modelId = "gemini",
                    modelVersion = "Gemini 3.5 Flash",
                    responseText = "Para procesar tareas concurrentes en Kotlin, lo ideal es usar un `CoroutineScope` estructurado con `SupervisorJob` y canalizar errores con `CoroutineExceptionHandler`.\n\n```kotlin\nval scope = CoroutineScope(Dispatchers.Default + SupervisorJob())\n```",
                    latencyMs = 430,
                    tokensEstimate = 320,
                    isWinner = false,
                    isStarred = true
                ),
                ModelResponseEntity(
                    conversationId = id1,
                    modelId = "chatgpt",
                    modelVersion = "GPT-4o",
                    responseText = "Aquí tienes una implementación robusta basada en el patrón Worker:\n\n```kotlin\nclass ConcurrentTaskManager(private val maxConcurrency: Int = 4) {\n    private val semaphore = Semaphore(maxConcurrency)\n    suspend fun <T> runTask(block: suspend () -> T): T {\n        return semaphore.withPermit { block() }\n    }\n}\n```",
                    latencyMs = 580,
                    tokensEstimate = 410,
                    isWinner = true,
                    isStarred = true
                ),
                ModelResponseEntity(
                    conversationId = id1,
                    modelId = "claude",
                    modelVersion = "Claude 3.5 Sonnet",
                    responseText = "Es primordial considerar la cancelabilidad cooperativa. Al usar `ensureActive()` o `yield()` dentro de bucles intensivos, garantizas que las coroutines liberen recursos limpiamente sin fugas de memoria.",
                    latencyMs = 720,
                    tokensEstimate = 390,
                    isWinner = false,
                    isStarred = false
                )
            )
        )

        // Seed 2: Business & ROI
        val conv2 = ConversationEntity(
            title = "Estrategia de Crecimiento y ROI",
            prompt = "Prepara un resumen ejecutivo para presentar el presupuesto de IA a la junta directiva",
            consensusSummary = "Astra Consensus: Enfatizar el retorno sobre inversión (ROI) a 6 meses, ahorro de horas operativas y gobernanza de datos.",
            selectedModelIds = "copilot,grok,llama",
            tags = "negocios, presupuesto, finanzas"
        )
        val id2 = db.conversationDao().insertConversation(conv2)
        db.conversationDao().insertResponses(
            listOf(
                ModelResponseEntity(
                    conversationId = id2,
                    modelId = "copilot",
                    modelVersion = "Copilot Balanced",
                    responseText = "### 📋 Resumen Ejecutivo de Presupuesto\n\n- **Inversión Propuesta:** \$45,000 USD\n- **ROI Esperado:** Reducción del 28% en tiempos de soporte técnico.\n- **Hito Q1:** Adopción del 80% en equipos clave.",
                    latencyMs = 490,
                    tokensEstimate = 290,
                    isWinner = true,
                    isStarred = false
                ),
                ModelResponseEntity(
                    conversationId = id2,
                    modelId = "grok",
                    modelVersion = "Grok 2 Fun",
                    responseText = "Diles la verdad sin rodeos: si no aprueban la IA ahora, en 18 meses la competencia los va a dejar atrás. El ROI no es solo dinero ahorrado, es relevancia en el mercado.",
                    latencyMs = 510,
                    tokensEstimate = 220,
                    isWinner = false,
                    isStarred = true
                )
            )
        )
    }
}
