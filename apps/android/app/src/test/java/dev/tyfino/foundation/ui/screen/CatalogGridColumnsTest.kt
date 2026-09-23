package dev.tyfino.foundation.ui.screen

import androidx.compose.ui.unit.dp
import dev.tyfino.foundation.xtream.CatalogSection
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogGridColumnsTest {
    @Test
    fun postersFitThreeColumnsOnCommonPhonesAndStayBoundedOnLargeScreens() {
        assertEquals(2, catalogGridColumns(288.dp, CatalogSection.Movies))
        assertEquals(3, catalogGridColumns(328.dp, CatalogSection.Movies))
        assertEquals(3, catalogGridColumns(328.dp, CatalogSection.Series))
        assertEquals(6, catalogGridColumns(1000.dp, CatalogSection.Movies))
    }

    @Test
    fun landscapeChannelArtworkKeepsWiderTiles() {
        assertEquals(2, catalogGridColumns(328.dp, CatalogSection.Live))
        assertEquals(4, catalogGridColumns(700.dp, CatalogSection.Live))
    }
}
