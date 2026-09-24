package dev.tyfino.foundation.ui.screen

import dev.tyfino.foundation.xtream.CatalogItem
import java.util.Locale

internal enum class CatalogSort { Newest, HighestRated, Name }

/** Sorts only the items already loaded for the current account; missing metadata goes last. */
internal fun sortCatalogItems(items: List<CatalogItem>, order: CatalogSort): List<CatalogItem> {
    val keyed = items.map { item ->
        SortableItem(
            item = item,
            rating = item.rating?.toDoubleOrNull()?.takeIf(Double::isFinite),
            normalizedName = item.name.lowercase(Locale.ROOT),
        )
    }
    val stable = compareBy<SortableItem> { it.item.providerOrder }.thenBy { it.item.providerId }
    val comparator = when (order) {
        CatalogSort.Newest -> compareByDescending<SortableItem> { it.item.addedAtEpochSeconds }.then(stable)
        CatalogSort.HighestRated -> compareByDescending<SortableItem> { it.rating }.then(stable)
        CatalogSort.Name -> compareBy<SortableItem> { it.normalizedName }.then(stable)
    }
    return keyed.sortedWith(comparator).map { it.item }
}

private data class SortableItem(
    val item: CatalogItem,
    val rating: Double?,
    val normalizedName: String,
)
