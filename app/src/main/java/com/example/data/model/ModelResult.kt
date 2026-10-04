package com.example.data.model

data class ModelResult(
    val model: AiModel,
    val modelVersion: String,
    val responseText: String = "",
    val thinkingTrace: String? = null,
    val latencyMs: Long = 0,
    val tokensEstimate: Int = 0,
    val isGenerating: Boolean = false,
    val isWinner: Boolean = false,
    val isStarred: Boolean = false,
    val error: String? = null
)
