package dev.tyfino.foundation.ui.screen

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogGridColumnsTest {
    @Test
    fun narrowPhoneUsesTwoCardsIncludingAtTheBreakpoint() {
        assertEquals(2, catalogGridColumns(288.dp))
        assertEquals(2, catalogGridColumns(328.dp))
        assertEquals(2, catalogGridColumns(479.dp))
    }

    @Test
    fun wideLayoutsUseFourCardsAtAndBeyondTheBreakpoint() {
        assertEquals(4, catalogGridColumns(480.dp))
        assertEquals(4, catalogGridColumns(700.dp))
        assertEquals(4, catalogGridColumns(1000.dp))
    }
}
