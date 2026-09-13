package dev.tyfino.foundation.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
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
}
