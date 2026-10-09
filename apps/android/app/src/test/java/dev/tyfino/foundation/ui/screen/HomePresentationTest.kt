package dev.tyfino.foundation.ui.screen

import dev.tyfino.foundation.xtream.CatalogItem
import dev.tyfino.foundation.xtream.CatalogSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomePresentationTest {
    @Test fun metadataKeepsValidProviderValuesWithoutGuessingMissingFields() {
        assertEquals(HomeShowcaseMetadata(8.75, 2026), HomePresentation.metadata(item("movie", "8.75")))
        assertEquals(HomeShowcaseMetadata(0.0, 1999), HomePresentation.metadata(
            item("zero", "-0.0").copy(releaseYear = " 1999 ")))
        assertEquals(HomeShowcaseMetadata(10.0, null), HomePresentation.metadata(
            item("maximum", "10").copy(releaseYear = null)))
        assertEquals(HomeShowcaseMetadata(null, null), HomePresentation.metadata(
            item("missing", null).copy(releaseYear = null)))
    }

    @Test fun metadataRejectsNonFiniteRatingsAndNonYearProviderText() {
        for (rating in listOf("NaN", "Infinity", "-1", "10.1", "", "unknown", "1e999")) {
            assertNull(HomePresentation.metadata(item("movie", rating)).rating)
        }
        for (year in listOf("", "0", "0000", "999", "10000", "2026-10-02", "year 2026", "２０２６")) {
            assertNull(HomePresentation.metadata(item("movie", "8").copy(releaseYear = year)).releaseYear)
        }
    }

    @Test fun highlightsMixSectionsAndRankAvailableRatingsWithoutIdCollisions() {
        val picks = HomePresentation.highlights(listOf(item("same", "3"), item("best", "9")),
            listOf(item("same", "8")))
        assertEquals(listOf("best", "same", "same"), picks.map { it.item.providerId })
        assertEquals(listOf(CatalogSection.Movies, CatalogSection.Series, CatalogSection.Movies), picks.map { it.section })
    }

    @Test fun malformedRatingsAreExcludedAndSelectionIsBoundedAndDeterministic() {
        val movies = listOf(item("invalid", "NaN"), item("over", "99"), item("valid", "7"), item("valid", "8"))
        assertEquals("valid", HomePresentation.highlights(movies, emptyList()).first().item.providerId)
        assertEquals(listOf("valid"), HomePresentation.highlights(movies, emptyList()).map { it.item.providerId })
        assertEquals(10, HomePresentation.highlights((1..30).map { item("$it", "8") },
            (1..30).map { item("$it", "7") }).size)
        assertEquals(emptyList<HomeHighlight>(), HomePresentation.highlights(emptyList(), emptyList()))
    }

    @Test fun ratingOrderIsGlobalRatherThanAlternatingSectionsOrUsingLatestDates() {
        val oldMovie = item("old-best", "9.9").copy(addedAtEpochSeconds = null)
        val freshMovie = item("new-lower", "7").copy(addedAtEpochSeconds = 9_999)
        val series = listOf(item("series", "9"), item("invalid", "Infinity"))
        assertEquals(listOf("old-best", "series", "new-lower"),
            HomePresentation.highlights(listOf(freshMovie, oldMovie), series).map { it.item.providerId })
        assertEquals(listOf("a", "b"), HomePresentation.highlights(
            listOf(item("b", "8"), item("a", "8")), emptyList()).map { it.item.providerId })
    }

    private fun item(id: String, rating: String?) = CatalogItem(id, "category", "Title $id", 0,
        null, rating, "2026", "mp4", 1_000)
}
