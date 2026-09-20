package dev.tyfino.foundation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TyfinoColors = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF031525),
    secondary = Color(0xFF8B5CF6),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFF07111F),
    onBackground = Color(0xFFF3F8FC),
    surface = Color(0xFF0D1B2A),
    onSurface = Color(0xFFF3F8FC),
    surfaceVariant = Color(0xFF172A3D),
    onSurfaceVariant = Color(0xFFC0D2E0),
    outline = Color(0xFF66849C),
)

@Composable
internal fun TyfinoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TyfinoColors,
        content = content,
    )
}
