package dev.tyfino.foundation.playback

import dev.tyfino.foundation.xtream.SeriesDetailsRepository
import dev.tyfino.foundation.xtream.SeriesEpisode
import dev.tyfino.foundation.xtream.SeriesStore
import dev.tyfino.foundation.xtream.XtreamAccountStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal data class EpisodeHistoryRecord(
    val accountId: String,
    val providerSeriesId: String,
    val providerEpisodeId: String,
    val startedAtEpochMillis: Long,
)

internal interface EpisodeHistoryStore {
    fun list(accountId: String, limit: Int): List<EpisodeHistoryRecord>
    fun upsert(record: EpisodeHistoryRecord)
    fun clearAccount(accountId: String)
}

internal data class SeriesHistoryItem(
    val episode: SeriesEpisode,
    val seriesTitle: String,
    val seriesGeneration: Long,
    val accountGeneration: Long,
    val startedAtEpochMillis: Long,
    val seriesArtworkUrl: String? = null,
)

internal sealed interface EpisodeHistoryListResult {
    data class Ready(val items: List<SeriesHistoryItem>) : EpisodeHistoryListResult
    data object Failure : EpisodeHistoryListResult
}

/** Records an episode only after Media3 reports real foreground playback. */
internal class EpisodeHistoryRepository(
    private val accountStore: XtreamAccountStore,
    private val store: EpisodeHistoryStore,
    private val seriesStore: SeriesStore,
    private val seriesRepository: SeriesDetailsRepository,
    private val clock: MovieResumeClock = AndroidMovieResumeClock,
) {
    private val mutex = Mutex()
    private var retainedOwner: Pair<String, Long>? = null

    suspend fun recordStarted(selection: EpisodePlaybackSelection): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!selection.valid() || !owns(selection) || !ensureOwner(selection.accountId, selection.accountGeneration)) {
                return@withLock false
            }
            var stored = false
            val committed = seriesRepository.commitIfEpisodeCurrent(
                selection.accountId,
                selection.accountGeneration,
                selection.providerSeriesId,
                selection.seriesGeneration,
                selection.providerEpisodeId,
                selection.containerExtension,
            ) {
                val now = clock.wallTimeMillis()
                if (now <= 0L) return@commitIfEpisodeCurrent
                try {
                    store.upsert(
                        EpisodeHistoryRecord(
                            selection.accountId,
                            selection.providerSeriesId,
                            selection.providerEpisodeId,
                            now,
                        ),
                    )
                    stored = true
                } catch (_: RuntimeException) { /* fail closed */ }
            }
            committed && stored && owns(selection)
        }
    }

    suspend fun recent(): EpisodeHistoryListResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            val account = accountStore.load() ?: return@withLock EpisodeHistoryListResult.Failure
            if (!ensureOwner(account.accountId, account.generation)) return@withLock EpisodeHistoryListResult.Failure
            val records = try { store.list(account.accountId, MAX_HISTORY) } catch (_: RuntimeException) {
                return@withLock EpisodeHistoryListResult.Failure
            }
            val now = clock.wallTimeMillis()
            if (records.size > MAX_HISTORY || records.any { !it.valid(account.accountId, now) }) {
                return@withLock EpisodeHistoryListResult.Failure
            }
            val snapshots = try {
                records.map(EpisodeHistoryRecord::providerSeriesId).distinct()
                    .associateWith { seriesStore.load(account.accountId, it) }
            } catch (_: RuntimeException) {
                return@withLock EpisodeHistoryListResult.Failure
            }
            if (accountStore.load()?.let { it.accountId == account.accountId && it.generation == account.generation } != true) {
                return@withLock EpisodeHistoryListResult.Failure
            }
            EpisodeHistoryListResult.Ready(
                records.mapNotNull { record ->
                    val snapshot = snapshots[record.providerSeriesId] ?: return@mapNotNull null
                    if (snapshot.generation <= 0L || snapshot.details.accountId != account.accountId ||
                        snapshot.details.providerSeriesId != record.providerSeriesId
                    ) return@mapNotNull null
                    val episode = snapshot.details.episodes.singleOrNull {
                        it.accountId == account.accountId && it.providerSeriesId == record.providerSeriesId &&
                            it.providerEpisodeId == record.providerEpisodeId
                    } ?: return@mapNotNull null
                    SeriesHistoryItem(
                        episode = episode,
                        seriesTitle = snapshot.details.summary.name.orEmpty(),
                        seriesGeneration = snapshot.generation,
                        accountGeneration = account.generation,
                        startedAtEpochMillis = record.startedAtEpochMillis,
                    )
                },
            )
        }
    }

    suspend fun clearAccount(accountId: String): Boolean = withContext(Dispatchers.IO) {
        if (!accountId.bounded(128)) return@withContext false
        mutex.withLock {
            try { store.clearAccount(accountId) } catch (_: RuntimeException) { return@withLock false }
            if (retainedOwner?.first == accountId) retainedOwner = null
            true
        }
    }

    private fun ensureOwner(accountId: String, generation: Long): Boolean {
        val owner = accountId to generation
        if (retainedOwner == owner) return true
        return try {
            if (retainedOwner?.first == accountId) store.clearAccount(accountId)
            retainedOwner = owner
            true
        } catch (_: RuntimeException) { false }
    }

    private fun owns(selection: EpisodePlaybackSelection): Boolean = accountStore.load()?.let {
        it.accountId == selection.accountId && it.generation == selection.accountGeneration
    } == true

    private fun EpisodePlaybackSelection.valid() =
        accountId.bounded(128) && providerSeriesId.bounded(256) && providerEpisodeId.bounded(256) &&
            seriesGeneration > 0L && SAFE_EXTENSION.matches(containerExtension)

    private fun EpisodeHistoryRecord.valid(owner: String, now: Long) =
        accountId == owner && providerSeriesId.bounded(256) && providerEpisodeId.bounded(256) &&
            startedAtEpochMillis in 1..now

    private fun String.bounded(limit: Int) = isNotBlank() && codePointCount(0, length) <= limit

    private companion object {
        const val MAX_HISTORY = 100
        val SAFE_EXTENSION = Regex("[a-z0-9]{1,12}")
    }
}
