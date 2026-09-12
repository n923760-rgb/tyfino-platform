package dev.tyfino.foundation.xtream

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogRepositoryTest {
    @Test
    fun freshCachePublishesImmediatelyWithoutNetworkWork() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 4))
        val store = FakeCatalogStore().apply {
            categories["account-a" to CatalogSection.Live] = snapshot(3, 9_000, "Cached")
        }
        val api = FixedCatalogApi(categories = success("Network"))
        val states = mutableListOf<CatalogState<CatalogCategory>>()

        repository(accountStore, api, store, wall = 10_000).categories(CatalogSection.Live, publish = states::add)

        assertEquals(0, api.categoryCalls)
        assertEquals(listOf("Cached"), (states.single() as CatalogState.Content).records.map { it.name })
    }

    @Test
    fun explicitRefreshPublishesCacheThenAtomicallyReplacesGeneration() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 4))
        val store = FakeCatalogStore().apply {
            categories["account-a" to CatalogSection.Movies] = snapshot(7, 9_000, "Old")
        }
        val states = mutableListOf<CatalogState<CatalogCategory>>()

        repository(accountStore, FixedCatalogApi(categories = success("New")), store, wall = 10_000)
            .categories(CatalogSection.Movies, forceRefresh = true, publish = states::add)

        assertTrue((states.first() as CatalogState.Content).isRefreshing)
        assertEquals("New", (states.last() as CatalogState.Content).records.single().name)
        assertEquals(8, store.categories.getValue("account-a" to CatalogSection.Movies).generation)
    }

    @Test
    fun failedRefreshKeepsPreviousSnapshotVisible() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 4))
        val store = FakeCatalogStore().apply {
            categories["account-a" to CatalogSection.Series] = snapshot(2, 1_000, "Cached")
        }
        val states = mutableListOf<CatalogState<CatalogCategory>>()

        repository(
            accountStore,
            FixedCatalogApi(CatalogResult.Failure(CatalogFailure.Timeout)),
            store,
            wall = 100_000_000,
        ).categories(CatalogSection.Series, publish = states::add)

        val stale = states.last() as CatalogState.StaleContent
        assertEquals("Cached", stale.records.single().name)
        assertEquals(CatalogFailure.Timeout, stale.failure)
        assertEquals(2, store.categories.getValue("account-a" to CatalogSection.Series).generation)
    }

    @Test
    fun newerRefreshCommitsAndOlderCompletionIsDiscarded() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 4))
        val store = FakeCatalogStore()
        val api = ControllableCatalogApi()
        val repository = repository(accountStore, api, store, wall = 20_000)
        val oldStates = mutableListOf<CatalogState<CatalogCategory>>()
        val newStates = mutableListOf<CatalogState<CatalogCategory>>()

        val old = async { repository.categories(CatalogSection.Live, true, oldStates::add) }
        api.firstStarted.await()
        repository.categories(CatalogSection.Live, true, newStates::add)
        api.releaseFirst.complete(Unit)
        old.await()

        assertEquals("New", store.categories.getValue("account-a" to CatalogSection.Live).records.single().name)
        assertTrue(oldStates.none { it is CatalogState.Content })
    }

    @Test
    fun accountReplacementDuringNetworkPreventsCommitAndPublication() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 4))
        val store = FakeCatalogStore()
        val api = BlockingCatalogApi(success("Old account data"))
        val states = mutableListOf<CatalogState<CatalogCategory>>()
        val repository = repository(accountStore, api, store, wall = 20_000)

        val pending = async { repository.categories(CatalogSection.Live, true, states::add) }
        api.started.await()
        accountStore.value = account("account-b", 1)
        api.release.complete(Unit)
        pending.await()

        assertNull(store.categories["account-a" to CatalogSection.Live])
        assertNull(store.categories["account-b" to CatalogSection.Live])
        assertEquals(listOf(CatalogState.Loading), states)
    }

    @Test
    fun futurePersistedTimestampIsStaleAndCannotSuppressRefresh() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 4))
        val store = FakeCatalogStore().apply {
            categories["account-a" to CatalogSection.Live] = snapshot(1, 20_000, "Future")
        }
        val api = FixedCatalogApi(categories = success("Corrected"))

        repository(accountStore, api, store, wall = 10_000).categories(CatalogSection.Live) {}

        assertEquals(1, api.categoryCalls)
        assertEquals("Corrected", store.categories.getValue("account-a" to CatalogSection.Live).records.single().name)
    }

    @Test
    fun cachedSearchIsScopedBoundedAndNeverCallsProvider() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 4))
        val store = FakeCatalogStore().apply {
            searchable += CatalogItem("movie", "category", "Movie Match", 0, null, null, null, "mp4")
            searchable += CatalogItem("other", "category", "Another", 1, null, null, null, "mp4")
        }
        val api = FixedCatalogApi(success("Unused"))
        val repository = repository(accountStore, api, store, wall = 10_000)

        assertEquals(CatalogSearchResult.InvalidQuery, repository.searchCached(CatalogSection.Movies, "x"))
        val result = repository.searchCached(CatalogSection.Movies, "match") as CatalogSearchResult.Ready
        assertEquals(listOf("movie"), result.records.map(CatalogItem::providerId))
        assertTrue(!result.limited)
        assertEquals(0, api.categoryCalls)
        assertEquals(listOf(Triple("account-a", CatalogSection.Movies, "match")), store.searchCalls)
    }

    @Test
    fun cachedSearchDiscardsResultsIfAccountChangesDuringRead() = runBlocking {
        val accountStore = FakeAccountStore(account("account-a", 4))
        val store = FakeCatalogStore().apply {
            searchable += CatalogItem("movie", "category", "Match", 0, null, null, null, "mp4")
            onSearch = { accountStore.value = account("account-b", 1) }
        }
        val repository = repository(accountStore, FixedCatalogApi(success("Unused")), store, wall = 10_000)
        assertEquals(CatalogSearchResult.StaleOwner, repository.searchCached(CatalogSection.Movies, "match"))
    }

    @Test
    fun cachedSearchLimitsResultsAndRejectsOverlongQuery() = runBlocking {
        val store = FakeCatalogStore().apply {
            (1..51).forEach { n ->
                searchable += CatalogItem("id-$n", "category", "Match $n", n, null, null, null, null)
            }
        }
        val repository = repository(FakeAccountStore(account("account-a", 4)), FixedCatalogApi(success("Unused")), store, 10_000)
        assertEquals(CatalogSearchResult.InvalidQuery, repository.searchCached(CatalogSection.Series, "x".repeat(81)))
        val result = repository.searchCached(CatalogSection.Series, "match") as CatalogSearchResult.Ready
        assertEquals(50, result.records.size)
        assertTrue(result.limited)
    }

    private fun repository(
        accountStore: FakeAccountStore,
        api: XtreamCatalogApi,
        store: FakeCatalogStore,
        wall: Long,
    ) = CatalogRepository(accountStore, api, store, FakeClock(wall, 5_000))

    private fun account(id: String, generation: Long) = SavedXtreamAccount(
        accountId = id,
        generation = generation,
        endpoint = ProviderEndpoint("https://provider.example", false),
        username = "user",
        password = "password",
        cleartextConsent = false,
    )

    private fun snapshot(generation: Long, refreshedAt: Long, name: String) = CatalogSnapshot(
        generation,
        refreshedAt,
        listOf(CatalogCategory("id-$name", name, 0)),
    )

    private fun success(name: String): CatalogResult<CatalogCategory> = CatalogResult.Success(
        listOf(CatalogCategory("id-$name", name, 0)),
        skippedEntries = 0,
    )

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

    private class FakeCatalogStore : CatalogStore {
        val categories = mutableMapOf<Pair<String, CatalogSection>, CatalogSnapshot<CatalogCategory>>()
        private val items = mutableMapOf<Triple<String, CatalogSection, String>, CatalogSnapshot<CatalogItem>>()
        val searchable = mutableListOf<CatalogItem>()
        val searchCalls = mutableListOf<Triple<String, CatalogSection, String>>()
        var onSearch: () -> Unit = {}
        override fun searchItems(accountId: String, section: CatalogSection, query: String, limit: Int): List<CatalogItem> {
            searchCalls += Triple(accountId, section, query)
            onSearch()
            return searchable.filter { it.name.contains(query, ignoreCase = true) }.take(limit)
        }


        override fun loadCategories(accountId: String, section: CatalogSection) = categories[accountId to section]
        override fun loadItems(accountId: String, section: CatalogSection, categoryId: String) =
            items[Triple(accountId, section, categoryId)]
        override fun replaceCategories(
            accountId: String,
            section: CatalogSection,
            snapshot: CatalogSnapshot<CatalogCategory>,
        ) {
            categories[accountId to section] = snapshot
        }
        override fun replaceItems(
            accountId: String,
            section: CatalogSection,
            categoryId: String,
            snapshot: CatalogSnapshot<CatalogItem>,
        ) {
            items[Triple(accountId, section, categoryId)] = snapshot
        }
        override fun clearAccount(accountId: String) {
            categories.keys.removeAll { it.first == accountId }
            items.keys.removeAll { it.first == accountId }
        }
    }

    private open class FixedCatalogApi(
        private val categories: CatalogResult<CatalogCategory>,
    ) : XtreamCatalogApi {
        var categoryCalls = 0
        override suspend fun categories(account: SavedXtreamAccount, section: CatalogSection): CatalogResult<CatalogCategory> {
            categoryCalls++
            return categories
        }
        override suspend fun items(
            account: SavedXtreamAccount,
            section: CatalogSection,
            categoryId: String,
        ): CatalogResult<CatalogItem> = CatalogResult.Success(emptyList(), 0)
    }

    private class BlockingCatalogApi(private val result: CatalogResult<CatalogCategory>) : XtreamCatalogApi {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        override suspend fun categories(account: SavedXtreamAccount, section: CatalogSection): CatalogResult<CatalogCategory> {
            started.complete(Unit)
            release.await()
            return result
        }
        override suspend fun items(
            account: SavedXtreamAccount,
            section: CatalogSection,
            categoryId: String,
        ): CatalogResult<CatalogItem> = CatalogResult.Success(emptyList(), 0)
    }

    private class ControllableCatalogApi : XtreamCatalogApi {
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        private var calls = 0
        override suspend fun categories(account: SavedXtreamAccount, section: CatalogSection): CatalogResult<CatalogCategory> {
            calls++
            if (calls == 1) {
                firstStarted.complete(Unit)
                releaseFirst.await()
                return successResult("Old")
            }
            return successResult("New")
        }
        override suspend fun items(
            account: SavedXtreamAccount,
            section: CatalogSection,
            categoryId: String,
        ): CatalogResult<CatalogItem> = CatalogResult.Success(emptyList(), 0)

        private fun successResult(name: String): CatalogResult<CatalogCategory> = CatalogResult.Success(
            listOf(CatalogCategory("id-$name", name, 0)),
            0,
        )
    }
}
