package dev.tyfino.foundation.playback

import dev.tyfino.foundation.xtream.CatalogItem
import org.junit.Assert.assertEquals
import org.junit.Test

class MovieResumePresentationTest {
    @Test
    fun joinsCurrentCatalogAndPreservesResumeOrder() {
        val records = listOf(record("two", 180_000L, 600_000L), record("one", 120_000L, 600_000L))
        val catalog = listOf(item("one"), item("removed"), item("two"))

        val result = MovieResumePresentation.assemble(records, catalog)

        assertEquals(listOf("two", "one"), result.map { it.catalogItem.providerId })
        assertEquals(listOf(30, 20), result.map { it.progressPercent })
    }

    @Test
    fun excludesShortAndCompletedRecords() {
        val records = listOf(
            record("short", 59_000L, 600_000L),
            record("complete", 570_000L, 600_000L),
        )

        assertEquals(emptyList<ContinueWatchingItem>(), MovieResumePresentation.assemble(records, records.map { item(it.providerItemId) }))
    }

    @Test
    fun clampsSavedPositionWhenProviderDurationShrinks() {
        assertEquals(90_000L, MovieResumePresentation.resumePosition(120_000L, 90_000L))
        assertEquals(120_000L, MovieResumePresentation.resumePosition(120_000L, null))
    }

    private fun record(id: String, position: Long, duration: Long) = MovieResumeRecord(
        accountId = "account-a",
        providerItemId = id,
        positionMillis = position,
        durationMillis = duration,
        updatedAtEpochMillis = 1L,
    )

    private fun item(id: String) = CatalogItem(
        providerId = id,
        categoryId = "category",
        name = id,
        providerOrder = 0,
        artworkUrl = null,
        rating = null,
        releaseYear = null,
        containerExtension = "mp4",
    )
}
