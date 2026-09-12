package dev.tyfino.foundation.playback

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.xtream.CatalogSection
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SQLiteCatalogHistoryStoreTest {
    @Test fun historyPersistsAndIsolatesAccountsAndSections() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = SQLiteCatalogHistoryStore(context)
        val a = "history-${UUID.randomUUID()}"
        val b = "history-${UUID.randomUUID()}"
        try {
            store.upsert(CatalogHistoryRecord(a, CatalogSection.Live, "same", 1_000))
            store.upsert(CatalogHistoryRecord(a, CatalogSection.Movies, "same", 2_000))
            store.upsert(CatalogHistoryRecord(b, CatalogSection.Live, "same", 3_000))
            assertEquals(1, SQLiteCatalogHistoryStore(context).list(a, CatalogSection.Live, 100).size)
            assertEquals(1, store.list(a, CatalogSection.Movies, 100).size)
            store.clearAccount(a)
            assertTrue(store.list(a, CatalogSection.Live, 100).isEmpty())
            assertEquals(1, store.list(b, CatalogSection.Live, 100).size)
        } finally {
            store.clearAccount(a)
            store.clearAccount(b)
        }
    }

    @Test fun historyKeepsAtMostOneHundredMostRecentItems() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = SQLiteCatalogHistoryStore(context)
        val a = "history-cap-${UUID.randomUUID()}"
        try {
            (1..105).forEach { index ->
                store.upsert(CatalogHistoryRecord(a, CatalogSection.Live, "item-$index", index.toLong()))
            }
            val records = store.list(a, CatalogSection.Live, 100)
            assertEquals(100, records.size)
            assertFalse(records.any { it.providerItemId == "item-1" })
            assertTrue(records.any { it.providerItemId == "item-105" })
        } finally { store.clearAccount(a) }
    }
}
