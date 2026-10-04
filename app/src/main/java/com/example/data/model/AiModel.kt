package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class ModelCategory {
    MULTIMODAL_REASONING,
    DEEP_LOGIC_CODE,
    PROSE_SAFETY,
    REALTIME_WIT,
    PRODUCTIVITY_OFFICE,
    OPEN_SOURCE_SPEED
}

enum class SubscriptionTier(val label: String, val badgeColor: Long) {
    FREE("Gratis", 0xFF64748B),
    PRO("Astra Pro", 0xFF6366F1),
    ENTERPRISE("Ultimate", 0xFFA855F7)
}

data class ModelFeature(
    val name: String,
    val description: String,
    val isPremium: Boolean
)

enum class AiModel(
    val id: String,
    val displayName: String,
    val provider: String,
    val tagline: String,
    val accentColor: Color,
    val darkAccentColor: Color,
    val category: ModelCategory,
    val defaultTier: SubscriptionTier,
    val contextWindow: String,
    val description: String,
    val defaultVersion: String,
    val premiumVersion: String,
    val features: List<ModelFeature>
) {
    GEMINI(
        id = "gemini",
        displayName = "Gemini",
        provider = "Google DeepMind",
        tagline = "Razonamiento multimodal y búsqueda en tiempo real",
        accentColor = Color(0xFF1A73E8),
        darkAccentColor = Color(0xFF8AB4F8),
        category = ModelCategory.MULTIMODAL_REASONING,
        defaultTier = SubscriptionTier.FREE,
        contextWindow = "2,000,000 tokens",
        description = "El modelo insignia de Google con ventana de contexto de 2M tokens, análisis multimodal nativo y conexión en tiempo real con Google Search.",
        defaultVersion = "Gemini 3.5 Flash",
        premiumVersion = "Gemini 3.1 Pro (Advanced)",
        features = listOf(
            ModelFeature("Contexto masivo 2M", "Procesamiento de documentos masivos y código", true),
            ModelFeature("Google Search Grounding", "Información web fresca en tiempo real", false),
            ModelFeature("Visión y Audio Nativo", "Análisis multimodal sin pérdidas", true)
        )
    ),
    CHATGPT(
        id = "chatgpt",
        displayName = "ChatGPT",
        provider = "OpenAI",
        tagline = "Razonamiento profundo y generación de código de clase mundial",
        accentColor = Color(0xFF10A37F),
        darkAccentColor = Color(0xFF4EE1A0),
        category = ModelCategory.DEEP_LOGIC_CODE,
        defaultTier = SubscriptionTier.FREE,
        contextWindow = "128,000 tokens",
        description = "Pionero en razonamiento conversacional con arquitecturas GPT-4o y serie o1, especializado en programación, matemáticas y desglose lógico.",
        defaultVersion = "GPT-4o mini",
        premiumVersion = "GPT-4o & o1 Reasoning",
        features = listOf(
            ModelFeature("Cadena de pensamiento o1", "Razonamiento paso a paso para lógica compleja", true),
            ModelFeature("Python Data Analysis", "Ejecución de código en sandbox integrado", true),
            ModelFeature("GPT Store & Prompts", "Especialistas pre-entrenados para cada área", false)
        )
    ),
    CLAUDE(
        id = "claude",
        displayName = "Claude",
        provider = "Anthropic",
        tagline = "Prosa matizada, Artifacts y excelencia en refactorización",
        accentColor = Color(0xFFD97706),
        darkAccentColor = Color(0xFFFBBF24),
        category = ModelCategory.PROSE_SAFETY,
        defaultTier = SubscriptionTier.FREE,
        contextWindow = "200,000 tokens",
        description = "Modelo constitucional de Anthropic reconocido por su tono humano natural, análisis ético cuidadoso y la mejor comprensión sintáctica de código.",
        defaultVersion = "Claude 3.5 Haiku",
        premiumVersion = "Claude 3.5 Sonnet",
        features = listOf(
            ModelFeature("Artifacts Interactivos", "Generación de componentes y diagramas visuales", true),
            ModelFeature("Redacción Editorial Fluida", "Textos sin estilo robótico o repetitivo", false),
            ModelFeature("Análisis de Proyectos Grandes", "Refactorización de arquitecturas completas", true)
        )
    ),
    GROK(
        id = "grok",
        displayName = "Grok",
        provider = "xAI",
        tagline = "Perspectiva sincera, agudeza y datos en tiempo real de X",
        accentColor = Color(0xFFE11D48),
        darkAccentColor = Color(0xFFFB7185),
        category = ModelCategory.REALTIME_WIT,
        defaultTier = SubscriptionTier.FREE,
        contextWindow = "128,000 tokens",
        description = "Desarrollado por xAI con acceso directo a eventos globales instantáneos en la red X, con modo 'Fun' cargado de humor y respuestas sin filtro innecesario.",
        defaultVersion = "Grok 2 Mini",
        premiumVersion = "Grok 2 Fun & Vision",
        features = listOf(
            ModelFeature("Modo Divertido (Fun Mode)", "Respuestas ingeniosas, audaces y con humor", false),
            ModelFeature("Pulso de Tendencias X", "Análisis de conversaciones mundiales en vivo", true),
            ModelFeature("Generador Flux Image", "Generación visual hiperrealista", true)
        )
    ),
    COPILOT(
        id = "copilot",
        displayName = "Copilot",
        provider = "Microsoft",
        tagline = "Productividad ejecutiva, resúmenes y síntesis empresarial",
        accentColor = Color(0xFF0284C7),
        darkAccentColor = Color(0xFF38BDF8),
        category = ModelCategory.PRODUCTIVITY_OFFICE,
        defaultTier = SubscriptionTier.FREE,
        contextWindow = "128,000 tokens",
        description = "El compañero de productividad de Microsoft optimizado para sintetizar reuniones, redactar memorandos corporativos y estructurar tareas accionables.",
        defaultVersion = "Copilot Balanced",
        premiumVersion = "Copilot Pro (Creative & Precise)",
        features = listOf(
            ModelFeature("Síntesis Ejecutiva", "Puntos clave y planes de acción inmediatos", false),
            ModelFeature("Modo Preciso & Creativo", "Ajuste de tono según el entregable", false),
            ModelFeature("Plantillas Profesionales", "Formatos listos para trabajo y negocio", true)
        )
    ),
    LLAMA(
        id = "llama",
        displayName = "Llama",
        provider = "Meta AI",
        tagline = "Código abierto, privacidad y velocidad ultrarrápida",
        accentColor = Color(0xFF8B5CF6),
        darkAccentColor = Color(0xFFA78BFA),
        category = ModelCategory.OPEN_SOURCE_SPEED,
        defaultTier = SubscriptionTier.FREE,
        contextWindow = "128,000 tokens",
        description = "La joya de código abierto de Meta. Ofrece máxima transparencia, alta velocidad de inferencia y la libertad de auditar el razonamiento sin cajas negras.",
        defaultVersion = "Llama 3.3 8B Instant",
        premiumVersion = "Llama 3.3 70B Instruct",
        features = listOf(
            ModelFeature("Privacidad y Apertura", "Sin retención secreta de datos privados", false),
            ModelFeature("Inferencia Ultrarrápida", "Latencias inferiores a 300ms", true),
            ModelFeature("Instrucción Multilingüe", "Excelente rendimiento en español y lógica pura", false)
        )
    );

    companion object {
        fun fromId(id: String): AiModel = entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: GEMINI
    }
}
