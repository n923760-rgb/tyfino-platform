package dev.tyfino.foundation.playback

import dev.tyfino.foundation.xtream.CatalogItem

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
        return records.asSequence()
            .filter(MovieResumePolicy::isContinueWatching)
            .mapNotNull { record ->
                catalogById[record.providerItemId]?.let { item ->
                    ContinueWatchingItem(
                        catalogItem = item,
                        progressPercent = progressPercent(record),
                    )
                }
            }
            .toList()
    }

    fun resumePosition(savedPositionMillis: Long, currentDurationMillis: Long?): Long {
        require(savedPositionMillis >= 0L)
        return currentDurationMillis
            ?.takeIf { it > 0L }
            ?.let { duration -> savedPositionMillis.coerceAtMost(duration) }
            ?: savedPositionMillis
    }

    private fun progressPercent(record: MovieResumeRecord): Int? {
        val duration = record.durationMillis?.takeIf { it > 0L } ?: return null
        return ((record.positionMillis.toDouble() / duration.toDouble()) * 100.0)
            .toInt()
            .coerceIn(0, 94)
    }
}
