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
    fun selectedIdsAreMatchedBeforeTheLimitAndRemainAccountSectionAndCategoryScoped() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = SQLiteCatalogStore(context)
        val accountA = "filtered-search-${UUID.randomUUID()}"
        val accountB = "filtered-search-${UUID.randomUUID()}"
        try {
            fun seed(accountId: String, section: CatalogSection) {
                store.replaceCategories(accountId, section, CatalogSnapshot(
                    1L, 1_000L, listOf(CatalogCategory("category", "Category", 0)),
                ))
                store.replaceItems(accountId, section, "category", CatalogSnapshot(
                    1L, 1_000L, (0..119).map { index ->
                        CatalogItem("item-$index", "category", "Match ${index.toString().padStart(3, '0')}", index, null, null, null, null)
                    },
                ))
            }
            seed(accountA, CatalogSection.Movies)
            seed(accountB, CatalogSection.Movies)
            seed(accountA, CatalogSection.Live)
            assertEquals(51, store.searchItems(accountA, CatalogSection.Movies, "Match", 51).size)
            assertEquals(listOf("item-90", "item-91"), store.searchItems(
                accountA, CatalogSection.Movies, "Match", 51, setOf("item-90", "item-91"),
            ).map { it.providerId })
            assertTrue(store.searchItems(accountA, CatalogSection.Movies, "Match", 51, emptySet()).isEmpty())
            assertTrue(store.searchItems(accountA, CatalogSection.Series, "Match", 51, setOf("item-90")).isEmpty())
            assertEquals(51, store.searchItems(
                accountA, CatalogSection.Movies, "Match", 51, (60..119).mapTo(hashSetOf()) { "item-$it" },
            ).size)
            store.replaceCategories(accountA, CatalogSection.Movies, CatalogSnapshot(2L, 2_000L, emptyList()))
            assertTrue(store.searchItems(accountA, CatalogSection.Movies, "Match", 51, setOf("item-90")).isEmpty())
            assertEquals(listOf("item-90"), store.searchItems(accountB, CatalogSection.Movies, "Match", 51, setOf("item-90")).map { it.providerId })
            assertEquals(listOf("item-90"), store.searchItems(accountA, CatalogSection.Live, "Match", 51, setOf("item-90")).map { it.providerId })
        } finally {
            store.clearAccount(accountA)
            store.clearAccount(accountB)
        }
    }

    @Test
    fun selectedIdsAndSearchMetacharactersRemainLiteralBoundParameters() {
        val store = SQLiteCatalogStore(InstrumentationRegistry.getInstrumentation().targetContext)
        val account = "literal-search-${UUID.randomUUID()}"
        val id = "quoted') OR 1=1 --"
        try {
            store.replaceCategories(account, CatalogSection.Movies, CatalogSnapshot(
                1L, 1_000L, listOf(CatalogCategory("category", "Category", 0)),
            ))
            store.replaceItems(account, CatalogSection.Movies, "category", CatalogSnapshot(
                1L, 1_000L, listOf(
                    CatalogItem(id, "category", "100%_Match", 0, null, null, null, null),
                    CatalogItem("ordinary", "category", "100xxMatch", 1, null, null, null, null),
                ),
            ))
            assertEquals(listOf(id), store.searchItems(account, CatalogSection.Movies, "%_", 51, setOf(id)).map { it.providerId })
            assertTrue(store.searchItems(account, CatalogSection.Movies, "Match", 51, setOf("absent') OR 1=1 --")).isEmpty())
        } finally {
            store.clearAccount(account)
        }
    }

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
