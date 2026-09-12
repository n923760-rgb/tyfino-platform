package dev.tyfino.foundation.xtream

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SQLiteSeriesStoreTest {
    @Test
    fun failedReplacementRollsBackAndAccountCleanupPreservesOtherAccounts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = SQLiteSeriesStore(context)
        val firstAccount = UUID.randomUUID().toString()
        val secondAccount = UUID.randomUUID().toString()
        val seriesId = UUID.randomUUID().toString()
        val first = snapshot(firstAccount, seriesId, generation = 1)
        val second = snapshot(secondAccount, seriesId, generation = 1)
        try {
            store.replace(firstAccount, seriesId, first)
            store.replace(secondAccount, seriesId, second)
            val duplicateSeason = first.copy(
                generation = 2,
                details = first.details.copy(seasons = first.details.seasons + first.details.seasons.single()),
            )

            assertThrows(RuntimeException::class.java) {
                store.replace(firstAccount, seriesId, duplicateSeason)
            }
            assertEquals(first, store.load(firstAccount, seriesId))
            store.clearAccount(firstAccount)
            assertNull(store.load(firstAccount, seriesId))
            assertEquals(second, store.load(secondAccount, seriesId))
            store.replace(firstAccount, seriesId, first)
            store.clearOtherAccounts(secondAccount)
            assertNull(store.load(firstAccount, seriesId))
            assertEquals(second, store.load(secondAccount, seriesId))
        } finally {
            store.clearAccount(firstAccount)
            store.clearAccount(secondAccount)
        }
    }

    private fun snapshot(accountId: String, seriesId: String, generation: Long) = SeriesSnapshot(
        generation = generation,
        refreshedAtEpochMillis = 12_345L,
        details = SeriesDetailsCandidate(
            accountId = accountId,
            providerSeriesId = seriesId,
            summary = SeriesSummary("Series", null, null, null, null, null, null, null, null),
            seasons = listOf(SeriesSeason(accountId, seriesId, 1, "Season 1", null, null, null, 0, 1)),
            episodes = listOf(
                SeriesEpisode(accountId, seriesId, "episode", 1, 1, "Episode", "mp4", 0, null, null, null, null),
            ),
            skippedEntries = 0,
            seasonMismatchCount = 0,
        ),
    )
}
