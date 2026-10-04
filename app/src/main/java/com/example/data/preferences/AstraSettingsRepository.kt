package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.model.SubscriptionTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AstraSettings(
    val currentTier: SubscriptionTier = SubscriptionTier.PRO,
    val geminiApiKey: String = "",
    val openaiApiKey: String = "",
    val claudeApiKey: String = "",
    val grokApiKey: String = "",
    val copilotApiKey: String = "",
    val llamaApiKey: String = "",
    val totalTokensUsed: Long = 18450,
    val totalQueriesCount: Int = 12,
    val splitScreenLayoutMode: String = "DUAL_COLUMN", // "DUAL_COLUMN" or "TABS_PAGER"
    val autoSynthesizeConsensus: Boolean = true,
    val isSimulationFallbackEnabled: Boolean = true
)

class AstraSettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("astra_ai_settings_prefs", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<AstraSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): AstraSettings {
        val tierOrdinal = prefs.getInt("tier_ordinal", SubscriptionTier.PRO.ordinal)
        val defaultGeminiKey = try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key != "MY_GEMINI_API_KEY" && key.isNotBlank()) key else ""
        } catch (_: Exception) {
            ""
        }

        return AstraSettings(
            currentTier = SubscriptionTier.entries.getOrElse(tierOrdinal) { SubscriptionTier.PRO },
            geminiApiKey = prefs.getString("gemini_key", defaultGeminiKey) ?: defaultGeminiKey,
            openaiApiKey = prefs.getString("openai_key", "") ?: "",
            claudeApiKey = prefs.getString("claude_key", "") ?: "",
            grokApiKey = prefs.getString("grok_key", "") ?: "",
            copilotApiKey = prefs.getString("copilot_key", "") ?: "",
            llamaApiKey = prefs.getString("llama_key", "") ?: "",
            totalTokensUsed = prefs.getLong("total_tokens", 18450L),
            totalQueriesCount = prefs.getInt("total_queries", 12),
            splitScreenLayoutMode = prefs.getString("split_layout", "DUAL_COLUMN") ?: "DUAL_COLUMN",
            autoSynthesizeConsensus = prefs.getBoolean("auto_consensus", true),
            isSimulationFallbackEnabled = prefs.getBoolean("simulation_fallback", true)
        )
    }

    fun updateTier(tier: SubscriptionTier) {
        prefs.edit().putInt("tier_ordinal", tier.ordinal).apply()
        _settingsFlow.value = _settingsFlow.value.copy(currentTier = tier)
    }

    fun saveApiKeys(
        geminiKey: String,
        openaiKey: String,
        claudeKey: String,
        grokKey: String,
        copilotKey: String,
        llamaKey: String
    ) {
        prefs.edit()
            .putString("gemini_key", geminiKey)
            .putString("openai_key", openaiKey)
            .putString("claude_key", claudeKey)
            .putString("grok_key", grokKey)
            .putString("copilot_key", copilotKey)
            .putString("llama_key", llamaKey)
            .apply()
        _settingsFlow.value = _settingsFlow.value.copy(
            geminiApiKey = geminiKey,
            openaiApiKey = openaiKey,
            claudeApiKey = claudeKey,
            grokApiKey = grokKey,
            copilotApiKey = copilotKey,
            llamaApiKey = llamaKey
        )
    }

    fun trackUsage(tokens: Int) {
        val newTokens = _settingsFlow.value.totalTokensUsed + tokens
        val newQueries = _settingsFlow.value.totalQueriesCount + 1
        prefs.edit()
            .putLong("total_tokens", newTokens)
            .putInt("total_queries", newQueries)
            .apply()
        _settingsFlow.value = _settingsFlow.value.copy(
            totalTokensUsed = newTokens,
            totalQueriesCount = newQueries
        )
    }

    fun toggleSplitLayout(mode: String) {
        prefs.edit().putString("split_layout", mode).apply()
        _settingsFlow.value = _settingsFlow.value.copy(splitScreenLayoutMode = mode)
    }
}
