package dev.tyfino.foundation.ui.screen

import dev.tyfino.foundation.xtream.CatalogItem
import dev.tyfino.foundation.xtream.CatalogSection

internal data class HomeHighlight(val section: CatalogSection, val item: CatalogItem)
internal data class HomeShowcaseMetadata(val rating: Double?, val releaseYear: Int?)

/** Bounded local discovery, not a claim about provider-wide popularity. No network work. */
internal object HomePresentation {
    fun metadata(item: CatalogItem): HomeShowcaseMetadata = HomeShowcaseMetadata(
        rating = item.rating?.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..10.0 }
            ?.let { if (it == 0.0) 0.0 else it },
        releaseYear = item.releaseYear?.trim()?.takeIf { year ->
            year.length == 4 && year.all { it in '0'..'9' }
        }?.toIntOrNull()?.takeIf { it in 1_000..9_999 },
    )

    fun highlights(movies: List<CatalogItem>, series: List<CatalogItem>): List<HomeHighlight> {
        fun ranked(items: List<CatalogItem>): List<CatalogItem> = items.take(20)
            .distinctBy { it.providerId }
            .sortedWith(compareByDescending<CatalogItem> {
                it.rating?.toDoubleOrNull()?.takeIf { rating -> rating.isFinite() && rating in 0.0..10.0 }
            }.thenByDescending { it.addedAtEpochSeconds })
            .take(3)
        val moviePicks = ranked(movies)
        val seriesPicks = ranked(series)
        return buildList {
            repeat(maxOf(moviePicks.size, seriesPicks.size)) { index ->
                moviePicks.getOrNull(index)?.let { add(HomeHighlight(CatalogSection.Movies, it)) }
                seriesPicks.getOrNull(index)?.let { add(HomeHighlight(CatalogSection.Series, it)) }
            }
        }
    }
}
