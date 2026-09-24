package dev.tyfino.foundation.ui.screen

import dev.tyfino.foundation.xtream.CatalogItem
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogSortTest {
    private val items = listOf(
        item("b", "zebra", 2, "8.5", 100L),
        item("a", "Alpha", 1, "9.0", 200L),
        item("c", "مرحبا", 3, "unknown", null),
        item("d", "alpha", 4, null, 200L),
    )

    @Test fun newestPutsMissingDatesLastAndKeepsProviderOrderForTies() {
        assertEquals(listOf("a", "d", "b", "c"), sortCatalogItems(items, CatalogSort.Newest).map { it.providerId })
    }

    @Test fun ratingPutsMissingOrInvalidScoresLast() {
        assertEquals(listOf("a", "b", "c", "d"), sortCatalogItems(items, CatalogSort.HighestRated).map { it.providerId })
    }

    @Test fun nameIgnoresEnglishCaseAndPreservesProviderOrderForTies() {
        assertEquals(listOf("a", "d", "b", "c"), sortCatalogItems(items, CatalogSort.Name).map { it.providerId })
    }

    private fun item(id: String, title: String, order: Int, rating: String?, added: Long?) =
        CatalogItem(id, "category", title, order, null, rating, null, null, added)
}
