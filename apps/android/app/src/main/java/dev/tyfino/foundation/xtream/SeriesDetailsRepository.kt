package dev.tyfino.foundation.xtream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal data class SeriesSnapshot(
    val generation: Long,
    val refreshedAtEpochMillis: Long,
    val details: SeriesDetailsCandidate,
)

internal interface SeriesStore {
    fun load(accountId: String, seriesId: String): SeriesSnapshot?
    fun replace(accountId: String, seriesId: String, snapshot: SeriesSnapshot)
    fun clearAccount(accountId: String)
    fun clearOtherAccounts(accountId: String)
}

internal data class SeriesDestination internal constructor(
    val accountId: String,
    val accountGeneration: Long,
    val seriesId: String,
    val epoch: Long,
)

internal sealed interface SeriesState {
    data object Empty : SeriesState
    data object Loading : SeriesState
    data class Content(
        val details: SeriesDetailsCandidate,
        val lastSuccessfulRefreshMillis: Long,
        val isRefreshing: Boolean,
        val generation: Long = 0L,
    ) : SeriesState
    data class EmptyContent(
        val details: SeriesDetailsCandidate,
        val lastSuccessfulRefreshMillis: Long,
        val isRefreshing: Boolean,
        val generation: Long = 0L,
    ) : SeriesState
    data class Error(val failure: SeriesFailure) : SeriesState
    data class StaleContent(
        val details: SeriesDetailsCandidate,
        val lastSuccessfulRefreshMillis: Long,
        val failure: SeriesFailure,
        val generation: Long = 0L,
    ) : SeriesState
}

internal class SeriesDetailsRepository(
    private val accountStore: XtreamAccountStore,
    private val api: XtreamSeriesApi,
    private val store: SeriesStore,
    private val clock: CatalogClock = AndroidCatalogClock,
) {
    private val mutex = Mutex()
    private val latestOperations = mutableMapOf<SeriesKey, Long>()
    private val monotonicRefreshes = mutableMapOf<SeriesKey, MonotonicRefresh>()
    private var destinationEpoch = 0L
    private var activeDestination: SeriesDestination? = null
    private var retainedAccountId: String? = null
    private var retainedAccountGeneration: Long? = null

    suspend fun open(seriesId: String): SeriesDestination? = withContext(Dispatchers.IO) {
        if (!isValidProviderId(seriesId)) return@withContext null
        mutex.withLock {
            val account = accountStore.load() ?: return@withLock null
            if (retainedAccountId != account.accountId) {
                retainedAccountId = account.accountId
                retainedAccountGeneration = account.generation
                latestOperations.keys.removeAll { it.accountId != account.accountId }
                monotonicRefreshes.keys.removeAll { it.accountId != account.accountId }
            } else if (retainedAccountGeneration != account.generation) {
                try {
                    store.clearAccount(account.accountId)
                } catch (_: RuntimeException) {
                    return@withLock null
                }
                retainedAccountGeneration = account.generation
                latestOperations.keys.removeAll { it.accountId == account.accountId }
                monotonicRefreshes.keys.removeAll { it.accountId == account.accountId }
            }
            destinationEpoch = nextCounter(destinationEpoch)
            SeriesDestination(
                accountId = account.accountId,
                accountGeneration = account.generation,
                seriesId = seriesId,
                epoch = destinationEpoch,
            ).also { activeDestination = it }
        }
    }

    suspend fun details(
        destination: SeriesDestination,
        forceRefresh: Boolean = false,
        publish: (SeriesState) -> Unit,
    ) {
        val prepared = withContext(Dispatchers.IO) {
            mutex.withLock {
                val account = accountStore.load() ?: return@withLock Preparation.Stale
                if (!ownsDestination(destination, account)) return@withLock Preparation.Stale
                val key = SeriesKey(account.accountId, destination.seriesId)
                val operationId = nextCounter(latestOperations[key] ?: 0L)
                latestOperations[key] = operationId
                val cache = try {
                    store.load(account.accountId, destination.seriesId)
                } catch (_: RuntimeException) {
                    return@withLock Preparation.StorageFailure(destination, key, operationId)
                }
                Preparation.Ready(account, destination, key, operationId, cache)
            }
        }
        when (prepared) {
            Preparation.Stale -> return
            is Preparation.StorageFailure -> {
                publishIfOwned(prepared.destination, prepared.key, prepared.operationId, SeriesState.Error(SeriesFailure.LocalStorage), publish)
                return
            }
            is Preparation.Ready -> Unit
        }
        prepared as Preparation.Ready
        val cache = prepared.cache
        if (cache != null && !forceRefresh && isFresh(prepared.key, cache)) {
            publishIfOwned(
                prepared.destination,
                prepared.key,
                prepared.operationId,
                contentState(cache, refreshing = false),
                publish,
            )
            return
        }
        val loadingState = cache?.let { contentState(it, refreshing = true) } ?: SeriesState.Loading
        if (!publishIfOwned(prepared.destination, prepared.key, prepared.operationId, loadingState, publish)) {
            return
        }

        val result = api.details(prepared.account, prepared.destination.seriesId)
        val state = withContext(Dispatchers.IO) {
            mutex.withLock {
                if (!owns(prepared)) return@withLock null
                when (result) {
                    is SeriesDetailsResult.Failure -> failureState(cache, result.reason)
                    is SeriesDetailsResult.Success -> {
                        val candidate = result.details
                        if (!isOwnedCandidate(candidate, prepared) || containsCredential(candidate, prepared.account)) {
                            return@withLock failureState(cache, SeriesFailure.UnsupportedResponse)
                        }
                        val nowWall = clock.wallTimeMillis()
                        val next = SeriesSnapshot(
                            generation = nextCounter(cache?.generation ?: 0L),
                            refreshedAtEpochMillis = nowWall,
                            details = candidate.copy(
                                seasons = candidate.seasons.toList(),
                                episodes = candidate.episodes.toList(),
                            ),
                        )
                        try {
                            store.replace(prepared.account.accountId, prepared.destination.seriesId, next)
                        } catch (_: RuntimeException) {
                            return@withLock failureState(cache, SeriesFailure.LocalStorage)
                        }
                        monotonicRefreshes[prepared.key] = MonotonicRefresh(
                            generation = next.generation,
                            elapsedMillis = clock.elapsedTimeMillis(),
                        )
                        contentState(next, refreshing = false)
                    }
                }
            }
        }
        if (state != null) {
            publishIfOwned(prepared.destination, prepared.key, prepared.operationId, state, publish)
        }
    }

    /** Hold the cache lock through the playback commit, so a newer Series generation cannot race it. */
    suspend fun commitIfEpisodeCurrent(
        accountId: String,
        accountGeneration: Long,
        seriesId: String,
        seriesGeneration: Long,
        episodeId: String,
        extension: String,
        publish: () -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val account = accountStore.load() ?: return@withLock false
            if (account.accountId != accountId || account.generation != accountGeneration ||
                seriesGeneration <= 0L || !isValidProviderId(seriesId) ||
                !isValidProviderId(episodeId) || !SAFE_EPISODE_EXTENSION.matches(extension)
            ) return@withLock false
            val snapshot = try {
                store.load(accountId, seriesId)
            } catch (_: RuntimeException) {
                return@withLock false
            } ?: return@withLock false
            if (snapshot.generation != seriesGeneration ||
                snapshot.details.accountId != accountId ||
                snapshot.details.providerSeriesId != seriesId ||
                snapshot.details.episodes.none {
                    it.accountId == accountId && it.providerSeriesId == seriesId &&
                        it.providerEpisodeId == episodeId && it.containerExtension == extension
                }
            ) return@withLock false
            publish()
            true
        }
    }

    suspend fun close(destination: SeriesDestination) {
        mutex.withLock {
            if (activeDestination == destination) {
                activeDestination = null
                destinationEpoch = nextCounter(destinationEpoch)
                latestOperations.remove(SeriesKey(destination.accountId, destination.seriesId))
            }
        }
    }

    suspend fun clearActiveAccount() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val accountId = accountStore.load()?.accountId ?: activeDestination?.accountId
            activeDestination = null
            destinationEpoch = nextCounter(destinationEpoch)
            retainedAccountId = null
            retainedAccountGeneration = null
            if (accountId != null) {
                latestOperations.keys.removeAll { it.accountId == accountId }
                monotonicRefreshes.keys.removeAll { it.accountId == accountId }
                store.clearAccount(accountId)
            }
        }
    }

    private suspend fun publishIfOwned(
        destination: SeriesDestination,
        key: SeriesKey,
        operationId: Long,
        state: SeriesState,
        publish: (SeriesState) -> Unit,
    ): Boolean = mutex.withLock {
        val account = accountStore.load() ?: return@withLock false
        if (
            !ownsDestination(destination, account) ||
            latestOperations[key] != operationId
        ) {
            return@withLock false
        }
        publish(state)
        true
    }

    private fun owns(prepared: Preparation.Ready): Boolean {
        val account = accountStore.load() ?: return false
        return ownsDestination(prepared.destination, account) &&
            latestOperations[prepared.key] == prepared.operationId
    }

    private fun ownsDestination(destination: SeriesDestination, account: SavedXtreamAccount): Boolean =
        activeDestination == destination &&
            destination.accountId == account.accountId &&
            destination.accountGeneration == account.generation &&
            destination.epoch == destinationEpoch

    private fun isOwnedCandidate(candidate: SeriesDetailsCandidate, prepared: Preparation.Ready): Boolean =
        candidate.accountId == prepared.account.accountId &&
            candidate.providerSeriesId == prepared.destination.seriesId &&
            candidate.seasons.size <= MAX_SEASONS &&
            candidate.episodes.size <= MAX_EPISODES &&
            candidate.skippedEntries >= 0 &&
            candidate.seasonMismatchCount >= 0 &&
            candidate.seasons.all {
                it.accountId == prepared.account.accountId &&
                    it.providerSeriesId == prepared.destination.seriesId
            } &&
            candidate.episodes.all {
                it.accountId == prepared.account.accountId &&
                    it.providerSeriesId == prepared.destination.seriesId
            }

    private fun containsCredential(candidate: SeriesDetailsCandidate, account: SavedXtreamAccount): Boolean {
        val credentials = listOf(account.username, account.password).filter(String::isNotEmpty)
        if (credentials.isEmpty()) return false
        val values = buildList {
            add(candidate.providerSeriesId)
            with(candidate.summary) {
                addAll(listOfNotNull(name, plot, genre, releaseDate, rating, cast, director, coverUrl, backdropUrl))
            }
            candidate.seasons.forEach { season ->
                add(season.displayLabel)
                addAll(listOfNotNull(season.overview, season.airDate, season.coverUrl))
            }
            candidate.episodes.forEach { episode ->
                add(episode.providerEpisodeId)
                addAll(
                    listOfNotNull(
                        episode.title,
                        episode.containerExtension,
                        episode.plot,
                        episode.duration,
                        episode.releaseDate,
                        episode.rating,
                    ),
                )
            }
        }
        return values.any { value ->
            credentials.any { credential -> value.contains(credential) }
        }
    }

    private fun contentState(snapshot: SeriesSnapshot, refreshing: Boolean): SeriesState =
        if (snapshot.details.episodes.isEmpty()) {
            SeriesState.EmptyContent(snapshot.details, snapshot.refreshedAtEpochMillis, refreshing, snapshot.generation)
        } else {
            SeriesState.Content(snapshot.details, snapshot.refreshedAtEpochMillis, refreshing, snapshot.generation)
        }

    private fun failureState(cache: SeriesSnapshot?, failure: SeriesFailure): SeriesState =
        cache?.let { SeriesState.StaleContent(it.details, it.refreshedAtEpochMillis, failure, it.generation) }
            ?: SeriesState.Error(failure)

    private fun isFresh(key: SeriesKey, snapshot: SeriesSnapshot): Boolean {
        monotonicRefreshes[key]
            ?.takeIf { it.generation == snapshot.generation }
            ?.let { refresh ->
                val elapsed = clock.elapsedTimeMillis() - refresh.elapsedMillis
                return elapsed in 0 until FRESHNESS_MILLIS
            }
        val age = clock.wallTimeMillis() - snapshot.refreshedAtEpochMillis
        return snapshot.refreshedAtEpochMillis > 0L && age in 0 until FRESHNESS_MILLIS
    }

    private fun isValidProviderId(value: String): Boolean =
        value.isNotBlank() && value.codePointCount(0, value.length) <= MAX_PROVIDER_ID_CODE_POINTS

    private fun nextCounter(value: Long): Long = if (value == Long.MAX_VALUE) 1L else value + 1L

    private data class SeriesKey(val accountId: String, val seriesId: String)
    private data class MonotonicRefresh(val generation: Long, val elapsedMillis: Long)

    private sealed interface Preparation {
        data class Ready(
            val account: SavedXtreamAccount,
            val destination: SeriesDestination,
            val key: SeriesKey,
            val operationId: Long,
            val cache: SeriesSnapshot?,
        ) : Preparation
        data class StorageFailure(
            val destination: SeriesDestination,
            val key: SeriesKey,
            val operationId: Long,
        ) : Preparation
        data object Stale : Preparation
    }

    private companion object {
        const val FRESHNESS_MILLIS = 6L * 60L * 60L * 1_000L
        const val MAX_PROVIDER_ID_CODE_POINTS = 256
        const val MAX_SEASONS = 1_000
        const val MAX_EPISODES = 10_000
        val SAFE_EPISODE_EXTENSION = Regex("[a-z0-9]{1,12}")
    }
}
