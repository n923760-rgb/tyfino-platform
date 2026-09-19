package dev.tyfino.foundation.xtream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal data class MovieDetailsDestination(
    val accountId: String,
    val accountGeneration: Long,
    val movieId: String,
    val epoch: Long,
)

internal sealed interface MovieDetailsState {
    data object Empty : MovieDetailsState
    data object Loading : MovieDetailsState
    data class Content(val details: MovieDetails, val isRefreshing: Boolean) : MovieDetailsState
    data class Error(val failure: MovieDetailsFailure) : MovieDetailsState
    data class StaleContent(val details: MovieDetails, val failure: MovieDetailsFailure) : MovieDetailsState
}

internal class MovieDetailsRepository(
    private val accountStore: XtreamAccountStore,
    private val api: XtreamMovieDetailsApi,
    private val store: MovieDetailsStore,
    private val clock: CatalogClock = AndroidCatalogClock,
) {
    private val mutex = Mutex()
    private var epoch = 0L
    private var active: MovieDetailsDestination? = null
    private var operation = 0L
    private var retainedAccountId: String? = null
    private var retainedAccountGeneration: Long? = null

    suspend fun open(movieId: String): MovieDetailsDestination? = withContext(Dispatchers.IO) {
        if (movieId.isBlank() || movieId.codePointCount(0, movieId.length) > 256) return@withContext null
        mutex.withLock {
            val account = accountStore.load() ?: return@withLock null
            if (retainedAccountId == account.accountId && retainedAccountGeneration != null &&
                retainedAccountGeneration != account.generation
            ) {
                try { store.clearAccount(account.accountId) } catch (_: RuntimeException) { return@withLock null }
            }
            retainedAccountId = account.accountId
            retainedAccountGeneration = account.generation
            epoch = next(epoch)
            MovieDetailsDestination(account.accountId, account.generation, movieId, epoch).also { active = it }
        }
    }

    suspend fun details(
        destination: MovieDetailsDestination,
        forceRefresh: Boolean = false,
        publish: (MovieDetailsState) -> Unit,
    ) {
        val prepared = withContext(Dispatchers.IO) {
            mutex.withLock {
                val account = accountStore.load() ?: return@withLock null
                if (!owns(destination, account)) return@withLock null
                operation = next(operation)
                val currentOperation = operation
                val cached = try { store.load(account.accountId, destination.movieId) } catch (_: RuntimeException) {
                    return@withLock Prepared(account, destination, currentOperation, null, storageFailure = true)
                }
                Prepared(account, destination, currentOperation, cached, storageFailure = false)
            }
        } ?: return
        if (prepared.storageFailure) {
            publishIfOwned(prepared, MovieDetailsState.Error(MovieDetailsFailure.LocalStorage), publish)
            return
        }
        val cached = prepared.cached
        if (cached != null && !forceRefresh && isFresh(cached)) {
            publishIfOwned(prepared, MovieDetailsState.Content(cached.details, false), publish)
            return
        }
        if (!publishIfOwned(prepared, cached?.let { MovieDetailsState.Content(it.details, true) } ?: MovieDetailsState.Loading, publish)) return
        val result = api.details(prepared.account, destination.movieId)
        val nextState = withContext(Dispatchers.IO) {
            mutex.withLock {
                if (!owns(prepared.destination, accountStore.load()) || operation != prepared.operation) return@withLock null
                when (result) {
                    is MovieDetailsResult.Failure -> cached?.let { MovieDetailsState.StaleContent(it.details, result.reason) }
                        ?: MovieDetailsState.Error(result.reason)
                    is MovieDetailsResult.Success -> {
                        val value = result.details
                        if (value.accountId != prepared.account.accountId || value.providerMovieId != destination.movieId ||
                            containsCredential(value, prepared.account)
                        ) return@withLock cached?.let { MovieDetailsState.StaleContent(it.details, MovieDetailsFailure.UnsupportedResponse) }
                            ?: MovieDetailsState.Error(MovieDetailsFailure.UnsupportedResponse)
                        val snapshot = MovieDetailsSnapshot(next(cached?.generation ?: 0L), clock.wallTimeMillis(), value)
                        try { store.replace(value.accountId, value.providerMovieId, snapshot) } catch (_: RuntimeException) {
                            return@withLock cached?.let { MovieDetailsState.StaleContent(it.details, MovieDetailsFailure.LocalStorage) }
                                ?: MovieDetailsState.Error(MovieDetailsFailure.LocalStorage)
                        }
                        MovieDetailsState.Content(value, false)
                    }
                }
            }
        }
        if (nextState != null) publishIfOwned(prepared, nextState, publish)
    }

    suspend fun close(destination: MovieDetailsDestination) = mutex.withLock {
        if (active == destination) { active = null; epoch = next(epoch); operation = next(operation) }
    }

    suspend fun clearAccount(accountId: String): Boolean = withContext(Dispatchers.IO) {
        if (accountId.isBlank() || accountId.codePointCount(0, accountId.length) > 128) return@withContext false
        mutex.withLock {
            if (active?.accountId == accountId) { active = null; epoch = next(epoch); operation = next(operation) }
            if (retainedAccountId == accountId) { retainedAccountId = null; retainedAccountGeneration = null }
            try { store.clearAccount(accountId); true } catch (_: RuntimeException) { false }
        }
    }

    private suspend fun publishIfOwned(prepared: Prepared, state: MovieDetailsState, publish: (MovieDetailsState) -> Unit): Boolean =
        mutex.withLock {
            if (!owns(prepared.destination, accountStore.load()) || operation != prepared.operation) return@withLock false
            publish(state); true
        }

    private fun owns(destination: MovieDetailsDestination, account: SavedXtreamAccount?): Boolean =
        account != null && active == destination && destination.epoch == epoch &&
            destination.accountId == account.accountId && destination.accountGeneration == account.generation

    private fun isFresh(snapshot: MovieDetailsSnapshot): Boolean {
        val age = clock.wallTimeMillis() - snapshot.refreshedAtEpochMillis
        return snapshot.refreshedAtEpochMillis > 0L && age in 0 until FRESHNESS_MILLIS
    }

    private fun containsCredential(details: MovieDetails, account: SavedXtreamAccount): Boolean {
        val credentials = listOf(account.username, account.password).filter(String::isNotEmpty)
        val values = listOfNotNull(details.name, details.plot, details.genre, details.releaseDate, details.rating,
            details.duration, details.cast, details.director, details.posterUrl, details.backdropUrl)
        return values.any { value -> credentials.any(value::contains) }
    }

    private fun next(value: Long) = if (value == Long.MAX_VALUE) 1L else value + 1L

    private data class Prepared(
        val account: SavedXtreamAccount,
        val destination: MovieDetailsDestination,
        val operation: Long,
        val cached: MovieDetailsSnapshot?,
        val storageFailure: Boolean,
    )

    private companion object { const val FRESHNESS_MILLIS = 6L * 60L * 60L * 1_000L }
}
