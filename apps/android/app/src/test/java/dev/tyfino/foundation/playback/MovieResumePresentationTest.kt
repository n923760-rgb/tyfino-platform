package dev.tyfino.foundation.playback

import dev.tyfino.foundation.xtream.CatalogItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MovieResumePresentationTest {
    @Test
    fun continueWatchingUsesCurrentCatalogAndResumeOrdering() {
        val records = listOf(
            record("movie-new", 90_000, 300_000),
            record("movie-removed", 80_000, 300_000),
            record("movie-old", 60_000, null),
        )
        val catalog = listOf(item("movie-old", "Old"), item("movie-new", "New"))

        val result = MovieResumePresentation.assemble(records, catalog)

        assertEquals(listOf("movie-new", "movie-old"), result.map { it.catalogItem.providerId })
        assertEquals(30, result.first().progressPercent)
        assertNull(result.last().progressPercent)
    }

    @Test
    fun ineligibleAndCompletedRecordsAreExcluded() {
        val catalog = listOf(item("short", "Short"), item("complete", "Complete"))
        val records = listOf(
            record("short", 59_999, 300_000),
            record("complete", 190_000, 200_000),
        )

        assertEquals(emptyList<ContinueWatchingItem>(), MovieResumePresentation.assemble(records, catalog))
    }

    @Test
    fun savedPositionIsClampedWhenProviderDurationShrinks() {
        assertEquals(75_000, MovieResumePresentation.resumePosition(90_000, 75_000))
        assertEquals(90_000, MovieResumePresentation.resumePosition(90_000, null))
    }

    private fun record(id: String, position: Long, duration: Long?) = MovieResumeRecord(
        accountId = "account-a",
        providerItemId = id,
        positionMillis = position,
        durationMillis = duration,
        updatedAtEpochMillis = 100_000,
    )

    private fun item(id: String, name: String) = CatalogItem(
        providerId = id,
        categoryId = "category",
        name = name,
        providerOrder = 0,
        artworkUrl = null,
        rating = null,
        releaseYear = null,
        containerExtension = "mp4",
    )
}
