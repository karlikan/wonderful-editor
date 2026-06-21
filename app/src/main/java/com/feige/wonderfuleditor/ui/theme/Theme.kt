package com.feige.wonderfuleditor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Цветовые палитры для трех тем
val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFF3F4F6)
val LightPrimary = Color(0xFF4F46E5) // Indigo
val LightOnPrimary = Color(0xFFFFFFFF)
val LightOnBackground = Color(0xFF1F2937)
val LightOnSurface = Color(0xFF374151)

val DarkBackground = Color(0xFF121212) // Very dark gray
val DarkSurface = Color(0xFF1E1E1E)
val DarkPrimary = Color(0xFF90CAF9) // Light blue
val DarkOnPrimary = Color(0xFF0D47A1)
val DarkOnBackground = Color(0xFFF3F4F6)
val DarkOnSurface = Color(0xFFE5E7EB)

val YellowBackground = Color(0xFFF4ECD8) // Warm Sepia / Old Paper
val YellowSurface = Color(0xFFEAE0C8)
val YellowPrimary = Color(0xFF854D0E) // Warm Amber
val YellowOnPrimary = Color(0xFFFFFFFF)
val YellowOnBackground = Color(0xFF2B2B2B) // Warm Charcoal
val YellowOnSurface = Color(0xFF451A03)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface
)

private val YellowColorScheme = lightColorScheme(
    primary = YellowPrimary,
    onPrimary = YellowOnPrimary,
    background = YellowBackground,
    onBackground = YellowOnBackground,
    surface = YellowSurface,
    onSurface = YellowOnSurface
)

@Composable
fun WonderfulEditorTheme(
    themeName: String = "dark", // "light", "dark", "yellow"
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName.lowercase()) {
        "light" -> LightColorScheme
        "yellow" -> YellowColorScheme
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
