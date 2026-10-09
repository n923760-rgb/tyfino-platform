package dev.tyfino.foundation.ui.screen

import dev.tyfino.foundation.xtream.CatalogItem
import dev.tyfino.foundation.xtream.CatalogSection

internal data class HomeHighlight(val section: CatalogSection, val item: CatalogItem)
internal data class HomeShowcaseMetadata(val rating: Double?, val releaseYear: Int?)

/** Bounded local discovery, not a claim about provider-wide popularity. No network work. */
internal object HomePresentation {
    const val HIGHLIGHT_LIMIT = 10
    fun metadata(item: CatalogItem): HomeShowcaseMetadata = HomeShowcaseMetadata(
        rating = item.rating?.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..10.0 }
            ?.let { if (it == 0.0) 0.0 else it },
        releaseYear = item.releaseYear?.trim()?.takeIf { year ->
            year.length == 4 && year.all { it in '0'..'9' }
        }?.toIntOrNull()?.takeIf { it in 1_000..9_999 },
    )

    fun highlights(movies: List<CatalogItem>, series: List<CatalogItem>): List<HomeHighlight> {
        return (movies.take(HIGHLIGHT_LIMIT).map { HomeHighlight(CatalogSection.Movies, it) } +
            series.take(HIGHLIGHT_LIMIT).map { HomeHighlight(CatalogSection.Series, it) })
            .filter { metadata(it.item).rating != null }
            .sortedWith(compareByDescending<HomeHighlight> { metadata(it.item).rating }
                .thenByDescending { it.item.addedAtEpochSeconds }
                .thenBy { it.section.ordinal }.thenBy { it.item.providerId })
            .distinctBy { it.section to it.item.providerId }
            .take(HIGHLIGHT_LIMIT)
    }
}
