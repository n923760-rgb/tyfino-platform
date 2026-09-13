package dev.tyfino.foundation.xtream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal data class LiveEpgSnapshot(
    val generation: Long,
    val refreshedAtEpochMillis: Long,
    val programs: List<LiveEpgProgram>,
)

internal interface LiveEpgStore {
    fun retainOwner(accountId: String, accountGeneration: Long)
    fun load(accountId: String, channelId: String, nowEpochMillis: Long): LiveEpgSnapshot?
    fun replace(accountId: String, channelId: String, accountGeneration: Long, snapshot: LiveEpgSnapshot,
        nowEpochMillis: Long)
    fun clearAccount(accountId: String)
}

internal data class LiveEpgDestination internal constructor(
    val accountId: String,
    val accountGeneration: Long,
    val channelId: String,
    val epoch: Long,
)

internal sealed interface LiveEpgState {
    data object Loading : LiveEpgState
    data class Content(val programs: List<LiveEpgProgram>, val refreshedAtMillis: Long,
        val refreshing: Boolean) : LiveEpgState
    data class Empty(val refreshedAtMillis: Long, val refreshing: Boolean) : LiveEpgState
    data class Stale(val programs: List<LiveEpgProgram>, val refreshedAtMillis: Long,
        val failure: LiveEpgFailure) : LiveEpgState
    data class Error(val failure: LiveEpgFailure) : LiveEpgState
}

/** One explicit Live channel destination. No startup fetch or background polling. */
internal class LiveEpgRepository(
    private val accountStore: XtreamAccountStore,
    private val api: LiveEpgApi,
    private val store: LiveEpgStore,
    private val clock: CatalogClock = AndroidCatalogClock,
) {
    private val mutex = Mutex()
    private var owner: Pair<String, Long>? = null
    private var destinationEpoch = 0L
    private var operationEpoch = 0L
    private var activeDestination: LiveEpgDestination? = null
    private var lastRefresh: Pair<Long, Long>? = null

    suspend fun open(channelId: String): LiveEpgDestination? = withContext(Dispatchers.IO) {
        if (!channelId.bounded(256)) return@withContext null
        mutex.withLock {
            val account = accountStore.load() ?: return@withLock null
            if (!account.accountId.bounded(128)) return@withLock null
            if (!retainOwner(account)) return@withLock null
            destinationEpoch = next(destinationEpoch)
            operationEpoch = next(operationEpoch)
            lastRefresh = null
            LiveEpgDestination(account.accountId, account.generation, channelId, destinationEpoch)
                .also { activeDestination = it }
        }
    }

    suspend fun guide(destination: LiveEpgDestination, forceRefresh: Boolean = false,
        publish: (LiveEpgState) -> Unit) {
        val prepared = withContext(Dispatchers.IO) {
            mutex.withLock {
                val account = accountStore.load() ?: return@withLock null
                if (!owns(destination, account)) return@withLock null
                operationEpoch = next(operationEpoch)
                val operation = operationEpoch
                val now = clock.wallTimeMillis()
                val cache = try { store.load(account.accountId, destination.channelId, now) }
                catch (_: RuntimeException) {
                    return@withLock Prepared(account, destination, operation, null, true)
                }
                Prepared(account, destination, operation, cache, false)
            }
        } ?: return

        if (prepared.storageFailure) {
            publishIfOwned(prepared, LiveEpgState.Error(LiveEpgFailure.LocalStorage), publish)
            return
        }
        val cache = prepared.cache
        if (cache != null && !forceRefresh && isFresh(cache)) {
            publishIfOwned(prepared, content(cache, false), publish)
            return
        }
        if (!publishIfOwned(prepared, cache?.let { content(it, true) } ?: LiveEpgState.Loading, publish)) {
            return
        }
        val result = api.shortGuide(prepared.account, destination.channelId)
        val state = withContext(Dispatchers.IO) {
            mutex.withLock {
                if (!owns(prepared)) return@withLock null
                when (result) {
                    is LiveEpgResult.Failure -> fallback(cache, result.reason)
                    is LiveEpgResult.Success -> {
                        val now = clock.wallTimeMillis()
                        if (now <= 0L || !valid(result, prepared)) {
                            return@withLock fallback(cache, LiveEpgFailure.UnsupportedResponse)
                        }
                        val next = LiveEpgSnapshot(
                            generation = next(cache?.generation ?: 0L),
                            refreshedAtEpochMillis = now,
                            programs = result.programs.filter { it.endEpochMillis > now }.toList(),
                        )
                        try {
                            store.replace(prepared.account.accountId, destination.channelId,
                                prepared.account.generation, next, now)
                        } catch (_: RuntimeException) {
                            return@withLock fallback(cache, LiveEpgFailure.LocalStorage)
                        }
                        lastRefresh = next.generation to clock.elapsedTimeMillis()
                        content(next, false)
                    }
                }
            }
        }
        if (state != null) publishIfOwned(prepared, state, publish)
    }

    suspend fun close(destination: LiveEpgDestination) {
        mutex.withLock {
            if (activeDestination == destination) {
                activeDestination = null
                destinationEpoch = next(destinationEpoch)
                operationEpoch = next(operationEpoch)
                lastRefresh = null
            }
        }
    }

    suspend fun clearActiveAccount(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val accountId = accountStore.load()?.accountId ?: activeDestination?.accountId
            accountId?.let(::clearAccountLocked) ?: true
        }
    }

    suspend fun clearAccount(accountId: String): Boolean = withContext(Dispatchers.IO) {
        if (!accountId.bounded(128)) return@withContext false
        mutex.withLock { clearAccountLocked(accountId) }
    }

    private fun clearAccountLocked(accountId: String): Boolean {
        if (activeDestination?.accountId == accountId) {
            activeDestination = null
            destinationEpoch = next(destinationEpoch)
            operationEpoch = next(operationEpoch)
            lastRefresh = null
        }
        if (owner?.first == accountId) owner = null
        try { store.clearAccount(accountId) } catch (_: RuntimeException) { return false }
        return true
    }

    private fun retainOwner(account: SavedXtreamAccount): Boolean {
        val current = account.accountId to account.generation
        if (owner == current) return true
        return try {
            store.retainOwner(account.accountId, account.generation)
            owner = current
            true
        } catch (_: RuntimeException) { false }
    }

    private suspend fun publishIfOwned(prepared: Prepared, state: LiveEpgState,
        publish: (LiveEpgState) -> Unit): Boolean = mutex.withLock {
        if (!owns(prepared)) return@withLock false
        publish(state)
        true
    }

    private fun owns(prepared: Prepared): Boolean =
        operationEpoch == prepared.operation && owns(prepared.destination, accountStore.load())

    private fun owns(destination: LiveEpgDestination, account: SavedXtreamAccount?): Boolean =
        account != null && activeDestination == destination && destination.epoch == destinationEpoch &&
            destination.accountId == account.accountId && destination.accountGeneration == account.generation

    private fun valid(result: LiveEpgResult.Success, prepared: Prepared): Boolean {
        val credentials = listOf(prepared.account.username, prepared.account.password).filter(String::isNotEmpty)
        return result.programs.size <= 100 && result.skippedEntries >= 0 &&
            result.programs.all {
                it.accountId == prepared.account.accountId &&
                    it.channelId == prepared.destination.channelId &&
                    it.startEpochMillis in MIN_TIME..MAX_TIME &&
                    it.endEpochMillis in MIN_TIME..MAX_TIME &&
                    it.endEpochMillis > it.startEpochMillis &&
                    listOfNotNull(it.title, it.description).all { value ->
                        value.codePointCount(0, value.length) <= 512 &&
                            credentials.none(value::contains)
                    }
            }
    }

    private fun isFresh(snapshot: LiveEpgSnapshot): Boolean {
        lastRefresh?.takeIf { it.first == snapshot.generation }?.let {
            val elapsed = clock.elapsedTimeMillis() - it.second
            return elapsed in 0 until FRESHNESS
        }
        val age = clock.wallTimeMillis() - snapshot.refreshedAtEpochMillis
        return snapshot.refreshedAtEpochMillis > 0L && age in 0 until FRESHNESS
    }

    private fun content(snapshot: LiveEpgSnapshot, refreshing: Boolean): LiveEpgState =
        if (snapshot.programs.isEmpty()) LiveEpgState.Empty(snapshot.refreshedAtEpochMillis, refreshing)
        else LiveEpgState.Content(snapshot.programs, snapshot.refreshedAtEpochMillis, refreshing)

    private fun fallback(cache: LiveEpgSnapshot?, failure: LiveEpgFailure): LiveEpgState =
        cache?.let { LiveEpgState.Stale(it.programs, it.refreshedAtEpochMillis, failure) }
            ?: LiveEpgState.Error(failure)

    private fun String.bounded(max: Int) = isNotBlank() && codePointCount(0, length) <= max
    private fun next(current: Long) = if (current == Long.MAX_VALUE) 1L else current + 1L
    private data class Prepared(val account: SavedXtreamAccount, val destination: LiveEpgDestination,
        val operation: Long, val cache: LiveEpgSnapshot?, val storageFailure: Boolean)
    private companion object {
        const val FRESHNESS = 30L * 60 * 1_000
        const val MIN_TIME = 946_684_800_000L
        const val MAX_TIME = 4_102_444_800_000L
    }
}
