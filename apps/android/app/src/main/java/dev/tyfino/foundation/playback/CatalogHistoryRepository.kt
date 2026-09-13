package dev.tyfino.foundation.playback

import dev.tyfino.foundation.xtream.CatalogItem
import dev.tyfino.foundation.xtream.CatalogRepository
import dev.tyfino.foundation.xtream.CatalogSection
import dev.tyfino.foundation.xtream.XtreamAccountStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal data class CatalogHistoryRecord(
    val accountId: String,
    val section: CatalogSection,
    val providerItemId: String,
    val startedAtEpochMillis: Long,
)

internal interface CatalogHistoryStore {
    fun list(accountId: String, section: CatalogSection, limit: Int): List<CatalogHistoryRecord>
    fun upsert(record: CatalogHistoryRecord)
    fun clearAccount(accountId: String)
    fun clearOtherAccounts(accountId: String)
}

internal sealed interface CatalogHistoryListResult {
    data class Ready(val items: List<CatalogItem>) : CatalogHistoryListResult
    data object Failure : CatalogHistoryListResult
}

/** Only records a Live/Movie item after the player actually starts playing. */
internal class CatalogHistoryRepository(
    private val accountStore: XtreamAccountStore,
    private val catalogRepository: CatalogRepository,
    private val store: CatalogHistoryStore,
    private val clock: MovieResumeClock = AndroidMovieResumeClock,
) {
    private val mutex = Mutex()
    private var retainedOwner: Pair<String, Long>? = null

    suspend fun recordStarted(selection: PlaybackSelection): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!selection.valid() || !owns(selection) || !ensureOwner(selection.accountId, selection.accountGeneration)) {
                return@withLock false
            }
            val item = catalogRepository.cachedItems(selection.section, setOf(selection.providerItemId))
                .singleOrNull { it.providerId == selection.providerItemId } ?: return@withLock false
            if (item.providerId != selection.providerItemId || !owns(selection)) return@withLock false
            val now = clock.wallTimeMillis()
            if (now <= 0L) return@withLock false
            try {
                store.upsert(CatalogHistoryRecord(selection.accountId, selection.section, selection.providerItemId, now))
                true
            } catch (_: RuntimeException) { false }
        }
    }

    suspend fun recent(section: CatalogSection): CatalogHistoryListResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (section == CatalogSection.Series) return@withLock CatalogHistoryListResult.Ready(emptyList())
            val account = accountStore.load() ?: return@withLock CatalogHistoryListResult.Failure
            if (!ensureOwner(account.accountId, account.generation)) return@withLock CatalogHistoryListResult.Failure
            val records = try { store.list(account.accountId, section, MAX_HISTORY) }
            catch (_: RuntimeException) { return@withLock CatalogHistoryListResult.Failure }
            val now = clock.wallTimeMillis()
            if (records.size > MAX_HISTORY || records.any {
                    it.accountId != account.accountId || it.section != section ||
                        !it.providerItemId.bounded(256) || it.startedAtEpochMillis !in 1..now
                }) return@withLock CatalogHistoryListResult.Failure
            val ids = records.mapTo(linkedSetOf()) { it.providerItemId }
            val current = catalogRepository.cachedItems(section, ids).associateBy { it.providerId }
            if (accountStore.load()?.let { it.accountId == account.accountId && it.generation == account.generation } != true) {
                return@withLock CatalogHistoryListResult.Failure
            }
            CatalogHistoryListResult.Ready(records.mapNotNull { current[it.providerItemId] })
        }
    }

    suspend fun clearActiveAccount(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val accountId = accountStore.load()?.accountId ?: return@withLock true
            try { store.clearAccount(accountId) } catch (_: RuntimeException) { return@withLock false }
            retainedOwner = null
            true
        }
    }

    private fun PlaybackSelection.valid(): Boolean =
        section != CatalogSection.Series && accountId.bounded(128) && providerItemId.bounded(256)

    private fun owns(selection: PlaybackSelection): Boolean = accountStore.load()?.let {
        it.accountId == selection.accountId && it.generation == selection.accountGeneration
    } == true

    private fun ensureOwner(id: String, generation: Long): Boolean {
        val owner = id to generation
        if (retainedOwner == owner) return true
        return try {
            if (retainedOwner?.first == id) store.clearAccount(id)
            retainedOwner = owner
            true
        } catch (_: RuntimeException) { false }
    }

    private fun String.bounded(maxCodePoints: Int) = isNotBlank() && codePointCount(0, length) <= maxCodePoints

    private companion object { const val MAX_HISTORY = 100 }
}
