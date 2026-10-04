package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ConversationEntity
import com.example.data.local.ConversationWithResponses
import com.example.data.local.ModelResponseEntity
import com.example.data.local.QueryIntent
import com.example.data.local.SemanticSearchEngine
import com.example.data.model.AiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Astra AI", appName)
    }

    @Test
    fun `test all six AI models are available`() {
        assertEquals(6, AiModel.entries.size)
        assertNotNull(AiModel.fromId("gemini"))
        assertNotNull(AiModel.fromId("chatgpt"))
        assertNotNull(AiModel.fromId("claude"))
        assertNotNull(AiModel.fromId("grok"))
        assertNotNull(AiModel.fromId("copilot"))
        assertNotNull(AiModel.fromId("llama"))
    }

    @Test
    fun `test semantic search intent detection`() {
        val codeIntent = SemanticSearchEngine.detectIntent("como corregir un error de sintaxis en python")
        assertEquals(QueryIntent.CODE_DEV, codeIntent)

        val bizIntent = SemanticSearchEngine.detectIntent("presupuesto financiero y roi trimestral")
        assertEquals(QueryIntent.BUSINESS, bizIntent)
    }

    @Test
    fun `test semantic search finds relevant items`() {
        val conv = ConversationEntity(
            id = 1,
            title = "Desarrollo Android",
            prompt = "Como usar Room database en Kotlin",
            selectedModelIds = "gemini,chatgpt"
        )
        val resp = ModelResponseEntity(
            id = 10,
            conversationId = 1,
            modelId = "gemini",
            modelVersion = "Gemini 3.5 Flash",
            responseText = "Room es un ORM sobre SQLite que permite definir entidades, DAOs y consultas reactivas con Flow.",
            latencyMs = 350,
            tokensEstimate = 120
        )
        val data = listOf(ConversationWithResponses(conv, listOf(resp)))

        val results = SemanticSearchEngine.search(
            query = "base de datos sqlite",
            conversations = data
        )

        assertTrue(results.isNotEmpty())
        assertEquals("gemini", results.first().model.id)
    }
}
