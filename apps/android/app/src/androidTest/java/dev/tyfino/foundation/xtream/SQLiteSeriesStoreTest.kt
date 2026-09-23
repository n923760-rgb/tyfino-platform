package dev.tyfino.foundation.xtream

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SQLiteSeriesStoreTest {
    @Test
    fun latestEpisodesAreDatedScopedToCurrentAccountAndPublishedGeneration() {
        val store = SQLiteSeriesStore(InstrumentationRegistry.getInstrumentation().targetContext)
        val owner = UUID.randomUUID().toString()
        val other = UUID.randomUUID().toString()
        val seriesId = UUID.randomUUID().toString()
        try {
            val base = snapshot(owner, seriesId, 1)
            store.replace(owner, seriesId, base.copy(details = base.details.copy(episodes = listOf(
                base.details.episodes.single().copy(releaseDate = "2020-01-01"),
            ))))
            val newer = base.copy(generation = 2, details = base.details.copy(episodes = listOf(
                base.details.episodes.single().copy(providerEpisodeId = "latest", releaseDate = "2024-02-02"),
            )))
            store.replace(owner, seriesId, newer)
            store.replace(other, seriesId, snapshot(other, seriesId, 1).copy(details =
                snapshot(other, seriesId, 1).details.copy(episodes = listOf(
                    snapshot(other, seriesId, 1).details.episodes.single().copy(releaseDate = "2025-01-01"),
                )),
            ))
            val latest = store.latestDatedEpisodes(owner, 7, 8)
            assertEquals(listOf("latest"), latest.map { it.episode.providerEpisodeId })
            assertEquals(2, latest.single().seriesGeneration)
            assertEquals(7, latest.single().accountGeneration)
        } finally {
            store.clearAccount(owner)
            store.clearAccount(other)
        }
    }

    @Test
    fun failedReplacementRollsBackAndAccountCleanupPreservesOtherAccounts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = SQLiteSeriesStore(context)
        val firstAccount = UUID.randomUUID().toString()
        val secondAccount = UUID.randomUUID().toString()
        val seriesId = UUID.randomUUID().toString()
        val first = snapshot(firstAccount, seriesId, generation = 1)
        val second = snapshot(secondAccount, seriesId, generation = 1)
        try {
            store.replace(firstAccount, seriesId, first)
            store.replace(secondAccount, seriesId, second)
            val duplicateSeason = first.copy(
                generation = 2,
                details = first.details.copy(seasons = first.details.seasons + first.details.seasons.single()),
            )

            assertThrows(RuntimeException::class.java) {
                store.replace(firstAccount, seriesId, duplicateSeason)
            }
            assertEquals(first, store.load(firstAccount, seriesId))
            store.clearAccount(firstAccount)
            assertNull(store.load(firstAccount, seriesId))
            assertEquals(second, store.load(secondAccount, seriesId))
            store.replace(firstAccount, seriesId, first)
            store.clearOtherAccounts(secondAccount)
            assertNull(store.load(firstAccount, seriesId))
            assertEquals(second, store.load(secondAccount, seriesId))
        } finally {
            store.clearAccount(firstAccount)
            store.clearAccount(secondAccount)
        }
    }

    private fun snapshot(accountId: String, seriesId: String, generation: Long) = SeriesSnapshot(
        generation = generation,
        refreshedAtEpochMillis = 12_345L,
        details = SeriesDetailsCandidate(
            accountId = accountId,
            providerSeriesId = seriesId,
            summary = SeriesSummary("Series", null, null, null, null, null, null, null, null),
            seasons = listOf(SeriesSeason(accountId, seriesId, 1, "Season 1", null, null, null, 0, 1)),
            episodes = listOf(
                SeriesEpisode(accountId, seriesId, "episode", 1, 1, "Episode", "mp4", 0, null, null, null, null),
            ),
            skippedEntries = 0,
            seasonMismatchCount = 0,
        ),
    )
}
