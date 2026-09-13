package dev.tyfino.foundation.playback

import dev.tyfino.foundation.xtream.SeriesDetailsRepository
import dev.tyfino.foundation.xtream.SeriesEpisode
import dev.tyfino.foundation.xtream.SeriesStore
import dev.tyfino.foundation.xtream.XtreamAccountStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal data class EpisodeResumeRecord(
    val accountId: String,
    val providerSeriesId: String,
    val providerEpisodeId: String,
    val positionMillis: Long,
    val durationMillis: Long?,
    val updatedAtEpochMillis: Long,
)

internal interface EpisodeResumeStore {
    fun load(accountId: String, seriesId: String, episodeId: String): EpisodeResumeRecord?
    fun list(accountId: String, limit: Int): List<EpisodeResumeRecord>
    fun upsert(record: EpisodeResumeRecord)
    fun delete(accountId: String, seriesId: String, episodeId: String)
    fun clearAccount(accountId: String)
    fun clearOtherAccounts(accountId: String)
}

internal data class SeriesContinueWatchingItem(
    val episode: SeriesEpisode,
    val seriesTitle: String,
    val seriesGeneration: Long,
    val accountGeneration: Long,
    val positionMillis: Long,
    val durationMillis: Long?,
) {
    val progressPercent: Int?
        get() = durationMillis?.takeIf { it > 0L }?.let { ((positionMillis.toDouble() / it) * 100).toInt().coerceIn(0, 100) }
}

internal sealed interface EpisodeResumeLoadResult {
    data class Ready(val record: EpisodeResumeRecord?) : EpisodeResumeLoadResult
    data object Failure : EpisodeResumeLoadResult
}

internal sealed interface EpisodeResumeListResult {
    data class Ready(val items: List<SeriesContinueWatchingItem>) : EpisodeResumeListResult
    data object Failure : EpisodeResumeListResult
}

internal class EpisodeResumeRepository(
    private val accountStore: XtreamAccountStore,
    private val store: EpisodeResumeStore,
    private val seriesStore: SeriesStore,
    private val seriesRepository: SeriesDetailsRepository,
    private val clock: MovieResumeClock = AndroidMovieResumeClock,
) {
    private val mutex = Mutex()
    private val checkpointTimes = mutableMapOf<Triple<String, String, String>, Long>()
    private var retainedOwner: Pair<String, Long>? = null

    suspend fun load(selection: EpisodePlaybackSelection): EpisodeResumeLoadResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!owns(selection) || !ensureOwner(selection.accountId, selection.accountGeneration)) return@withLock EpisodeResumeLoadResult.Failure
            var record: EpisodeResumeRecord? = null
            var failed = false
            val current = seriesRepository.commitIfEpisodeCurrent(
                selection.accountId, selection.accountGeneration, selection.providerSeriesId,
                selection.seriesGeneration, selection.providerEpisodeId, selection.containerExtension,
            ) {
                try { record = store.load(selection.accountId, selection.providerSeriesId, selection.providerEpisodeId) }
                catch (_: RuntimeException) { failed = true }
            }
            if (!current || failed || !owns(selection)) return@withLock EpisodeResumeLoadResult.Failure
            val found = record ?: return@withLock EpisodeResumeLoadResult.Ready(null)
            if (!found.valid(clock.wallTimeMillis()) || found.accountId != selection.accountId ||
                found.providerSeriesId != selection.providerSeriesId || found.providerEpisodeId != selection.providerEpisodeId
            ) return@withLock EpisodeResumeLoadResult.Failure
            if (found.positionMillis < MovieResumePolicy.MIN_SAVE_POSITION_MILLIS ||
                MovieResumePolicy.isCompleted(found.positionMillis, found.durationMillis)
            ) {
                var deleted = false
                val stillCurrent = seriesRepository.commitIfEpisodeCurrent(
                    selection.accountId, selection.accountGeneration, selection.providerSeriesId,
                    selection.seriesGeneration, selection.providerEpisodeId, selection.containerExtension,
                ) {
                    try {
                        store.delete(found.accountId, found.providerSeriesId, found.providerEpisodeId)
                        deleted = true
                    } catch (_: RuntimeException) { /* local storage failure */ }
                }
                if (!stillCurrent || !deleted) return@withLock EpisodeResumeLoadResult.Failure
                return@withLock EpisodeResumeLoadResult.Ready(null)
            }
            EpisodeResumeLoadResult.Ready(found)
        }
    }

    suspend fun checkpoint(selection: EpisodePlaybackSelection, positionMillis: Long, durationMillis: Long?): Boolean =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val key = Triple(selection.accountId, selection.providerSeriesId, selection.providerEpisodeId)
                val elapsed = clock.elapsedTimeMillis()
                val last = checkpointTimes[key]
                if (last != null && elapsed >= last && elapsed - last < MovieResumePolicy.CHECKPOINT_INTERVAL_MILLIS) return@withLock false
                saveLocked(selection, positionMillis, durationMillis).also { if (it) checkpointTimes[key] = elapsed }
            }
        }

    suspend fun saveImmediately(selection: EpisodePlaybackSelection, positionMillis: Long, durationMillis: Long?): Boolean =
        withContext(Dispatchers.IO) {
            mutex.withLock { saveLocked(selection, positionMillis, durationMillis) }
        }

    suspend fun continueWatching(): EpisodeResumeListResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            val account = accountStore.load() ?: return@withLock EpisodeResumeListResult.Failure
            if (!ensureOwner(account.accountId, account.generation)) return@withLock EpisodeResumeListResult.Failure
            val records = try { store.list(account.accountId, 200) } catch (_: RuntimeException) {
                return@withLock EpisodeResumeListResult.Failure
            }
            val now = clock.wallTimeMillis()
            if (records.any { it.accountId != account.accountId || !it.valid(now) }) return@withLock EpisodeResumeListResult.Failure
            val snapshots = try { records.map { it.providerSeriesId }.distinct().associateWith { seriesStore.load(account.accountId, it) } }
            catch (_: RuntimeException) { return@withLock EpisodeResumeListResult.Failure }
            if (accountStore.load()?.let { it.accountId == account.accountId && it.generation == account.generation } != true) {
                return@withLock EpisodeResumeListResult.Failure
            }
            EpisodeResumeListResult.Ready(records.mapNotNull { record ->
                if (record.positionMillis < MovieResumePolicy.MIN_CONTINUE_WATCHING_POSITION_MILLIS ||
                    MovieResumePolicy.isCompleted(record.positionMillis, record.durationMillis)) return@mapNotNull null
                val snapshot = snapshots[record.providerSeriesId] ?: return@mapNotNull null
                if (snapshot.details.accountId != account.accountId ||
                    snapshot.details.providerSeriesId != record.providerSeriesId || snapshot.generation <= 0L) return@mapNotNull null
                val episode = snapshot.details.episodes.singleOrNull {
                    it.accountId == account.accountId && it.providerSeriesId == record.providerSeriesId &&
                        it.providerEpisodeId == record.providerEpisodeId
                } ?: return@mapNotNull null
                SeriesContinueWatchingItem(episode, snapshot.details.summary.name.orEmpty(), snapshot.generation,
                    account.generation, record.positionMillis, record.durationMillis)
            })
        }
    }

    suspend fun clearActiveAccount(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val id = accountStore.load()?.accountId ?: return@withLock true
            clearAccountLocked(id)
        }
    }

    suspend fun clearAccount(accountId: String): Boolean = withContext(Dispatchers.IO) {
        if (!accountId.bounded(128)) return@withContext false
        mutex.withLock { clearAccountLocked(accountId) }
    }

    private fun clearAccountLocked(accountId: String): Boolean {
        try { store.clearAccount(accountId) } catch (_: RuntimeException) { return false }
        checkpointTimes.keys.removeAll { it.first == accountId }
        if (retainedOwner?.first == accountId) retainedOwner = null
        return true
    }

    private suspend fun saveLocked(selection: EpisodePlaybackSelection, positionMillis: Long, durationMillis: Long?): Boolean {
        if (!owns(selection) || !MovieResumePolicy.isValidPosition(positionMillis, durationMillis) ||
            !ensureOwner(selection.accountId, selection.accountGeneration)) return false
        if (positionMillis < MovieResumePolicy.MIN_SAVE_POSITION_MILLIS) return false
        var saved = false
        val committed = seriesRepository.commitIfEpisodeCurrent(
            selection.accountId, selection.accountGeneration, selection.providerSeriesId,
            selection.seriesGeneration, selection.providerEpisodeId, selection.containerExtension,
        ) {
            try {
                if (MovieResumePolicy.isCompleted(positionMillis, durationMillis)) {
                    store.delete(selection.accountId, selection.providerSeriesId, selection.providerEpisodeId)
                } else {
                    val now = clock.wallTimeMillis()
                    if (now <= 0L) return@commitIfEpisodeCurrent
                    store.upsert(EpisodeResumeRecord(selection.accountId, selection.providerSeriesId,
                        selection.providerEpisodeId, positionMillis, durationMillis, now))
                }
                saved = true
            } catch (_: RuntimeException) { /* local storage failure: never expose credentials */ }
        }
        return committed && saved
    }

    private fun owns(selection: EpisodePlaybackSelection): Boolean = accountStore.load()?.let {
        it.accountId == selection.accountId && it.generation == selection.accountGeneration
    } == true

    private fun ensureOwner(id: String, generation: Long): Boolean {
        val owner = id to generation
        if (retainedOwner == owner) return true
        return try {
            if (retainedOwner?.first == id) store.clearAccount(id)
            checkpointTimes.clear()
            retainedOwner = owner
            true
        } catch (_: RuntimeException) { false }
    }

    private fun EpisodeResumeRecord.valid(now: Long): Boolean =
        accountId.bounded(128) && providerSeriesId.bounded(256) && providerEpisodeId.bounded(256) &&
            MovieResumePolicy.isValidPosition(positionMillis, durationMillis) && updatedAtEpochMillis in 1..now

    private fun String.bounded(limit: Int): Boolean = isNotBlank() && codePointCount(0, length) <= limit
}
