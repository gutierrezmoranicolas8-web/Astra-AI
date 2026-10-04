package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AstraPrimaryDark,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = AstraPrimaryContainer,
    onPrimaryContainer = AstraOnPrimaryContainer,
    secondary = AstraSecondaryDark,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = AstraSecondaryContainer,
    tertiary = AstraTertiaryDark,
    background = CosmicBackgroundDark,
    onBackground = Color(0xFFF1F5F9),
    surface = CosmicSurfaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = CosmicSurfaceVariantDark,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = CosmicOutlineDark,
    outlineVariant = Color(0xFF1E293B)
)

private val LightColorScheme = lightColorScheme(
    primary = AstraPrimary,
    onPrimary = Color.White,
    primaryContainer = AstraPrimaryContainer,
    onPrimaryContainer = AstraOnPrimaryContainer,
    secondary = AstraSecondary,
    onSecondary = Color.White,
    tertiary = AstraTertiary,
    background = CosmicBackgroundLight,
    onBackground = Color(0xFF0F172A),
    surface = CosmicSurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = CosmicSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF64748B),
    outline = CosmicOutlineLight,
    outlineVariant = Color(0xFFE2E8F0)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve our custom curated Astra palette
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
