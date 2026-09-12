package dev.tyfino.foundation.xtream

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SQLiteFavoriteStoreTest {
    @Test fun favoritesPersistAndRemainAccountAndSectionScoped() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val accountA = "favorite-test-${UUID.randomUUID()}"
        val accountB = "favorite-test-${UUID.randomUUID()}"
        val store = SQLiteFavoriteStore(context)
        try {
            store.upsert(FavoriteRecord(accountA, CatalogSection.Live, "same", 1_000))
            store.upsert(FavoriteRecord(accountA, CatalogSection.Movies, "same", 2_000))
            store.upsert(FavoriteRecord(accountB, CatalogSection.Movies, "same", 3_000))
            assertTrue(SQLiteFavoriteStore(context).contains(accountA, CatalogSection.Movies, "same"))
            assertEquals(1, store.list(accountA, CatalogSection.Live, 200).size)
            assertEquals(1, store.list(accountA, CatalogSection.Movies, 200).size)
            store.delete(accountA, CatalogSection.Movies, "same")
            assertFalse(store.contains(accountA, CatalogSection.Movies, "same"))
            assertTrue(store.contains(accountA, CatalogSection.Live, "same"))
            assertTrue(store.contains(accountB, CatalogSection.Movies, "same"))
        } finally {
            store.clearAccount(accountA)
            store.clearAccount(accountB)
        }
    }

    @Test fun favoritesAreCappedAtTwoHundredPerAccount() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val account = "favorite-cap-${UUID.randomUUID()}"
        val store = SQLiteFavoriteStore(context)
        try {
            (1..205).forEach { index ->
                store.upsert(FavoriteRecord(account, CatalogSection.Series, "item-$index", index.toLong()))
            }
            assertEquals(200, store.list(account, CatalogSection.Series, 200).size)
            assertFalse(store.contains(account, CatalogSection.Series, "item-1"))
            assertTrue(store.contains(account, CatalogSection.Series, "item-205"))
        } finally {
            store.clearAccount(account)
        }
    }
}
