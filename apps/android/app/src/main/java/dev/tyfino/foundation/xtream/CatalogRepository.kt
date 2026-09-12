package dev.tyfino.foundation.xtream

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal data class CatalogSnapshot<T>(
    val generation: Long,
    val refreshedAtEpochMillis: Long,
    val records: List<T>,
)

internal interface CatalogStore {
    fun loadCategories(accountId: String, section: CatalogSection): CatalogSnapshot<CatalogCategory>?
    fun loadItems(accountId: String, section: CatalogSection, categoryId: String): CatalogSnapshot<CatalogItem>?
    /** Searches only currently stored category snapshots; never initiates a provider request. */
    fun searchItems(accountId: String, section: CatalogSection, query: String, limit: Int): List<CatalogItem> = emptyList()
    fun loadItemsByProviderIds(
        accountId: String,
        section: CatalogSection,
        providerItemIds: Set<String>,
    ): List<CatalogItem> = emptyList()
    fun replaceCategories(
        accountId: String,
        section: CatalogSection,
        snapshot: CatalogSnapshot<CatalogCategory>,
    )
    fun replaceItems(
        accountId: String,
        section: CatalogSection,
        categoryId: String,
        snapshot: CatalogSnapshot<CatalogItem>,
    )
    fun clearAccount(accountId: String)
}

internal interface CatalogClock {
    fun wallTimeMillis(): Long
    fun elapsedTimeMillis(): Long
}

internal object AndroidCatalogClock : CatalogClock {
    override fun wallTimeMillis(): Long = System.currentTimeMillis()
    override fun elapsedTimeMillis(): Long = SystemClock.elapsedRealtime()
}

internal sealed interface CatalogState<out T> {
    data object Empty : CatalogState<Nothing>
    data object Loading : CatalogState<Nothing>
    data class Content<T>(
        val records: List<T>,
        val lastSuccessfulRefreshMillis: Long,
        val isRefreshing: Boolean,
    ) : CatalogState<T>
    data class EmptyContent(
        val lastSuccessfulRefreshMillis: Long,
        val isRefreshing: Boolean,
    ) : CatalogState<Nothing>
    data class Error(val failure: CatalogFailure) : CatalogState<Nothing>
    data class StaleContent<T>(
        val records: List<T>,
        val lastSuccessfulRefreshMillis: Long,
        val failure: CatalogFailure,
    ) : CatalogState<T>
}

internal sealed interface CatalogSearchResult {
    data class Ready(val records: List<CatalogItem>, val limited: Boolean) : CatalogSearchResult
    data object InvalidQuery : CatalogSearchResult
    data object StaleOwner : CatalogSearchResult
    data object LocalStorage : CatalogSearchResult
}

internal class CatalogRepository(
    private val accountStore: XtreamAccountStore,
    private val api: XtreamCatalogApi,
    private val store: CatalogStore,
    private val clock: CatalogClock = AndroidCatalogClock,
) {
    private val mutex = Mutex()
    private val latestOperations = mutableMapOf<CatalogKey, Long>()
    private val monotonicRefreshes = mutableMapOf<CatalogKey, MonotonicRefresh>()

    suspend fun categories(
        section: CatalogSection,
        forceRefresh: Boolean = false,
        publish: (CatalogState<CatalogCategory>) -> Unit,
    ) = load(
        key = CatalogKey(section, null),
        freshnessMillis = CATEGORY_FRESHNESS_MILLIS,
        forceRefresh = forceRefresh,
        loadCache = { accountId -> store.loadCategories(accountId, section) },
        fetch = { account -> api.categories(account, section) },
        replace = { accountId, snapshot -> store.replaceCategories(accountId, section, snapshot) },
        publish = publish,
    )

    suspend fun items(
        section: CatalogSection,
        categoryId: String,
        forceRefresh: Boolean = false,
        publish: (CatalogState<CatalogItem>) -> Unit,
    ) = load(
        key = CatalogKey(section, categoryId),
        freshnessMillis = ITEM_FRESHNESS_MILLIS,
        forceRefresh = forceRefresh,
        loadCache = { accountId -> store.loadItems(accountId, section, categoryId) },
        fetch = { account -> api.items(account, section, categoryId) },
        replace = { accountId, snapshot -> store.replaceItems(accountId, section, categoryId, snapshot) },
        publish = publish,
    )

    suspend fun cachedItems(
        section: CatalogSection,
        providerItemIds: Set<String>,
    ): List<CatalogItem> = withContext(Dispatchers.IO) {
        if (
            providerItemIds.isEmpty() ||
            providerItemIds.size > MAX_CACHED_ITEM_LOOKUP ||
            providerItemIds.any { it.isBlank() || it.codePointCount(0, it.length) > MAX_PROVIDER_ID_CODE_POINTS }
        ) return@withContext emptyList()
        mutex.withLock {
            val account = accountStore.load() ?: return@withLock emptyList()
            val records = runCatching {
                store.loadItemsByProviderIds(account.accountId, section, providerItemIds)
            }.getOrElse { return@withLock emptyList() }
            val current = accountStore.load()
            if (
                current?.accountId != account.accountId ||
                current.generation != account.generation
            ) emptyList() else records
        }
    }

    suspend fun searchCached(section: CatalogSection, rawQuery: String): CatalogSearchResult = withContext(Dispatchers.IO) {
        val query = rawQuery.trim()
        val length = query.codePointCount(0, query.length)
        if (length !in 2..MAX_SEARCH_QUERY_CODE_POINTS) return@withContext CatalogSearchResult.InvalidQuery
        mutex.withLock {
            val account = accountStore.load() ?: return@withLock CatalogSearchResult.StaleOwner
            val records = try {
                store.searchItems(account.accountId, section, query, MAX_SEARCH_RESULTS + 1)
            } catch (_: RuntimeException) {
                return@withLock CatalogSearchResult.LocalStorage
            }
            val active = accountStore.load()
            if (active?.accountId != account.accountId || active.generation != account.generation) {
                return@withLock CatalogSearchResult.StaleOwner
            }
            CatalogSearchResult.Ready(
                records.distinctBy(CatalogItem::providerId).take(MAX_SEARCH_RESULTS),
                limited = records.size > MAX_SEARCH_RESULTS,
            )
        }
    }

    suspend fun clearActiveAccount() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val accountId = accountStore.load()?.accountId ?: return@withLock
            latestOperations.clear()
            monotonicRefreshes.keys.removeAll { it.accountId == accountId }
            store.clearAccount(accountId)
        }
    }

    private suspend fun <T> load(
        key: CatalogKey,
        freshnessMillis: Long,
        forceRefresh: Boolean,
        loadCache: (String) -> CatalogSnapshot<T>?,
        fetch: suspend (SavedXtreamAccount) -> CatalogResult<T>,
        replace: (String, CatalogSnapshot<T>) -> Unit,
        publish: (CatalogState<T>) -> Unit,
    ) {
        val prepared = withContext(Dispatchers.IO) {
            mutex.withLock {
                val account = accountStore.load()
                    ?: return@withLock Preparation.Failure(CatalogFailure.AuthenticationRejected)
                val ownedKey = key.copy(accountId = account.accountId)
                val cache = runCatching { loadCache(account.accountId) }
                    .getOrElse { return@withLock Preparation.Failure(CatalogFailure.LocalStorage) }
                val operationId = (latestOperations[ownedKey] ?: 0L) + 1L
                latestOperations[ownedKey] = operationId
                Preparation.Ready(account, ownedKey, operationId, cache)
            }
        }
        if (prepared is Preparation.Failure) {
            publish(CatalogState.Error(prepared.failure))
            return
        }
        prepared as Preparation.Ready<T>
        val cache = prepared.cache
        if (cache != null && !forceRefresh && isFresh(prepared.key, cache, freshnessMillis)) {
            publish(contentState(cache, refreshing = false))
            return
        }
        publish(cache?.let { contentState(it, refreshing = true) } ?: CatalogState.Loading)

        val result = fetch(prepared.account)
        val state = withContext(Dispatchers.IO) {
            mutex.withLock {
                if (!owns(prepared)) return@withLock null
                when (result) {
                    is CatalogResult.Success -> {
                        val nowWall = clock.wallTimeMillis()
                        val next = CatalogSnapshot(
                            generation = (prepared.cache?.generation ?: 0L) + 1L,
                            refreshedAtEpochMillis = nowWall,
                            records = result.records,
                        )
                        try {
                            replace(prepared.account.accountId, next)
                        } catch (_: RuntimeException) {
                            return@withLock failureState(prepared.cache, CatalogFailure.LocalStorage)
                        }
                        monotonicRefreshes[prepared.key] = MonotonicRefresh(
                            generation = next.generation,
                            elapsedMillis = clock.elapsedTimeMillis(),
                        )
                        contentState(next, refreshing = false)
                    }
                    is CatalogResult.Failure -> failureState(prepared.cache, result.reason)
                }
            }
        }
        state?.let(publish)
    }

    private fun <T> contentState(
        snapshot: CatalogSnapshot<T>,
        refreshing: Boolean,
    ): CatalogState<T> = if (snapshot.records.isEmpty()) {
        CatalogState.EmptyContent(snapshot.refreshedAtEpochMillis, refreshing)
    } else {
        CatalogState.Content(
            records = snapshot.records,
            lastSuccessfulRefreshMillis = snapshot.refreshedAtEpochMillis,
            isRefreshing = refreshing,
        )
    }

    private fun <T> failureState(
        cache: CatalogSnapshot<T>?,
        failure: CatalogFailure,
    ): CatalogState<T> = cache?.let {
        CatalogState.StaleContent(it.records, it.refreshedAtEpochMillis, failure)
    } ?: CatalogState.Error(failure)

    private fun owns(prepared: Preparation.Ready<*>): Boolean {
        val current = accountStore.load() ?: return false
        return current.accountId == prepared.account.accountId &&
            current.generation == prepared.account.generation &&
            latestOperations[prepared.key] == prepared.operationId
    }

    private fun isFresh(key: CatalogKey, snapshot: CatalogSnapshot<*>, freshnessMillis: Long): Boolean {
        monotonicRefreshes[key]
            ?.takeIf { it.generation == snapshot.generation }
            ?.let { refresh ->
                val elapsed = clock.elapsedTimeMillis() - refresh.elapsedMillis
                return elapsed in 0 until freshnessMillis
            }
        val age = clock.wallTimeMillis() - snapshot.refreshedAtEpochMillis
        return snapshot.refreshedAtEpochMillis > 0L && age in 0 until freshnessMillis
    }

    private data class CatalogKey(
        val section: CatalogSection,
        val categoryId: String?,
        val accountId: String = "",
    )

    private data class MonotonicRefresh(val generation: Long, val elapsedMillis: Long)

    private sealed interface Preparation<out T> {
        data class Ready<T>(
            val account: SavedXtreamAccount,
            val key: CatalogKey,
            val operationId: Long,
            val cache: CatalogSnapshot<T>?,
        ) : Preparation<T>
        data class Failure(val failure: CatalogFailure) : Preparation<Nothing>
    }

    private companion object {
        const val CATEGORY_FRESHNESS_MILLIS = 12L * 60L * 60L * 1_000L
        const val ITEM_FRESHNESS_MILLIS = 6L * 60L * 60L * 1_000L
        const val MAX_CACHED_ITEM_LOOKUP = 200
        const val MAX_SEARCH_QUERY_CODE_POINTS = 80
        const val MAX_SEARCH_RESULTS = 50
        const val MAX_PROVIDER_ID_CODE_POINTS = 256
    }
}
