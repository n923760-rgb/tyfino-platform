package dev.tyfino.foundation.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
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

        compose.onNodeWithTag("favorite-item").assertIsSelected()
        compose.onNodeWithTag("regular-item").assertIsNotSelected()
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
}
