package dev.tyfino.foundation.ui.screen

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogGridColumnsTest {
    @Test
    fun portraitPhoneUsesThreeCards() {
        assertEquals(3, catalogGridColumns(288.dp))
        assertEquals(3, catalogGridColumns(328.dp))
        assertEquals(3, catalogGridColumns(479.dp))
    }

    @Test
    fun columnsScaleUpAtAvailableWidthBreakpoints() {
        assertEquals(4, catalogGridColumns(480.dp))
        assertEquals(4, catalogGridColumns(599.dp))
        assertEquals(5, catalogGridColumns(600.dp))
        assertEquals(5, catalogGridColumns(899.dp))
        assertEquals(6, catalogGridColumns(900.dp))
        assertEquals(6, catalogGridColumns(1000.dp))
    }

    @Test
    fun narrowPanesReduceColumnsBeforeCardsBecomeTooSmall() {
        assertEquals(1, catalogGridColumns(0.dp))
        assertEquals(1, catalogGridColumns(128.dp))
        assertEquals(2, catalogGridColumns(200.dp))
        assertEquals(2, catalogGridColumns(279.dp))
        assertEquals(3, catalogGridColumns(280.dp))
    }

    @Test
    fun largerFontsReduceDensityAtPhoneAndExpandedWidths() {
        assertEquals(2, catalogGridColumns(328.dp, 1.3f))
        assertEquals(3, catalogGridColumns(480.dp, 1.3f))
        assertEquals(4, catalogGridColumns(600.dp, 1.5f))
        assertEquals(4, catalogGridColumns(900.dp, 2f))
        assertEquals(5, catalogGridColumns(1000.dp, 2f))
        assertEquals(6, catalogGridColumns(1440.dp, 2f))
    }

    @Test
    fun smallerFontsDoNotIncreaseBaselineDensity() {
        assertEquals(3, catalogGridColumns(328.dp, 0.85f))
        assertEquals(6, catalogGridColumns(1440.dp, 0.85f))
    }
}
