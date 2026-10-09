package dev.tyfino.foundation.xtream

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogTopRatedStoreTest {
    @Test fun topRatingsIncludeOldAndUndatedWorksAndExcludeInvalidOtherAccountsAndRemovedCategories() {
        val store = SQLiteCatalogStore(InstrumentationRegistry.getInstrumentation().targetContext)
        val a = "rated-${UUID.randomUUID()}"
        val b = "rated-${UUID.randomUUID()}"
        fun item(id: String, score: String?, date: Long? = null) =
            CatalogItem(id, "category", id, 0, null, score, null, null, date)
        fun seed(account: String, section: CatalogSection, records: List<CatalogItem>) {
            store.replaceCategories(account, section, CatalogSnapshot(1, 1_000,
                listOf(CatalogCategory("category", "Category", 0), CatalogCategory("other", "Other", 1))))
            store.replaceItems(account, section, "category", CatalogSnapshot(1, 1_000, records))
        }
        try {
            seed(a, CatalogSection.Movies, listOf(item("old-best", "9.9", 1), item("undated", "9.8")) +
                (1..20).map { item("recent-$it", "7", 1_000L + it) } +
                listOf(item("missing", null), item("text", "not-rated"), item("nan", "NaN"),
                    item("infinite", "Infinity"), item("over", "11"), item("negative", "-1")))
            store.replaceItems(a, CatalogSection.Movies, "other", CatalogSnapshot(1, 1_000,
                listOf(item("old-best", "9.0").copy(categoryId = "other"))))
            seed(a, CatalogSection.Series, listOf(item("series-best", "10")))
            seed(b, CatalogSection.Movies, listOf(item("other-account", "10")))
            val picks = store.topRatedCached(a, CatalogSection.Movies, 10)
            assertEquals(10, picks.size)
            assertEquals(listOf("old-best", "undated", "recent-20"), picks.take(3).map { it.providerId })
            assertEquals(10, picks.map { it.providerId }.distinct().size)
            assertEquals(listOf("series-best"), store.topRatedCached(a, CatalogSection.Series, 10).map { it.providerId })
            assertEquals(listOf("other-account"), store.topRatedCached(b, CatalogSection.Movies, 10).map { it.providerId })
            store.replaceItems(a, CatalogSection.Movies, "category", CatalogSnapshot(2, 2_000,
                listOf(item("zero", "0"), item("malformed", "8junk"))))
            assertEquals(listOf("old-best", "zero"), store.topRatedCached(a, CatalogSection.Movies, 10).map { it.providerId })
            store.replaceCategories(a, CatalogSection.Movies, CatalogSnapshot(2, 2_000, emptyList()))
            assertTrue(store.topRatedCached(a, CatalogSection.Movies, 10).isEmpty())
        } finally {
            store.clearAccount(a)
            store.clearAccount(b)
        }
    }
}
