package dev.tyfino.foundation.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.dp

// Keep TYFINO's cyan/violet identity across cards, menus, fields and dialogs.
private val TyfinoColors = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF031525),
    primaryContainer = Color(0xFF123449),
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = Color(0xFF8B5CF6),
    onSecondary = Color(0xFF0F0624),
    secondaryContainer = Color(0xFF292148),
    onSecondaryContainer = Color(0xFFE4DCFF),
    tertiary = Color(0xFF5EEAD4),
    onTertiary = Color(0xFF00382F),
    tertiaryContainer = Color(0xFF123B3A),
    onTertiaryContainer = Color(0xFFA5F3E5),
    background = Color(0xFF07111F),
    onBackground = Color(0xFFF3F8FC),
    surface = Color(0xFF0D1B2A),
    onSurface = Color(0xFFF3F8FC),
    surfaceVariant = Color(0xFF172A3D),
    onSurfaceVariant = Color(0xFFC0D2E0),
    surfaceContainerLowest = Color(0xFF050D17),
    surfaceContainerLow = Color(0xFF0A1625),
    surfaceContainer = Color(0xFF102033),
    surfaceContainerHigh = Color(0xFF172B40),
    surfaceContainerHighest = Color(0xFF1E354D),
    surfaceTint = Color(0xFF38BDF8),
    outline = Color(0xFF66849C),
    outlineVariant = Color(0xFF294057),
    inverseSurface = Color(0xFFDBE9F2),
    inverseOnSurface = Color(0xFF122235),
    inversePrimary = Color(0xFF005D82),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val DefaultTypography = Typography()
private val TyfinoTypography = Typography(
    headlineLarge = DefaultTypography.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = DefaultTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = DefaultTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = DefaultTypography.bodyLarge.copy(
        lineHeight = 1.55.em,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    ),
    bodyMedium = DefaultTypography.bodyMedium.copy(
        lineHeight = 1.55.em,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    ),
)

private val TyfinoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
internal fun TyfinoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TyfinoColors,
        typography = TyfinoTypography,
        shapes = TyfinoShapes,
        content = content,
    )
}
