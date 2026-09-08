package dev.tyfino.foundation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Neutral development-only colors. Final branding remains OPEN.
private val FoundationColors = darkColorScheme(
    primary = Color(0xFFE1E3E8),
    onPrimary = Color(0xFF16181C),
    background = Color(0xFF0B0D10),
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF111419),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF20242B),
    onSurfaceVariant = Color(0xFFB7BDC7),
    outline = Color(0xFF6F7682),
)

@Composable
internal fun TyfinoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FoundationColors,
        content = content,
    )
}
