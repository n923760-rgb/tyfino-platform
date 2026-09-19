package dev.tyfino.foundation.playback

import dev.tyfino.foundation.xtream.ProviderEndpoint
import dev.tyfino.foundation.xtream.SavedXtreamAccount
import dev.tyfino.foundation.xtream.SeriesDetailsCandidate
import dev.tyfino.foundation.xtream.SeriesDetailsRepository
import dev.tyfino.foundation.xtream.SeriesDetailsResult
import dev.tyfino.foundation.xtream.SeriesEpisode
import dev.tyfino.foundation.xtream.SeriesFailure
import dev.tyfino.foundation.xtream.SeriesSnapshot
import dev.tyfino.foundation.xtream.SeriesStore
import dev.tyfino.foundation.xtream.SeriesSummary
import dev.tyfino.foundation.xtream.XtreamAccountStore
import dev.tyfino.foundation.xtream.XtreamSeriesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeHistoryRepositoryTest {
    @Test
    fun recordsOnlyCurrentPublishedEpisodeAndReturnsCurrentMetadata() = runBlocking {
        val fixture = Fixture()

        assertTrue(fixture.history.recordStarted(fixture.selection()))
        val result = fixture.history.recent() as EpisodeHistoryListResult.Ready

        assertEquals(1, result.items.size)
        assertEquals("Episode", result.items.single().episode.title)
        assertEquals(1L, result.items.single().seriesGeneration)
        assertEquals(100_000L, result.items.single().startedAtEpochMillis)
    }

    @Test
    fun staleAccountAndSeriesGenerationCannotWrite() = runBlocking {
        val fixture = Fixture()
        fixture.series.snapshots["a" to "series"] = fixture.snapshot(generation = 2)
        assertFalse(fixture.history.recordStarted(fixture.selection()))
        fixture.series.snapshots["a" to "series"] = fixture.snapshot()
        fixture.accounts.value = fixture.account("b", 1)
        assertFalse(fixture.history.recordStarted(fixture.selection()))
        assertTrue(fixture.store.records.isEmpty())
    }

    @Test
    fun orphanIsHiddenAndClearTargetsOneAccount() = runBlocking {
        val fixture = Fixture()
        fixture.store.records[Triple("a", "missing", "episode")] =
            EpisodeHistoryRecord("a", "missing", "episode", 90_000)
        fixture.store.records[Triple("b", "series", "episode")] =
            EpisodeHistoryRecord("b", "series", "episode", 90_000)

        assertTrue((fixture.history.recent() as EpisodeHistoryListResult.Ready).items.isEmpty())
        assertTrue(fixture.history.clearAccount("a"))
        assertTrue(fixture.store.records.keys.none { it.first == "a" })
        assertTrue(fixture.store.records.keys.any { it.first == "b" })
    }

    @Test
    fun invalidFutureTimestampFailsClosed() = runBlocking {
        val fixture = Fixture()
        fixture.store.records[Triple("a", "series", "episode")] =
            EpisodeHistoryRecord("a", "series", "episode", 100_001)
        assertEquals(EpisodeHistoryListResult.Failure, fixture.history.recent())
    }

    private class Fixture {
        val accounts = FakeAccountStore(account("a", 1))
        val series = FakeSeriesStore()
        val store = FakeHistoryStore()
        val details = SeriesDetailsRepository(
            accounts,
            object : XtreamSeriesApi {
                override suspend fun details(account: SavedXtreamAccount, seriesId: String) =
                    SeriesDetailsResult.Failure(SeriesFailure.NetworkUnavailable)
            },
            series,
        )
        val history = EpisodeHistoryRepository(accounts, store, series, details, FixedClock())

        init { series.snapshots["a" to "series"] = snapshot() }

        fun account(id: String, generation: Long) = SavedXtreamAccount(
            id, generation, ProviderEndpoint("https://provider.example", false), "user", "password", false,
        )

        fun snapshot(generation: Long = 1) = SeriesSnapshot(
            generation,
            90_000,
            SeriesDetailsCandidate(
                "a", "series", SeriesSummary(name = "Series", plot = null, genre = null, releaseDate = null,
                    rating = null, cast = null, director = null, coverUrl = null, backdropUrl = null),
                emptyList(),
                listOf(SeriesEpisode("a", "series", "episode", 1, 1, "Episode", "mp4", 0,
                    null, null, null, null)),
                0, 0,
            ),
        )

        fun selection() = EpisodePlaybackSelection.from(
            "a", 1, "series", 1,
            series.snapshots.getValue("a" to "series").details.episodes.single(),
        )!!
    }

    private class FixedClock : MovieResumeClock {
        override fun wallTimeMillis() = 100_000L
        override fun elapsedTimeMillis() = 100_000L
    }

    private class FakeAccountStore(var value: SavedXtreamAccount?) : XtreamAccountStore {
        override fun load() = value
        override fun save(account: SavedXtreamAccount) { value = account }
        override fun clear() { value = null }
    }

    private class FakeSeriesStore : SeriesStore {
        val snapshots = mutableMapOf<Pair<String, String>, SeriesSnapshot>()
        override fun load(accountId: String, seriesId: String) = snapshots[accountId to seriesId]
        override fun replace(accountId: String, seriesId: String, snapshot: SeriesSnapshot) {
            snapshots[accountId to seriesId] = snapshot
        }
        override fun clearAccount(accountId: String) { snapshots.keys.removeAll { it.first == accountId } }
        override fun clearOtherAccounts(accountId: String) { snapshots.keys.removeAll { it.first != accountId } }
    }

    private class FakeHistoryStore : EpisodeHistoryStore {
        val records = mutableMapOf<Triple<String, String, String>, EpisodeHistoryRecord>()
        override fun list(accountId: String, limit: Int) = records.values.filter { it.accountId == accountId }
            .sortedByDescending(EpisodeHistoryRecord::startedAtEpochMillis).take(limit)
        override fun upsert(record: EpisodeHistoryRecord) {
            records[Triple(record.accountId, record.providerSeriesId, record.providerEpisodeId)] = record
        }
        override fun clearAccount(accountId: String) { records.keys.removeAll { it.first == accountId } }
    }
}
