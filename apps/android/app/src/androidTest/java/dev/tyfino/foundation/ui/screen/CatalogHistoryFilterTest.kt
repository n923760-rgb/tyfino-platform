package dev.tyfino.foundation.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogHistoryFilterTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun recentViewingRemainsAccessibleThroughTheSectionFilter() {
        var historyOpened = false
        compose.setContent {
            MaterialTheme {
                CatalogFilterTabs(
                    favoritesOnly = false,
                    historyOnly = false,
                    onAll = {},
                    onFavorites = {},
                    onHistory = { historyOpened = true },
                )
            }
        }

        compose.onNodeWithTag("catalog-history-filter").performClick()
        compose.runOnIdle { assertEquals(true, historyOpened) }
    }
}
