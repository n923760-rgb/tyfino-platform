package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogLayoutAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun artworkCardsAlignWithLargeArabicTitlesAndRemainInteractive() {
        val shortTitle = "قناة"
        val longTitle = "مغامرات جديدة في عالم الطبيعة والبحار"
        var lastClicked = ""
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(density, 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                MaterialTheme {
                    Row {
                        CatalogTile(
                            label = shortTitle,
                            supporting = "2026",
                            selected = false,
                            onClick = { lastClicked = shortTitle },
                            modifier = Modifier.width(144.dp).testTag("short-card"),
                            showArtwork = true,
                            artworkAspectRatio = 2f / 3f,
                        )
                        CatalogTile(
                            label = longTitle,
                            supporting = "2026",
                            selected = false,
                            onClick = { lastClicked = longTitle },
                            modifier = Modifier.width(144.dp).testTag("long-card"),
                            showArtwork = true,
                            artworkAspectRatio = 2f / 3f,
                        )
                    }
                }
            }
        }

        val shortCard = compose.onNodeWithTag("short-card")
        val longCard = compose.onNodeWithTag("long-card")
        shortCard.assertIsDisplayed().assertContentDescriptionEquals("$shortTitle, 2026")
        longCard.assertIsDisplayed().assertContentDescriptionEquals("$longTitle, 2026")
        assertEquals(
            "Equal-width artwork cards should reserve equal title height at 200% font scale",
            shortCard.fetchSemanticsNode().boundsInRoot.height,
            longCard.fetchSemanticsNode().boundsInRoot.height,
            1f,
        )
        shortCard.performClick()
        compose.runOnIdle { assertEquals(shortTitle, lastClicked) }
        longCard.performClick()
        compose.runOnIdle { assertEquals(longTitle, lastClicked) }
    }

    @Test
    fun gridReflowsWithFontScaleAndKeepsItemActions() {
        var fontScale by mutableStateOf(1f)
        var lastClicked = 0
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                MaterialTheme {
                    Box(Modifier.width(328.dp).height(560.dp)) {
                        CatalogGrid {
                            items((1..6).toList(), key = { it }) { id ->
                                CatalogTile(
                                    label = "Channel $id",
                                    supporting = "2026",
                                    selected = false,
                                    onClick = { lastClicked = id },
                                    modifier = Modifier.fillMaxWidth().testTag("tile-$id"),
                                    showArtwork = true,
                                )
                            }
                        }
                    }
                }
            }
        }

        fun top(id: Int) = compose.onNodeWithTag("tile-$id").fetchSemanticsNode().boundsInRoot.top
        assertEquals(top(1), top(3), 1f)
        compose.runOnIdle { fontScale = 1.3f }
        assertEquals(top(1), top(2), 1f)
        assertTrue("Larger fonts should move the third card to the next row", top(3) > top(1))
        compose.onNodeWithTag("tile-3").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(3, lastClicked) }
        compose.runOnIdle { fontScale = 1f }
        assertEquals(top(1), top(3), 1f)
    }
}
