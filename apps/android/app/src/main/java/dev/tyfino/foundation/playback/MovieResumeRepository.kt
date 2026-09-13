package dev.tyfino.foundation.playback

import android.os.SystemClock
import dev.tyfino.foundation.xtream.CatalogSection
import dev.tyfino.foundation.xtream.XtreamAccountStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal data class MovieResumeRecord(
    val accountId: String,
    val providerItemId: String,
    val positionMillis: Long,
    val durationMillis: Long?,
    val updatedAtEpochMillis: Long,
)

internal interface MovieResumeStore {
    fun load(accountId: String, providerItemId: String): MovieResumeRecord?
    fun list(accountId: String, limit: Int): List<MovieResumeRecord>
    fun upsert(record: MovieResumeRecord)
    fun delete(accountId: String, providerItemId: String)
    fun clearAccount(accountId: String)
}

internal interface MovieResumeClock {
    fun wallTimeMillis(): Long
    fun elapsedTimeMillis(): Long
}

internal object AndroidMovieResumeClock : MovieResumeClock {
    override fun wallTimeMillis(): Long = System.currentTimeMillis()
    override fun elapsedTimeMillis(): Long = SystemClock.elapsedRealtime()
}

internal enum class MovieResumeFailure {
    InvalidSelection,
    InvalidPosition,
    InvalidRecord,
    StaleOwner,
    LocalStorage,
}

internal enum class MovieResumeSkipReason {
    BelowSaveThreshold,
    CheckpointTooSoon,
}

internal sealed interface MovieResumeLoadResult {
    data class Ready(val record: MovieResumeRecord?) : MovieResumeLoadResult
    data class Failure(val reason: MovieResumeFailure) : MovieResumeLoadResult
}

internal sealed interface MovieResumeListResult {
    data class Ready(val records: List<MovieResumeRecord>) : MovieResumeListResult
    data class Failure(val reason: MovieResumeFailure) : MovieResumeListResult
}

internal sealed interface MovieResumeWriteResult {
    data object Saved : MovieResumeWriteResult
    data object Deleted : MovieResumeWriteResult
    data class Skipped(val reason: MovieResumeSkipReason) : MovieResumeWriteResult
    data class Failure(val reason: MovieResumeFailure) : MovieResumeWriteResult
}

internal class MovieResumeRepository(
    private val accountStore: XtreamAccountStore,
    private val store: MovieResumeStore,
    private val clock: MovieResumeClock = AndroidMovieResumeClock,
) {
    private val mutex = Mutex()
    private val checkpointTimes = mutableMapOf<MovieResumeKey, Long>()

    suspend fun load(selection: PlaybackSelection): MovieResumeLoadResult =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (!selection.isValidMovieSelection()) {
                    return@withLock MovieResumeLoadResult.Failure(
                        MovieResumeFailure.InvalidSelection,
                    )
                }
                if (!owns(selection)) {
                    return@withLock MovieResumeLoadResult.Failure(MovieResumeFailure.StaleOwner)
                }
                val record = try {
                    store.load(selection.accountId, selection.providerItemId)
                } catch (_: RuntimeException) {
                    return@withLock MovieResumeLoadResult.Failure(MovieResumeFailure.LocalStorage)
                }
                if (!owns(selection)) {
                    return@withLock MovieResumeLoadResult.Failure(MovieResumeFailure.StaleOwner)
                }
                if (record == null) return@withLock MovieResumeLoadResult.Ready(null)
                val now = clock.wallTimeMillis()
                if (!record.isStructurallyValid(now) || record.key != selection.key) {
                    return@withLock MovieResumeLoadResult.Failure(MovieResumeFailure.InvalidRecord)
                }
                if (
                    record.positionMillis < MovieResumePolicy.MIN_SAVE_POSITION_MILLIS ||
                    MovieResumePolicy.isCompleted(record.positionMillis, record.durationMillis)
                ) {
                    try {
                        store.delete(record.accountId, record.providerItemId)
                    } catch (_: RuntimeException) {
                        return@withLock MovieResumeLoadResult.Failure(
                            MovieResumeFailure.LocalStorage,
                        )
                    }
                    return@withLock MovieResumeLoadResult.Ready(null)
                }
                MovieResumeLoadResult.Ready(record)
            }
        }

    suspend fun checkpoint(
        selection: PlaybackSelection,
        positionMillis: Long,
        durationMillis: Long?,
    ): MovieResumeWriteResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            val elapsed = clock.elapsedTimeMillis()
            val last = checkpointTimes[selection.key]
            if (
                last != null &&
                elapsed >= last &&
                elapsed - last < MovieResumePolicy.CHECKPOINT_INTERVAL_MILLIS
            ) {
                return@withLock MovieResumeWriteResult.Skipped(
                    MovieResumeSkipReason.CheckpointTooSoon,
                )
            }
            saveLocked(selection, positionMillis, durationMillis).also { result ->
                if (result is MovieResumeWriteResult.Saved || result is MovieResumeWriteResult.Deleted) {
                    checkpointTimes[selection.key] = elapsed
                }
            }
        }
    }

    suspend fun saveImmediately(
        selection: PlaybackSelection,
        positionMillis: Long,
        durationMillis: Long?,
    ): MovieResumeWriteResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            saveLocked(selection, positionMillis, durationMillis).also { result ->
                if (result is MovieResumeWriteResult.Saved || result is MovieResumeWriteResult.Deleted) {
                    checkpointTimes[selection.key] = clock.elapsedTimeMillis()
                }
            }
        }
    }

    suspend fun continueWatching(): MovieResumeListResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            val account = accountStore.load()
                ?: return@withLock MovieResumeListResult.Failure(
                    MovieResumeFailure.StaleOwner,
                )
            val records = try {
                store.list(account.accountId, MAX_CONTINUE_WATCHING_CANDIDATES)
            } catch (_: RuntimeException) {
                return@withLock MovieResumeListResult.Failure(MovieResumeFailure.LocalStorage)
            }
            val current = accountStore.load()
            if (
                current?.accountId != account.accountId ||
                current.generation != account.generation
            ) {
                return@withLock MovieResumeListResult.Failure(MovieResumeFailure.StaleOwner)
            }
            val now = clock.wallTimeMillis()
            if (records.any { !it.isStructurallyValid(now) || it.accountId != account.accountId }) {
                return@withLock MovieResumeListResult.Failure(MovieResumeFailure.InvalidRecord)
            }
            MovieResumeListResult.Ready(
                records.filter(MovieResumePolicy::isContinueWatching),
            )
        }
    }

    suspend fun clearActiveAccount(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val accountId = accountStore.load()?.accountId ?: return@withLock true
            clearAccountLocked(accountId)
        }
    }

    suspend fun clearAccount(accountId: String): Boolean = withContext(Dispatchers.IO) {
        if (accountId.isBlank() || accountId.codePointCount(0, accountId.length) > 128) {
            return@withContext false
        }
        mutex.withLock { clearAccountLocked(accountId) }
    }

    private fun clearAccountLocked(accountId: String): Boolean {
        try { store.clearAccount(accountId) } catch (_: RuntimeException) { return false }
        checkpointTimes.keys.removeAll { it.accountId == accountId }
        return true
    }

    private fun saveLocked(
        selection: PlaybackSelection,
        positionMillis: Long,
        durationMillis: Long?,
    ): MovieResumeWriteResult {
        if (!selection.isValidMovieSelection()) {
            return MovieResumeWriteResult.Failure(MovieResumeFailure.InvalidSelection)
        }
        if (!owns(selection)) {
            return MovieResumeWriteResult.Failure(MovieResumeFailure.StaleOwner)
        }
        if (!MovieResumePolicy.isValidPosition(positionMillis, durationMillis)) {
            return MovieResumeWriteResult.Failure(MovieResumeFailure.InvalidPosition)
        }
        if (positionMillis < MovieResumePolicy.MIN_SAVE_POSITION_MILLIS) {
            return MovieResumeWriteResult.Skipped(MovieResumeSkipReason.BelowSaveThreshold)
        }

        val action = if (MovieResumePolicy.isCompleted(positionMillis, durationMillis)) {
            ResumeAction.Delete
        } else {
            ResumeAction.Upsert
        }
        if (!owns(selection)) {
            return MovieResumeWriteResult.Failure(MovieResumeFailure.StaleOwner)
        }
        return try {
            when (action) {
                ResumeAction.Delete -> {
                    store.delete(selection.accountId, selection.providerItemId)
                    MovieResumeWriteResult.Deleted
                }
                ResumeAction.Upsert -> {
                    val updatedAt = clock.wallTimeMillis()
                    if (updatedAt <= 0L) {
                        MovieResumeWriteResult.Failure(MovieResumeFailure.LocalStorage)
                    } else {
                        store.upsert(
                            MovieResumeRecord(
                                accountId = selection.accountId,
                                providerItemId = selection.providerItemId,
                                positionMillis = positionMillis,
                                durationMillis = durationMillis,
                                updatedAtEpochMillis = updatedAt,
                            ),
                        )
                        MovieResumeWriteResult.Saved
                    }
                }
            }
        } catch (_: RuntimeException) {
            MovieResumeWriteResult.Failure(MovieResumeFailure.LocalStorage)
        }
    }

    private fun owns(selection: PlaybackSelection): Boolean {
        val account = accountStore.load() ?: return false
        return account.accountId == selection.accountId &&
            account.generation == selection.accountGeneration
    }

    private fun PlaybackSelection.isValidMovieSelection(): Boolean =
        section == CatalogSection.Movies &&
            accountId.isBoundedIdentifier(MAX_ACCOUNT_ID_CODE_POINTS) &&
            providerItemId.isBoundedIdentifier(MAX_PROVIDER_ID_CODE_POINTS)

    private fun MovieResumeRecord.isStructurallyValid(now: Long): Boolean =
        accountId.isBoundedIdentifier(MAX_ACCOUNT_ID_CODE_POINTS) &&
            providerItemId.isBoundedIdentifier(MAX_PROVIDER_ID_CODE_POINTS) &&
            MovieResumePolicy.isValidPosition(positionMillis, durationMillis) &&
            updatedAtEpochMillis in 1..now

    private fun String.isBoundedIdentifier(limit: Int): Boolean =
        isNotBlank() && codePointCount(0, length) <= limit

    private val PlaybackSelection.key: MovieResumeKey
        get() = MovieResumeKey(accountId, providerItemId)

    private val MovieResumeRecord.key: MovieResumeKey
        get() = MovieResumeKey(accountId, providerItemId)

    private enum class ResumeAction {
        Upsert,
        Delete,
    }

    private data class MovieResumeKey(
        val accountId: String,
        val providerItemId: String,
    )

    private companion object {
        const val MAX_ACCOUNT_ID_CODE_POINTS = 128
        const val MAX_PROVIDER_ID_CODE_POINTS = 256
        const val MAX_CONTINUE_WATCHING_CANDIDATES = 200
    }
}

internal object MovieResumePolicy {
    const val MIN_SAVE_POSITION_MILLIS = 30_000L
    const val MIN_CONTINUE_WATCHING_POSITION_MILLIS = 60_000L
    const val CHECKPOINT_INTERVAL_MILLIS = 10_000L
    private const val COMPLETION_REMAINING_MILLIS = 60_000L
    private const val COMPLETION_RATIO = 0.95

    fun isValidPosition(positionMillis: Long, durationMillis: Long?): Boolean =
        positionMillis >= 0L &&
            (durationMillis == null || (durationMillis > 0L && positionMillis <= durationMillis))

    fun isCompleted(positionMillis: Long, durationMillis: Long?): Boolean {
        if (!isValidPosition(positionMillis, durationMillis) || durationMillis == null) return false
        val ratio = positionMillis.toDouble() / durationMillis.toDouble()
        val remaining = durationMillis - positionMillis
        return ratio >= COMPLETION_RATIO || remaining < COMPLETION_REMAINING_MILLIS
    }

    fun isContinueWatching(record: MovieResumeRecord): Boolean =
        record.positionMillis >= MIN_CONTINUE_WATCHING_POSITION_MILLIS &&
            !isCompleted(record.positionMillis, record.durationMillis)
}
