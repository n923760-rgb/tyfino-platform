package dev.tyfino.foundation.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.tyfino.foundation.xtream.CatalogFailure
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogTileAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun announcesSupportingTextAndExposedSelectionState() {
        compose.setContent {
            MaterialTheme {
                CatalogTile(
                    label = "Featured",
                    supporting = "42% watched",
                    selected = true,
                    exposeSelectionState = true,
                    onClick = {},
                    modifier = Modifier.testTag("catalog-tile"),
                )
            }
        }

        compose.onNodeWithTag("catalog-tile")
            .assertContentDescriptionEquals("Featured, 42% watched")
            .assertIsSelected()
    }

    @Test
    fun artworkPlaceholderDoesNotReplaceCardDescription() {
        compose.setContent {
            MaterialTheme {
                CatalogTile(
                    label = "Featured",
                    supporting = "2026",
                    selected = false,
                    onClick = {},
                    modifier = Modifier.testTag("catalog-tile"),
                    showArtwork = true,
                    artworkUrl = null,
                )
            }
        }

        compose.onNodeWithTag("catalog-tile")
            .assertContentDescriptionEquals("Featured, 2026")
        compose.onNodeWithTag("catalog-artwork-placeholder", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun exposesCatalogFilterSelectionState() {
        compose.setContent {
            MaterialTheme {
                CatalogFilterButton(
                    label = "Favorites only",
                    selected = true,
                    onClick = {},
                    modifier = Modifier.testTag("selected-filter"),
                )
                CatalogFilterButton(
                    label = "Recently watched",
                    selected = false,
                    onClick = {},
                    modifier = Modifier.testTag("unselected-filter"),
                )
            }
        }

        compose.onNodeWithTag("selected-filter").assertIsSelected()
        compose.onNodeWithTag("unselected-filter").assertIsNotSelected()
    }

    @Test
    fun allThreeCatalogTabsStayVisibleAndSelectionIsExclusive() {
        var favorites by mutableStateOf(false)
        var history by mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                CatalogFilterTabs(
                    favoritesOnly = favorites,
                    historyOnly = history,
                    onAll = { favorites = false; history = false },
                    onFavorites = { favorites = true; history = false },
                    onHistory = { history = true; favorites = false },
                )
            }
        }
        compose.onNodeWithTag("catalog-all-filter").assertIsSelected()
        compose.onNodeWithTag("catalog-favorites-filter").assertIsNotSelected().performClick().assertIsSelected()
        compose.onNodeWithTag("catalog-history-filter").assertIsNotSelected().performClick().assertIsSelected()
        compose.onNodeWithTag("catalog-favorites-filter").assertIsNotSelected()
        compose.onNodeWithTag("catalog-all-filter").performClick().assertIsSelected()
    }

    @Test
    fun exposesFavoriteSelectionState() {
        compose.setContent {
            MaterialTheme {
                CatalogFavoriteButton(
                    label = "Remove Featured from favorites",
                    selected = true,
                    onClick = {},
                    modifier = Modifier.testTag("favorite-item"),
                )
                CatalogFavoriteButton(
                    label = "Add New item to favorites",
                    selected = false,
                    onClick = {},
                    modifier = Modifier.testTag("regular-item"),
                )
            }
        }

        compose.onNodeWithTag("favorite-item")
            .assertIsSelected()
            .assertContentDescriptionEquals("Remove Featured from favorites")
        compose.onNodeWithTag("regular-item")
            .assertIsNotSelected()
            .assertContentDescriptionEquals("Add New item to favorites")
    }

    @Test
    fun exposesCatalogFailureAsAssertiveLiveRegion() {
        compose.setContent {
            MaterialTheme {
                CatalogErrorState(
                    failure = CatalogFailure.NetworkUnavailable,
                    onRetry = {},
                )
            }
        }

        compose.onNodeWithTag("catalog-error").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Assertive,
            ),
        )
    }

    @Test
    fun exposesCatalogLoadingAsPoliteLiveRegion() {
        compose.setContent {
            MaterialTheme {
                CatalogLoadingState()
            }
        }

        compose.onNodeWithTag("catalog-loading").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Polite,
            ),
        )
    }

    @Test
    fun exposesStaleCatalogWarningAsAssertiveLiveRegion() {
        compose.setContent {
            MaterialTheme {
                CatalogStaleNotice(CatalogFailure.Timeout)
            }
        }

        compose.onNodeWithTag("catalog-stale").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Assertive,
            ),
        )
    }

    @Test
    fun exposesCatalogRefreshAsPoliteLiveRegion() {
        compose.setContent {
            MaterialTheme {
                CatalogRefreshingNotice()
            }
        }

        compose.onNodeWithTag("catalog-refreshing").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Polite,
            ),
        )
    }
}
