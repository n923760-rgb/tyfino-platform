package dev.tyfino.foundation.ui.screen

import dev.tyfino.foundation.xtream.CatalogItem
import dev.tyfino.foundation.xtream.CatalogSection
import org.junit.Assert.assertEquals
import org.junit.Test

class HomePresentationTest {
    @Test fun highlightsMixSectionsAndRankAvailableRatingsWithoutIdCollisions() {
        val picks = HomePresentation.highlights(listOf(item("same", "3"), item("best", "9")),
            listOf(item("same", "8")))
        assertEquals(listOf("best", "same", "same"), picks.map { it.item.providerId })
        assertEquals(listOf(CatalogSection.Movies, CatalogSection.Series, CatalogSection.Movies), picks.map { it.section })
    }

    @Test fun malformedRatingsAreLastAndSelectionIsBoundedAndDeterministic() {
        val movies = listOf(item("invalid", "NaN"), item("over", "99"), item("valid", "7"), item("valid", "8"))
        assertEquals("valid", HomePresentation.highlights(movies, emptyList()).first().item.providerId)
        assertEquals(6, HomePresentation.highlights((1..30).map { item("$it", "8") },
            (1..30).map { item("$it", "7") }).size)
        assertEquals(emptyList<HomeHighlight>(), HomePresentation.highlights(emptyList(), emptyList()))
    }

    private fun item(id: String, rating: String?) = CatalogItem(id, "category", "Title $id", 0,
        null, rating, "2026", "mp4", 1_000)
}
