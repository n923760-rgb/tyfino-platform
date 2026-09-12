package dev.tyfino.foundation.xtream

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeriesDetailsRepositoryTest {
    @Test
    fun freshCachePublishesWithoutNetworkWork() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 4))
        val store = FakeSeriesStore().apply {
            snapshots["account-a" to "series"] = snapshot("account-a", "series", 3, 9_000, "Cached")
        }
        val api = FixedSeriesApi("Network")
        val repository = repository(accountStore, api, store, wall = 10_000)
        val destination = repository.open("series")!!
        val states = mutableListOf<SeriesState>()

        repository.details(destination, publish = states::add)

        assertEquals(0, api.calls)
        assertEquals("Cached", (states.single() as SeriesState.Content).details.summary.name)
    }

    @Test
    fun explicitRefreshPublishesCacheThenAtomicallyReplacesGeneration() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 4))
        val store = FakeSeriesStore().apply {
            snapshots["account-a" to "series"] = snapshot("account-a", "series", 7, 9_000, "Old")
        }
        val repository = repository(accountStore, FixedSeriesApi("New"), store, wall = 10_000)
        val destination = repository.open("series")!!
        val states = mutableListOf<SeriesState>()

        repository.details(destination, forceRefresh = true, publish = states::add)

        assertTrue((states.first() as SeriesState.Content).isRefreshing)
        assertEquals("New", (states.last() as SeriesState.Content).details.summary.name)
        assertEquals(8, store.snapshots.getValue("account-a" to "series").generation)
    }

    @Test
    fun failedRefreshKeepsPreviousSnapshotVisible() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 4))
        val store = FakeSeriesStore().apply {
            snapshots["account-a" to "series"] = snapshot("account-a", "series", 2, 1_000, "Cached")
        }
        val repository = repository(
            accountStore,
            FixedResultSeriesApi(SeriesDetailsResult.Failure(SeriesFailure.Timeout)),
            store,
            wall = 100_000_000,
        )
        val states = mutableListOf<SeriesState>()

        repository.details(repository.open("series")!!, publish = states::add)

        val stale = states.last() as SeriesState.StaleContent
        assertEquals("Cached", stale.details.summary.name)
        assertEquals(SeriesFailure.Timeout, stale.failure)
        assertEquals(2, store.snapshots.getValue("account-a" to "series").generation)
    }

    @Test
    fun newerRefreshCommitsAndOlderCompletionIsDiscarded() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 4))
        val store = FakeSeriesStore()
        val api = ControllableSeriesApi()
        val repository = repository(accountStore, api, store, wall = 20_000)
        val destination = repository.open("series")!!
        val oldStates = mutableListOf<SeriesState>()
        val newStates = mutableListOf<SeriesState>()

        val old = async { repository.details(destination, true, oldStates::add) }
        api.firstStarted.await()
        repository.details(destination, true, newStates::add)
        api.releaseFirst.complete(Unit)
        old.await()

        assertEquals("New", store.snapshots.getValue("account-a" to "series").details.summary.name)
        assertTrue(oldStates.none { it is SeriesState.Content })
    }

    @Test
    fun accountReplacementDuringNetworkPreventsCommitAndPublication() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 4))
        val store = FakeSeriesStore()
        val api = BlockingSeriesApi("Old account data")
        val repository = repository(accountStore, api, store, wall = 20_000)
        val destination = repository.open("series")!!
        val states = mutableListOf<SeriesState>()

        val pending = async { repository.details(destination, true, states::add) }
        api.started.await()
        accountStore.value = account("account-b", 1)
        api.release.complete(Unit)
        pending.await()

        assertNull(store.snapshots["account-a" to "series"])
        assertNull(store.snapshots["account-b" to "series"])
        assertEquals(listOf(SeriesState.Loading), states)
    }

    @Test
    fun credentialGenerationChangeWithinSameAccountRejectsPendingCommit() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 4))
        val store = FakeSeriesStore()
        val api = BlockingSeriesApi("Old credentials")
        val repository = repository(accountStore, api, store, wall = 20_000)
        val destination = repository.open("series")!!
        val states = mutableListOf<SeriesState>()

        val pending = async { repository.details(destination, true, states::add) }
        api.started.await()
        accountStore.value = account("account-a", 5)
        api.release.complete(Unit)
        pending.await()

        assertTrue(store.snapshots.isEmpty())
        assertEquals(listOf(SeriesState.Loading), states)
    }

    @Test
    fun accountReplacementRemovesOldRowsBeforeOpeningNewDestination() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 4))
        val store = FakeSeriesStore().apply {
            snapshots["account-a" to "series"] = snapshot("account-a", "series", 1, 9_000, "A")
        }
        val repository = repository(accountStore, FixedSeriesApi("B"), store, wall = 10_000)
        repository.open("series")!!

        accountStore.value = account("account-b", 1)
        repository.open("series")!!

        assertNull(store.snapshots["account-a" to "series"])
    }

    @Test
    fun credentialGenerationReplacementRemovesOldSnapshotBeforePublication() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 4))
        val store = FakeSeriesStore().apply {
            snapshots["account-a" to "series"] = snapshot("account-a", "series", 1, 9_000, "A")
        }
        val repository = repository(accountStore, FixedSeriesApi("Updated"), store, wall = 10_000)
        repository.open("series")!!
        accountStore.value = account("account-a", 5)
        val destination = repository.open("series")!!

        assertNull(store.snapshots["account-a" to "series"])
        val states = mutableListOf<SeriesState>()
        repository.details(destination, publish = states::add)
        assertEquals(SeriesState.Loading, states.first())
    }

    @Test
    fun closingDestinationRejectsPendingCommitAndPublication() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 4))
        val store = FakeSeriesStore()
        val api = BlockingSeriesApi("Closed")
        val repository = repository(accountStore, api, store, wall = 20_000)
        val destination = repository.open("series")!!
        val states = mutableListOf<SeriesState>()

        val pending = async { repository.details(destination, true, states::add) }
        api.started.await()
        repository.close(destination)
        api.release.complete(Unit)
        pending.await()

        assertTrue(store.snapshots.isEmpty())
        assertEquals(listOf(SeriesState.Loading), states)
    }

    @Test
    fun futureTimestampIsStaleAndCannotSuppressRefresh() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 4))
        val store = FakeSeriesStore().apply {
            snapshots["account-a" to "series"] = snapshot("account-a", "series", 1, 20_000, "Future")
        }
        val api = FixedSeriesApi("Corrected")
        val repository = repository(accountStore, api, store, wall = 10_000)

        repository.details(repository.open("series")!!, publish = {})

        assertEquals(1, api.calls)
        assertEquals("Corrected", store.snapshots.getValue("account-a" to "series").details.summary.name)
    }

    @Test
    fun mismatchedCandidateOwnerNeverReplacesCache() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 4))
        val store = FakeSeriesStore()
        val api = FixedResultSeriesApi(
            SeriesDetailsResult.Success(candidate("account-b", "series", "Wrong owner")),
        )
        val repository = repository(accountStore, api, store, wall = 10_000)
        val states = mutableListOf<SeriesState>()

        repository.details(repository.open("series")!!, publish = states::add)

        assertTrue(store.snapshots.isEmpty())
        assertEquals(SeriesFailure.UnsupportedResponse, (states.last() as SeriesState.Error).failure)
    }

    @Test
    fun credentialBearingMetadataIsNotStoredOrPublished() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 4))
        val store = FakeSeriesStore()
        val unsafe = candidate("account-a", "series", "credential-password")
        val repository = repository(accountStore, FixedResultSeriesApi(SeriesDetailsResult.Success(unsafe)), store, 10_000)
        val states = mutableListOf<SeriesState>()

        repository.details(repository.open("series")!!, publish = states::add)

        assertTrue(store.snapshots.isEmpty())
        assertEquals(SeriesFailure.UnsupportedResponse, (states.last() as SeriesState.Error).failure)
    }

    @Test
    fun validEmptyEpisodeGenerationUsesEmptyContent() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 4))
        val store = FakeSeriesStore()
        val empty = candidate("account-a", "series", "Empty").copy(seasons = emptyList(), episodes = emptyList())
        val repository = repository(accountStore, FixedResultSeriesApi(SeriesDetailsResult.Success(empty)), store, 10_000)
        val states = mutableListOf<SeriesState>()

        repository.details(repository.open("series")!!, publish = states::add)

        assertTrue(states.last() is SeriesState.EmptyContent)
        assertTrue(store.snapshots.getValue("account-a" to "series").details.episodes.isEmpty())
    }

    @Test
    fun storageFailurePreservesOldGenerationAndReturnsSafeState() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 4))
        val store = FakeSeriesStore().apply {
            snapshots["account-a" to "series"] = snapshot("account-a", "series", 5, 1_000, "Cached")
            failReplace = true
        }
        val repository = repository(accountStore, FixedSeriesApi("Rejected write"), store, wall = 100_000_000)
        val states = mutableListOf<SeriesState>()

        repository.details(repository.open("series")!!, publish = states::add)

        val stale = states.last() as SeriesState.StaleContent
        assertEquals(SeriesFailure.LocalStorage, stale.failure)
        assertEquals("Cached", store.snapshots.getValue("account-a" to "series").details.summary.name)
        assertEquals(5, store.snapshots.getValue("account-a" to "series").generation)
    }

    @Test
    fun clearActiveAccountInvalidatesDestinationBeforeDeletingOnlyItsRows() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 4))
        val store = FakeSeriesStore().apply {
            snapshots["account-a" to "series"] = snapshot("account-a", "series", 1, 1_000, "A")
        }
        val repository = repository(accountStore, FixedSeriesApi("Unused"), store, wall = 10_000)
        val destination = repository.open("series")!!
        store.snapshots["account-b" to "series"] = snapshot("account-b", "series", 1, 1_000, "B")

        repository.clearActiveAccount()
        val states = mutableListOf<SeriesState>()
        repository.details(destination, publish = states::add)

        assertNull(store.snapshots["account-a" to "series"])
        assertEquals("B", store.snapshots.getValue("account-b" to "series").details.summary.name)
        assertTrue(states.isEmpty())
    }

    @Test
    fun blankAndOversizedSeriesIdsNeverCreateDestination() = runBlocking {
        val repository = repository(
            FakeAccountStore(account("account-a", 4)),
            FixedSeriesApi("Unused"),
            FakeSeriesStore(),
            wall = 10_000,
        )

        assertNull(repository.open(" "))
        assertNull(repository.open("x".repeat(257)))
    }

    @Test
    fun episodeCommitRequiresCurrentAccountSeriesGenerationAndExactEpisodeMetadata() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 4))
        val store = FakeSeriesStore().apply {
            snapshots["account-a" to "series"] = snapshot("account-a", "series", 3, 9_000, "A")
        }
        val repository = repository(accountStore, FixedSeriesApi("Unused"), store, wall = 10_000)
        repository.open("series")!!
        var published = 0

        suspend fun attempt(seriesGeneration: Long, episodeId: String = "episode", extension: String = "mp4") =
            repository.commitIfEpisodeCurrent(
                "account-a", 4, "series", seriesGeneration, episodeId, extension,
            ) { published++ }

        assertTrue(attempt(3))
        assertEquals(1, published)
        assertTrue(!attempt(3, episodeId = "series"))
        assertTrue(!attempt(3, extension = "mp4/path"))
        store.snapshots["account-a" to "series"] = snapshot("account-a", "series", 4, 10_000, "B")
        assertTrue(!attempt(3))
        accountStore.value = account("account-a", 5)
        assertTrue(!attempt(4))
        assertEquals(1, published)
    }

    private fun repository(
        accountStore: FakeAccountStore,
        api: XtreamSeriesApi,
        store: FakeSeriesStore,
        wall: Long,
    ) = SeriesDetailsRepository(accountStore, api, store, FakeClock(wall, 5_000))

    private fun account(id: String, generation: Long) = SavedXtreamAccount(
        accountId = id,
        generation = generation,
        endpoint = ProviderEndpoint("https://provider.example", false),
        username = "credential-user",
        password = "credential-password",
        cleartextConsent = false,
    )

    private fun snapshot(
        accountId: String,
        seriesId: String,
        generation: Long,
        refreshedAt: Long,
        name: String,
    ) = SeriesSnapshot(generation, refreshedAt, candidate(accountId, seriesId, name))

    private class FakeClock(private val wall: Long, private val elapsed: Long) : CatalogClock {
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

    private class FakeSeriesStore : SeriesStore {
        val snapshots = mutableMapOf<Pair<String, String>, SeriesSnapshot>()
        var failReplace = false

        override fun load(accountId: String, seriesId: String) = snapshots[accountId to seriesId]

        override fun replace(accountId: String, seriesId: String, snapshot: SeriesSnapshot) {
            if (failReplace) error("storage unavailable")
            snapshots[accountId to seriesId] = snapshot
        }

        override fun clearAccount(accountId: String) {
            snapshots.keys.removeAll { it.first == accountId }
        }

        override fun clearOtherAccounts(accountId: String) {
            snapshots.keys.removeAll { it.first != accountId }
        }
    }

    private open class FixedSeriesApi(private val name: String) : XtreamSeriesApi {
        var calls = 0
        override suspend fun details(account: SavedXtreamAccount, seriesId: String): SeriesDetailsResult {
            calls++
            return SeriesDetailsResult.Success(candidate(account.accountId, seriesId, name))
        }
    }

    private class FixedResultSeriesApi(private val result: SeriesDetailsResult) : XtreamSeriesApi {
        override suspend fun details(account: SavedXtreamAccount, seriesId: String) = result
    }

    private class BlockingSeriesApi(private val name: String) : XtreamSeriesApi {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        override suspend fun details(account: SavedXtreamAccount, seriesId: String): SeriesDetailsResult {
            started.complete(Unit)
            release.await()
            return SeriesDetailsResult.Success(candidate(account.accountId, seriesId, name))
        }
    }

    private class ControllableSeriesApi : XtreamSeriesApi {
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        private var calls = 0

        override suspend fun details(account: SavedXtreamAccount, seriesId: String): SeriesDetailsResult {
            calls++
            if (calls == 1) {
                firstStarted.complete(Unit)
                releaseFirst.await()
                return SeriesDetailsResult.Success(candidate(account.accountId, seriesId, "Old"))
            }
            return SeriesDetailsResult.Success(candidate(account.accountId, seriesId, "New"))
        }
    }

    private companion object {
        fun candidate(accountId: String, seriesId: String, name: String) = SeriesDetailsCandidate(
            accountId = accountId,
            providerSeriesId = seriesId,
            summary = SeriesSummary(name, null, null, null, null, null, null, null, null),
            seasons = listOf(
                SeriesSeason(accountId, seriesId, 1, "1", null, null, null, 0, 1),
            ),
            episodes = listOf(
                SeriesEpisode(
                    accountId,
                    seriesId,
                    "episode",
                    1,
                    1,
                    "Episode",
                    "mp4",
                    0,
                    null,
                    null,
                    null,
                    null,
                ),
            ),
            skippedEntries = 0,
            seasonMismatchCount = 0,
        )
    }
}
