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
}
