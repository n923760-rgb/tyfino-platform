package dev.tyfino.foundation.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.xtream.CatalogItem
import dev.tyfino.foundation.xtream.MovieDetails
import dev.tyfino.foundation.xtream.MovieDetailsFailure
import dev.tyfino.foundation.xtream.MovieDetailsState
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MovieDetailsScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun useDpadInputMode() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(false)
        compose.activity.window.decorView.requestFocus()
    }

    @After
    fun restoreTouchInputMode() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
    }

    @Test
    fun rendersProviderDetailsAndUsesExplicitResumeAction() {
        var played = 0
        compose.setContent {
            MaterialTheme {
                MovieDetailsContent(
                    selection = selection(),
                    state = MovieDetailsState.Content(details(), false),
                    ownerChanged = false,
                    canResume = true,
                    onBack = {},
                    onRefresh = {},
                    onPlay = { played++ },
                )
            }
        }

        compose.onNodeWithTag("movie-details").assertExists()
        compose.onNodeWithText("Provider title").assertExists()
        compose.onNodeWithText("Story").assertExists()
        compose.onNodeWithText("Resume movie").performClick()
        compose.runOnIdle { assertEquals(1, played) }
    }

    @Test
    fun catalogMetadataRemainsVisibleWhenNetworkFails() {
        compose.setContent {
            MaterialTheme {
                MovieDetailsContent(selection(), MovieDetailsState.Error(MovieDetailsFailure.NetworkUnavailable), false, false, {}, {}, {})
            }
        }

        compose.onNodeWithText("Catalog title").assertExists()
        compose.onNodeWithText("2024 • 7.5").assertExists()
        compose.onNodeWithText("Play movie").assertExists()
        compose.onNodeWithText("No usable internet connection is available.").assertExists()
    }

    @Test
    fun backActionReceivesInitialDpadFocus() {
        compose.setContent {
            MaterialTheme {
                MovieDetailsContent(selection(), MovieDetailsState.Loading, false, false, {}, {}, {})
            }
        }

        compose.onNodeWithTag("movie-back").assertIsFocused()
    }

    private fun selection() = MovieSelection(
        "account", 1L,
        CatalogItem("42", "category", "Catalog title", 0, null, "7.5", "2024", "mp4"),
    )

    private fun details() = MovieDetails(
        "account", "42", "Provider title", "Story", "Drama", "2025", "8.4", "01:42:00",
        "Actor", "Director", null, null,
    )
}
