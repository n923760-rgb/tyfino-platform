package dev.tyfino.foundation.ui.screen

import dev.tyfino.foundation.xtream.SeriesDetailsCandidate
import dev.tyfino.foundation.xtream.SeriesEpisode
import dev.tyfino.foundation.xtream.SeriesSeason
import dev.tyfino.foundation.xtream.SeriesSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeriesPresentationTest {
    @Test
    fun stableSeasonSelectionSurvivesRefreshAndFallsBackWhenRemoved() {
        val initial = details(seasons = listOf(0, 1, 2))
        assertEquals(2, SeriesPresentation.selectedSeason(initial, 2))

        val refreshed = details(seasons = listOf(0, 1, 2), name = "Updated")
        assertEquals(2, SeriesPresentation.selectedSeason(refreshed, 2))
        assertEquals(0, SeriesPresentation.selectedSeason(details(seasons = listOf(0, 1)), 2))
        assertEquals(null, SeriesPresentation.selectedSeason(details(seasons = emptyList()), 2))
    }

    @Test
    fun episodeFilteringPreservesProviderOrderWithoutReversingSeasonNumbers() {
        val candidate = details(seasons = listOf(0, 2)).copy(
            episodes = listOf(episode("one", 0), episode("two", 2), episode("three", 2)),
        )
        assertEquals(listOf("two", "three"), SeriesPresentation.episodesForSeason(candidate, 2).map { it.providerEpisodeId })
        assertEquals(emptyList<SeriesEpisode>(), SeriesPresentation.episodesForSeason(candidate, null))
    }

    @Test
    fun safeDisplayFallbackAndPlaybackMetadataDoNotTreatSeriesIdAsEpisodeId() {
        val candidate = details(seasons = listOf(1), name = null)
        assertEquals("Catalog title", SeriesPresentation.displayName(candidate, "Catalog title"))
        assertTrue(SeriesPresentation.playableMetadataAvailable(episode("episode", 1)))
        assertFalse(SeriesPresentation.playableMetadataAvailable(episode("episode", 1).copy(containerExtension = null)))
        assertFalse(SeriesPresentation.playableMetadataAvailable(episode("episode", 1).copy(containerExtension = "mp4/path")))
    }

    private fun details(seasons: List<Int>, name: String? = "Series") = SeriesDetailsCandidate(
        accountId = "account",
        providerSeriesId = "series",
        summary = SeriesSummary(name, null, null, null, null, null, null, null, null),
        seasons = seasons.map { season ->
            SeriesSeason("account", "series", season, "Season $season", null, null, null, season, 1)
        },
        episodes = emptyList(),
        skippedEntries = 0,
        seasonMismatchCount = 0,
    )

    private fun episode(id: String, season: Int) = SeriesEpisode(
        "account", "series", id, season, 1, "Episode", "mp4", 0, null, null, null, null,
    )
}
