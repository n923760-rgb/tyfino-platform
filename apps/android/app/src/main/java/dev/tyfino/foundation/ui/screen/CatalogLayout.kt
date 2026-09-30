package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val CatalogGridSpacing = 8.dp
private val MinimumCatalogCardWidth = 88.dp

internal fun catalogGridColumns(width: Dp, fontScale: Float = 1f): Int {
    val preferredColumns = when {
        width >= 900.dp -> 6
        width >= 600.dp -> 5
        width >= 480.dp -> 4
        else -> 3
    }
    val readableFontScale = if (fontScale.isFinite()) fontScale.coerceAtLeast(1f) else 1f
    val fittingColumns = (
        (width.value + CatalogGridSpacing.value) /
            (MinimumCatalogCardWidth.value * readableFontScale + CatalogGridSpacing.value)
        ).toInt().coerceAtLeast(1)
    return minOf(preferredColumns, fittingColumns)
}

@Composable
internal fun CatalogGrid(content: LazyGridScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(catalogGridColumns(maxWidth, LocalDensity.current.fontScale)),
            modifier = Modifier.fillMaxSize().focusGroup().testTag("catalog-items"),
            horizontalArrangement = Arrangement.spacedBy(CatalogGridSpacing),
            verticalArrangement = Arrangement.spacedBy(CatalogGridSpacing),
            content = content,
        )
    }
}
