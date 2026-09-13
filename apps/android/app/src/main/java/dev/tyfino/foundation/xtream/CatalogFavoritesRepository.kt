package dev.tyfino.foundation.xtream

import dev.tyfino.foundation.playback.AndroidMovieResumeClock
import dev.tyfino.foundation.playback.MovieResumeClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal data class FavoriteOwner(val accountId: String, val accountGeneration: Long)

internal data class FavoriteRecord(
    val accountId: String,
    val section: CatalogSection,
    val providerItemId: String,
    val updatedAtEpochMillis: Long,
)

internal interface FavoriteStore {
    fun list(accountId: String, section: CatalogSection, limit: Int): List<FavoriteRecord>
    fun contains(accountId: String, section: CatalogSection, itemId: String): Boolean
    fun upsert(record: FavoriteRecord)
    fun delete(accountId: String, section: CatalogSection, itemId: String)
    fun clearAccount(accountId: String)
    fun clearOtherAccounts(accountId: String)
}

internal sealed interface FavoritesListResult {
    data class Ready(val owner: FavoriteOwner, val items: List<CatalogItem>) : FavoritesListResult
    data object Failure : FavoritesListResult
}

internal sealed interface FavoriteToggleResult {
    data class Changed(val selected: Boolean) : FavoriteToggleResult
    data object Failure : FavoriteToggleResult
}

/** IDs only at rest. Display metadata is always rejoined to the current account's catalog. */
internal class CatalogFavoritesRepository(
    private val accountStore: XtreamAccountStore,
    private val catalogRepository: CatalogRepository,
    private val store: FavoriteStore,
    private val clock: MovieResumeClock = AndroidMovieResumeClock,
) {
    private val mutex = Mutex()
    private var retainedOwner: FavoriteOwner? = null

    suspend fun list(section: CatalogSection): FavoritesListResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            val account = accountStore.load() ?: return@withLock FavoritesListResult.Failure
            val owner = FavoriteOwner(account.accountId, account.generation)
            if (!ensureOwner(owner)) return@withLock FavoritesListResult.Failure
            val records = try { store.list(owner.accountId, section, MAX_FAVORITES) }
            catch (_: RuntimeException) { return@withLock FavoritesListResult.Failure }
            val now = clock.wallTimeMillis()
            if (records.size > MAX_FAVORITES || records.any {
                    it.accountId != owner.accountId || it.section != section ||
                        !it.providerItemId.isValidId() || it.updatedAtEpochMillis !in 1..now
                }) return@withLock FavoritesListResult.Failure
            val ids = records.mapTo(linkedSetOf()) { it.providerItemId }
            val current = catalogRepository.cachedItems(section, ids).associateBy { it.providerId }
            if (!owns(owner)) return@withLock FavoritesListResult.Failure
            FavoritesListResult.Ready(owner, records.mapNotNull { current[it.providerItemId] })
        }
    }

    suspend fun toggle(owner: FavoriteOwner, section: CatalogSection, providerItemId: String): FavoriteToggleResult =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (!owner.accountId.isValidAccountId() || !providerItemId.isValidId() ||
                    !owns(owner) || !ensureOwner(owner)) return@withLock FavoriteToggleResult.Failure
                val item = catalogRepository.cachedItems(section, setOf(providerItemId))
                    .singleOrNull { it.providerId == providerItemId }
                    ?: return@withLock FavoriteToggleResult.Failure
                if (item.providerId != providerItemId || !owns(owner)) return@withLock FavoriteToggleResult.Failure
                try {
                    val existed = store.contains(owner.accountId, section, providerItemId)
                    if (!owns(owner)) return@withLock FavoriteToggleResult.Failure
                    if (existed) {
                        store.delete(owner.accountId, section, providerItemId)
                        FavoriteToggleResult.Changed(false)
                    } else {
                        val now = clock.wallTimeMillis()
                        if (now <= 0L) return@withLock FavoriteToggleResult.Failure
                        store.upsert(FavoriteRecord(owner.accountId, section, providerItemId, now))
                        FavoriteToggleResult.Changed(true)
                    }
                } catch (_: RuntimeException) { FavoriteToggleResult.Failure }
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

    private fun ensureOwner(owner: FavoriteOwner): Boolean {
        if (retainedOwner == owner) return true
        return try {
            if (retainedOwner?.accountId == owner.accountId) store.clearAccount(owner.accountId)
            retainedOwner = owner
            true
        } catch (_: RuntimeException) { false }
    }

    private fun owns(owner: FavoriteOwner): Boolean = accountStore.load()?.let {
        it.accountId == owner.accountId && it.generation == owner.accountGeneration
    } == true

    private fun String.isValidId(): Boolean = isNotBlank() && codePointCount(0, length) <= 256
    private fun String.isValidAccountId(): Boolean = isNotBlank() && codePointCount(0, length) <= 128

    private companion object { const val MAX_FAVORITES = 200 }
}
