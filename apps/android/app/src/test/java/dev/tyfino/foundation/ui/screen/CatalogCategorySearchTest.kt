package dev.tyfino.foundation.ui.screen

import dev.tyfino.foundation.xtream.CatalogCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogCategorySearchTest {
    private val categories = listOf(
        CatalogCategory("sports-2", "SPORTS international", 0),
        CatalogCategory("movies", "أفلام عربية", 1),
        CatalogCategory("sports-1", "Sports local", 2),
    )

    @Test fun caseInsensitiveTrimmedSearchRetainsExactIdentitiesAndProviderOrder() = runBlocking {
        assertEquals(listOf(categories[0], categories[2]), filterPickerCategories(categories, " sports "))
        assertEquals(listOf(categories[1]), filterPickerCategories(categories, "عربية"))
        assertTrue(filterPickerCategories(categories, "missing").isEmpty())
        assertSame(categories, filterPickerCategories(categories, "  "))
    }

    @Test fun queryLimitCountsUnicodeCodePointsWithoutSplittingSurrogates() {
        val emoji = "\uD83D\uDE00"
        assertEquals(emoji.repeat(80), boundCategoryPickerQuery(emoji.repeat(81)))
        assertEquals("a".repeat(80), boundCategoryPickerQuery("a".repeat(81)))
    }

    @Test fun maximumPublishedSnapshotKeepsAllMatchesInProviderOrder() = runBlocking {
        val maximum = (0 until 10_000).map { CatalogCategory("$it", "Channel group $it", it) }
        assertEquals(maximum, filterPickerCategories(maximum, "group"))
    }

    @Test fun cancelledFilteringDoesNotReturnResults() = runBlocking {
        val job = Job().apply { cancel() }
        try {
            withContext(job) { filterPickerCategories(categories, "sports") }
            throw AssertionError("cancelled filtering must not complete")
        } catch (_: CancellationException) { }
    }
}
