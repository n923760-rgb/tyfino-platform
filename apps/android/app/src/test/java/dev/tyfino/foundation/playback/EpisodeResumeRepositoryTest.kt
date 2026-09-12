package dev.tyfino.foundation.playback

import dev.tyfino.foundation.xtream.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeResumeRepositoryTest {
    @Test fun thresholdsCompletionAndNamespace() = runBlocking {
        val fixture = Fixture()
        val selection = fixture.selection()
        assertFalse(fixture.resume.saveImmediately(selection, 29_999, 600_000))
        assertTrue(fixture.store.records.isEmpty())
        assertTrue(fixture.resume.saveImmediately(selection, 60_000, 600_000))
        assertEquals(60_000L, (fixture.resume.load(selection) as EpisodeResumeLoadResult.Ready).record?.positionMillis)
        assertEquals(1, (fixture.resume.continueWatching() as EpisodeResumeListResult.Ready).items.size)
        assertTrue(fixture.resume.saveImmediately(selection, 570_000, 600_000))
        assertNull((fixture.resume.load(selection) as EpisodeResumeLoadResult.Ready).record)
        assertTrue((fixture.resume.continueWatching() as EpisodeResumeListResult.Ready).items.isEmpty())
    }

    @Test fun checkpointAndInvalidPositions() = runBlocking {
        val fixture = Fixture()
        val selection = fixture.selection()
        assertFalse(fixture.resume.saveImmediately(selection, -1, 600_000))
        assertFalse(fixture.resume.saveImmediately(selection, 600_001, 600_000))
        assertTrue(fixture.resume.checkpoint(selection, 60_000, 600_000))
        fixture.clock.elapsed = 29_999
        assertFalse(fixture.resume.checkpoint(selection, 70_000, 600_000))
        fixture.clock.elapsed = 30_000
        assertTrue(fixture.resume.checkpoint(selection, 80_000, 600_000))
        assertEquals(80_000L, fixture.store.records.values.single().positionMillis)
    }

    @Test fun staleAccountAndSeriesGenerationCannotWrite() = runBlocking {
        val fixture = Fixture()
        val selection = fixture.selection()
        fixture.series.snapshots["a" to "series"] = fixture.snapshot(generation = 2)
        assertFalse(fixture.resume.saveImmediately(selection, 60_000, 600_000))
        fixture.series.snapshots["a" to "series"] = fixture.snapshot(generation = 1)
        fixture.accounts.value = fixture.account("b", 1)
        assertFalse(fixture.resume.saveImmediately(selection, 60_000, 600_000))
        assertTrue(fixture.store.records.isEmpty())
    }

    @Test fun orphanedRecordHiddenAndAccountsIsolated() = runBlocking {
        val fixture = Fixture()
        val selection = fixture.selection()
        assertTrue(fixture.resume.saveImmediately(selection, 60_000, 600_000))
        fixture.store.records[Triple("a", "missing", "episode")] = EpisodeResumeRecord("a", "missing", "episode", 70_000, 600_000, 90_000)
        assertEquals(1, (fixture.resume.continueWatching() as EpisodeResumeListResult.Ready).items.size)
        fixture.accounts.value = fixture.account("b", 1)
        assertTrue((fixture.resume.continueWatching() as EpisodeResumeListResult.Ready).items.isEmpty())
        assertTrue(fixture.store.records.isEmpty())
    }

    @Test fun futureTimestampIsRejectedAndAccountClearIsScoped() = runBlocking {
        val fixture = Fixture()
        val selection = fixture.selection()
        val key = Triple("a", "series", "episode")
        fixture.store.records[key] = EpisodeResumeRecord("a", "series", "episode", 60_000, 600_000, 100_001)
        assertEquals(EpisodeResumeLoadResult.Failure, fixture.resume.load(selection))
        fixture.store.records[key] = fixture.store.records.getValue(key).copy(updatedAtEpochMillis = 90_000)
        fixture.store.records[Triple("b", "series", "episode")] =
            EpisodeResumeRecord("b", "series", "episode", 70_000, 600_000, 90_000)
        assertTrue(fixture.resume.clearActiveAccount())
        assertNull(fixture.store.records[key])
        assertTrue(fixture.store.records.containsKey(Triple("b", "series", "episode")))
    }

    private class Fixture {
        val accounts = FakeAccountStore(account("a", 1))
        val series = FakeSeriesStore()
        val store = FakeEpisodeStore()
        val clock = FakeClock()
        val details = SeriesDetailsRepository(accounts, object : XtreamSeriesApi {
            override suspend fun details(account: SavedXtreamAccount, seriesId: String) =
                SeriesDetailsResult.Failure(SeriesFailure.NetworkUnavailable)
        }, series)
        val resume = EpisodeResumeRepository(accounts, store, series, details, clock)
        init { series.snapshots["a" to "series"] = snapshot() }

        fun account(id: String, generation: Long) = SavedXtreamAccount(id, generation,
            ProviderEndpoint("https://provider.example", false), "user", "password", false)
        fun snapshot(generation: Long = 1): SeriesSnapshot = SeriesSnapshot(generation, 90_000,
            SeriesDetailsCandidate("a", "series", SeriesSummary.EMPTY, emptyList(),
                listOf(SeriesEpisode("a", "series", "episode", 1, 1, "Episode", "mp4", 0,
                    null, null, null, null)), 0, 0))
        fun selection() = EpisodePlaybackSelection.from("a", 1, "series", 1,
            series.snapshots.getValue("a" to "series").details.episodes.single())!!
    }

    private class FakeClock(var elapsed: Long = 20_000) : MovieResumeClock {
        override fun wallTimeMillis() = 100_000L
        override fun elapsedTimeMillis() = elapsed
    }
    private class FakeAccountStore(var value: SavedXtreamAccount?) : XtreamAccountStore {
        override fun load() = value
        override fun save(account: SavedXtreamAccount) { value = account }
        override fun clear() { value = null }
    }
    private class FakeSeriesStore : SeriesStore {
        val snapshots = mutableMapOf<Pair<String, String>, SeriesSnapshot>()
        override fun load(accountId: String, seriesId: String) = snapshots[accountId to seriesId]
        override fun replace(accountId: String, seriesId: String, snapshot: SeriesSnapshot) { snapshots[accountId to seriesId] = snapshot }
        override fun clearAccount(accountId: String) { snapshots.keys.removeAll { it.first == accountId } }
        override fun clearOtherAccounts(accountId: String) { snapshots.keys.removeAll { it.first != accountId } }
    }
    private class FakeEpisodeStore : EpisodeResumeStore {
        val records = mutableMapOf<Triple<String, String, String>, EpisodeResumeRecord>()
        override fun load(accountId: String, seriesId: String, episodeId: String) = records[Triple(accountId, seriesId, episodeId)]
        override fun list(accountId: String, limit: Int) = records.values.filter { it.accountId == accountId }
            .sortedByDescending { it.updatedAtEpochMillis }.take(limit)
        override fun upsert(record: EpisodeResumeRecord) { records[Triple(record.accountId, record.providerSeriesId, record.providerEpisodeId)] = record }
        override fun delete(accountId: String, seriesId: String, episodeId: String) { records.remove(Triple(accountId, seriesId, episodeId)) }
        override fun clearAccount(accountId: String) { records.keys.removeAll { it.first == accountId } }
        override fun clearOtherAccounts(accountId: String) { records.keys.removeAll { it.first != accountId } }
    }
}
