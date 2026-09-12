package dev.tyfino.foundation.xtream

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogSearchStoreTest {
    @Test
    fun searchUsesCurrentCategorySnapshotsLiteralTextAndActiveAccount() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = SQLiteCatalogStore(context)
        val accountA = "catalog-search-${UUID.randomUUID()}"
        val accountB = "catalog-search-${UUID.randomUUID()}"
        try {
            fun seed(accountId: String, section: CatalogSection, names: List<String>) {
                store.replaceCategories(accountId, section, CatalogSnapshot(
                    1L, 1_000L, listOf(CatalogCategory("category", "Category", 0)),
                ))
                store.replaceItems(accountId, section, "category", CatalogSnapshot(
                    1L, 1_000L, names.mapIndexed { index, name ->
                        CatalogItem("item-$index", "category", name, index, null, null, null, null)
                    },
                ))
            }
            seed(accountA, CatalogSection.Movies, listOf("مسلسل النور", "100% Offer", "100x Offer"))
            seed(accountB, CatalogSection.Movies, listOf("Secret 100% Offer"))
            seed(accountA, CatalogSection.Live, listOf("100% Live"))

            assertEquals(listOf("مسلسل النور"), store.searchItems(accountA, CatalogSection.Movies, "النور", 51).map { it.name })
            assertEquals(listOf("100% Offer"), store.searchItems(accountA, CatalogSection.Movies, "100%", 51).map { it.name })
            assertTrue(store.searchItems(accountA, CatalogSection.Series, "100%", 51).isEmpty())

            store.replaceCategories(accountA, CatalogSection.Movies, CatalogSnapshot(2L, 2_000L, emptyList()))
            assertTrue(store.searchItems(accountA, CatalogSection.Movies, "النور", 51).isEmpty())
            assertEquals(listOf("Secret 100% Offer"), store.searchItems(accountB, CatalogSection.Movies, "100%", 51).map { it.name })
        } finally {
            store.clearAccount(accountA)
            store.clearAccount(accountB)
        }
    }
}
