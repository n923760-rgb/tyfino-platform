package dev.tyfino.foundation.playback

import dev.tyfino.foundation.xtream.CatalogItem
import kotlin.math.roundToInt

internal data class ContinueWatchingItem(
    val catalogItem: CatalogItem,
    val progressPercent: Int?,
)

internal object MovieResumePresentation {
    fun assemble(
        records: List<MovieResumeRecord>,
        currentCatalog: List<CatalogItem>,
    ): List<ContinueWatchingItem> {
        val catalogById = currentCatalog.associateBy(CatalogItem::providerId)
        return records
            .asSequence()
            .filter(MovieResumePolicy::isContinueWatching)
            .mapNotNull { record ->
                catalogById[record.providerItemId]?.let { item ->
                    ContinueWatchingItem(item, record.progressPercent())
                }
            }
            .distinctBy { it.catalogItem.providerId }
            .toList()
    }

    fun resumePosition(savedPositionMillis: Long, currentDurationMillis: Long?): Long =
        if (currentDurationMillis != null && currentDurationMillis > 0L) {
            savedPositionMillis.coerceIn(0L, currentDurationMillis)
        } else {
            savedPositionMillis.coerceAtLeast(0L)
        }

    private fun MovieResumeRecord.progressPercent(): Int? = durationMillis
        ?.takeIf { it > 0L }
        ?.let { ((positionMillis.toDouble() / it) * 100.0).roundToInt().coerceIn(0, 94) }
}
