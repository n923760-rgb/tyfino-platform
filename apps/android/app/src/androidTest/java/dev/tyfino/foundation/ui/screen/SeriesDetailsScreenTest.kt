package dev.tyfino.foundation.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.tyfino.foundation.xtream.CatalogItem
import dev.tyfino.foundation.xtream.SeriesDetailsCandidate
import dev.tyfino.foundation.xtream.SeriesEpisode
import dev.tyfino.foundation.xtream.SeriesSeason
import dev.tyfino.foundation.xtream.SeriesState
import dev.tyfino.foundation.xtream.SeriesSummary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SeriesDetailsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun selectingSeasonPreservesStableSelectionAcrossRefreshAndShowsFallbackWhenRemoved() {
        var state by mutableStateOf<SeriesState>(SeriesState.Content(details(listOf(0, 2)), 1L, false))
        var selected by mutableStateOf<Int?>(null)
        compose.setContent {
            MaterialTheme {
                SeriesDetailsContent(selection(), state, false, selected, { selected = it }, {}, {})
            }
        }

        compose.onNodeWithTag("series-seasons").assertExists()
        compose.onNodeWithText("Special episode").assertExists()
        compose.onNodeWithText("Second season episode").assertDoesNotExist()
        compose.onNodeWithText("Season 2").performClick()
        compose.onNodeWithText("Second season episode").assertExists()
        compose.runOnIdle { state = SeriesState.Content(details(listOf(0, 2)), 2L, false) }
        compose.onNodeWithText("Second season episode").assertExists()
        compose.runOnIdle { state = SeriesState.Content(details(listOf(0)), 3L, false) }
        compose.onNodeWithText("Special episode").assertExists()
        compose.runOnIdle { assertEquals(2, selected) }
    }

    @Test
    fun onlyEpisodesWithPublishedGenerationAndSafeExtensionAreSelectable() {
        var played = 0
        val published = details(listOf(0))
        var state by mutableStateOf<SeriesState>(SeriesState.Content(published, 1L, false, 2L))
        compose.setContent {
            MaterialTheme {
                SeriesDetailsContent(selection(), state, false, null, {}, {}, {}, { _, _ -> played++ })
            }
        }

        compose.onNodeWithText("Special episode").performClick()
        compose.runOnIdle { assertEquals(1, played) }
        compose.runOnIdle {
            state = SeriesState.Content(
                published.copy(episodes = published.episodes.map { it.copy(containerExtension = null) }),
                2L, false, 3L,
            )
        }
        compose.onNodeWithText("Special episode").assertExists()
        compose.onNodeWithText("Playback information is unavailable for this episode.").assertExists()
    }

    private fun selection() = SeriesSelection(
        accountId = "account",
        accountGeneration = 1L,
        item = CatalogItem("series", "category", "Catalog title", 0, null, null, null, null),
    )

    private fun details(seasons: List<Int>) = SeriesDetailsCandidate(
        accountId = "account",
        providerSeriesId = "series",
        summary = SeriesSummary("Safe series", null, null, null, null, null, null, null, null),
        seasons = seasons.map { number ->
            SeriesSeason("account", "series", number, "Season $number", null, null, null, number, 1)
        },
        episodes = seasons.map { number ->
            SeriesEpisode(
                "account", "series", "episode-$number", number, 1,
                if (number == 0) "Special episode" else "Second season episode",
                "mp4", 0, null, null, null, null,
            )
        },
        skippedEntries = 0,
        seasonMismatchCount = 0,
    )
}
