package dev.tyfino.foundation.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.tyfino.foundation.playback.SeriesHistoryItem
import dev.tyfino.foundation.xtream.SeriesEpisode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SeriesHistoryStripTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun historyIsSeparateAndEpisodeCanBePlayedAgain() {
        var selected = 0
        compose.setContent {
            MaterialTheme {
                SeriesHistoryStrip(listOf(item())) { selected++ }
            }
        }

        compose.onNodeWithTag("series-history").assertExists()
        compose.onNodeWithText("Recently watched episodes").assertExists()
        compose.onNodeWithText("Series • Episode").performClick()
        compose.onNodeWithText("Play episode again").assertExists()
        compose.runOnIdle { assertEquals(1, selected) }
    }

    private fun item() = SeriesHistoryItem(
        episode = SeriesEpisode(
            "account", "series", "episode", 1, 1, "Episode", "mp4", 0,
            null, null, null, null,
        ),
        seriesTitle = "Series",
        seriesGeneration = 1,
        accountGeneration = 1,
        startedAtEpochMillis = 100_000,
    )
}
