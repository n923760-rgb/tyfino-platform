package dev.tyfino.foundation.playback

import dev.tyfino.foundation.xtream.CatalogSection
import dev.tyfino.foundation.xtream.ProviderEndpoint
import dev.tyfino.foundation.xtream.SavedXtreamAccount
import dev.tyfino.foundation.xtream.XtreamAccountStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieResumeRepositoryTest {
    @Test
    fun liveNeverCreatesResumeData() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 1))
        val store = FakeResumeStore()
        val repository = repository(accountStore, store)

        val result = repository.saveImmediately(
            selection("account-a", 1, CatalogSection.Live),
            positionMillis = 90_000,
            durationMillis = null,
        )

        assertEquals(
            MovieResumeWriteResult.Failure(MovieResumeFailure.InvalidSelection),
            result,
        )
        assertTrue(store.records.isEmpty())
    }

    @Test
    fun belowThirtySecondsIsNotSaved() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 1))
        val store = FakeResumeStore()
        val repository = repository(accountStore, store)

        val result = repository.saveImmediately(
            selection("account-a", 1),
            positionMillis = 29_999,
            durationMillis = 300_000,
        )

        assertEquals(
            MovieResumeWriteResult.Skipped(MovieResumeSkipReason.BelowSaveThreshold),
            result,
        )
        assertTrue(store.records.isEmpty())
    }

    @Test
    fun checkpointsAreLimitedToOncePerTenSeconds() = runBlocking {
        val clock = FakeClock(wall = 100_000, elapsed = 20_000)
        val accountStore = FakeAccountStore(account("account-a", 1))
        val store = FakeResumeStore()
        val repository = MovieResumeRepository(accountStore, store, clock)
        val selection = selection("account-a", 1)

        assertEquals(
            MovieResumeWriteResult.Saved,
            repository.checkpoint(selection, 40_000, 300_000),
        )
        clock.elapsed = 29_999
        assertEquals(
            MovieResumeWriteResult.Skipped(MovieResumeSkipReason.CheckpointTooSoon),
            repository.checkpoint(selection, 50_000, 300_000),
        )
        clock.elapsed = 30_000
        assertEquals(
            MovieResumeWriteResult.Saved,
            repository.checkpoint(selection, 60_000, 300_000),
        )
        assertEquals(60_000, store.records.values.single().positionMillis)
    }

    @Test
    fun completionAtNinetyFivePercentOrUnderOneMinuteRemainingDeletes() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 1))
        val store = FakeResumeStore()
        val repository = repository(accountStore, store)
        val selection = selection("account-a", 1)
        store.records[selection.key()] = record(position = 120_000, duration = 200_000)

        assertEquals(
            MovieResumeWriteResult.Deleted,
            repository.saveImmediately(selection, 190_000, 200_000),
        )
        assertNull(store.records[selection.key()])

        store.records[selection.key()] = record(position = 120_000, duration = 200_000)
        assertEquals(
            MovieResumeWriteResult.Deleted,
            repository.saveImmediately(selection, 141_000, 200_000),
        )
        assertNull(store.records[selection.key()])
    }

    @Test
    fun invalidPositionsAreRejectedWithoutMutation() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 1))
        val store = FakeResumeStore()
        val repository = repository(accountStore, store)
        val selection = selection("account-a", 1)

        assertEquals(
            MovieResumeWriteResult.Failure(MovieResumeFailure.InvalidPosition),
            repository.saveImmediately(selection, -1, 100_000),
        )
        assertEquals(
            MovieResumeWriteResult.Failure(MovieResumeFailure.InvalidPosition),
            repository.saveImmediately(selection, 100_001, 100_000),
        )
        assertTrue(store.records.isEmpty())
    }

    @Test
    fun accountCannotReadOrWriteAnotherAccountsResume() = runBlocking {
        val accountStore = FakeAccountStore(account("account-b", 2))
        val store = FakeResumeStore().apply {
            records["account-a" to "movie-1"] = record(accountId = "account-a")
        }
        val repository = repository(accountStore, store)
        val staleSelection = selection("account-a", 1)

        assertEquals(
            MovieResumeLoadResult.Failure(MovieResumeFailure.StaleOwner),
            repository.load(staleSelection),
        )
        assertEquals(
            MovieResumeWriteResult.Failure(MovieResumeFailure.StaleOwner),
            repository.saveImmediately(staleSelection, 60_000, 300_000),
        )
        assertEquals(1, store.records.size)
    }

    @Test
    fun accountReplacementDuringLoadRejectsTheCompletion() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 1))
        val store = FakeResumeStore().apply {
            records["account-a" to "movie-1"] = record()
            onLoad = { accountStore.value = account("account-b", 1) }
        }
        val repository = repository(accountStore, store)

        assertEquals(
            MovieResumeLoadResult.Failure(MovieResumeFailure.StaleOwner),
            repository.load(selection("account-a", 1)),
        )
    }

    @Test
    fun continueWatchingIsAccountScopedRecentAndAtLeastOneMinute() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 1))
        val store = FakeResumeStore().apply {
            records["account-a" to "movie-short"] = record(
                providerId = "movie-short",
                position = 59_999,
                updatedAt = 90_000,
            )
            records["account-a" to "movie-ready"] = record(
                providerId = "movie-ready",
                position = 60_000,
                updatedAt = 95_000,
            )
            records["account-b" to "movie-other"] = record(
                accountId = "account-b",
                providerId = "movie-other",
                updatedAt = 99_000,
            )
        }

        val result = repository(accountStore, store).continueWatching()

        result as MovieResumeListResult.Ready
        assertEquals(listOf("movie-ready"), result.records.map { it.providerItemId })
    }

    @Test
    fun futurePersistedTimestampIsRejected() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 1))
        val store = FakeResumeStore().apply {
            records["account-a" to "movie-1"] = record(updatedAt = 100_001)
        }

        assertEquals(
            MovieResumeLoadResult.Failure(MovieResumeFailure.InvalidRecord),
            repository(accountStore, store).load(selection("account-a", 1)),
        )
    }

    @Test
    fun clearingActiveAccountRemovesOnlyItsRecords() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 1))
        val store = FakeResumeStore().apply {
            records["account-a" to "movie-1"] = record()
            records["account-b" to "movie-1"] = record(accountId = "account-b")
        }

        assertTrue(repository(accountStore, store).clearActiveAccount())

        assertNull(store.records["account-a" to "movie-1"])
        assertTrue(store.records.containsKey("account-b" to "movie-1"))
    }

    private fun repository(
        accountStore: FakeAccountStore,
        store: FakeResumeStore,
    ) = MovieResumeRepository(accountStore, store, FakeClock(100_000, 20_000))

    private fun selection(
        accountId: String,
        generation: Long,
        section: CatalogSection = CatalogSection.Movies,
        providerId: String = "movie-1",
    ) = PlaybackSelection(accountId, generation, section, providerId, "mp4")

    private fun PlaybackSelection.key() = accountId to providerItemId

    private fun record(
        accountId: String = "account-a",
        providerId: String = "movie-1",
        position: Long = 60_000,
        duration: Long? = 300_000,
        updatedAt: Long = 90_000,
    ) = MovieResumeRecord(accountId, providerId, position, duration, updatedAt)

    private fun account(id: String, generation: Long) = SavedXtreamAccount(
        accountId = id,
        generation = generation,
        endpoint = ProviderEndpoint("https://provider.example", false),
        username = "user",
        password = "password",
        cleartextConsent = false,
    )

    private class FakeClock(var wall: Long, var elapsed: Long) : MovieResumeClock {
        override fun wallTimeMillis() = wall
        override fun elapsedTimeMillis() = elapsed
    }

    private class FakeAccountStore(var value: SavedXtreamAccount?) : XtreamAccountStore {
        override fun load() = value
        override fun save(account: SavedXtreamAccount) {
            value = account
        }
        override fun clear() {
            value = null
        }
    }

    private class FakeResumeStore : MovieResumeStore {
        val records = linkedMapOf<Pair<String, String>, MovieResumeRecord>()
        var onLoad: () -> Unit = {}

        override fun load(accountId: String, providerItemId: String): MovieResumeRecord? {
            val result = records[accountId to providerItemId]
            onLoad()
            return result
        }

        override fun list(accountId: String, limit: Int): List<MovieResumeRecord> =
            records.values
                .filter { it.accountId == accountId }
                .sortedByDescending(MovieResumeRecord::updatedAtEpochMillis)
                .take(limit)

        override fun upsert(record: MovieResumeRecord) {
            records[record.accountId to record.providerItemId] = record
        }

        override fun delete(accountId: String, providerItemId: String) {
            records.remove(accountId to providerItemId)
        }

        override fun clearAccount(accountId: String) {
            records.keys.removeAll { it.first == accountId }
        }
    }
}
