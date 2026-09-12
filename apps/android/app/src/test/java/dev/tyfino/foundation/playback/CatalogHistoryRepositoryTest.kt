package dev.tyfino.foundation.playback

import dev.tyfino.foundation.xtream.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogHistoryRepositoryTest {
    @Test fun onlyOwnedPlayableCatalogItemsAreRecorded() = runBlocking {
        val fixture = Fixture()
        assertFalse(fixture.history.recordStarted(fixture.selection(CatalogSection.Series)))
        assertFalse(fixture.history.recordStarted(fixture.selection(CatalogSection.Movies, "missing")))
        assertFalse(fixture.history.recordStarted(fixture.selection(CatalogSection.Live, generation = 2)))
        assertTrue(fixture.store.records.isEmpty())
        assertTrue(fixture.history.recordStarted(fixture.selection(CatalogSection.Live)))
        assertTrue(fixture.history.recordStarted(fixture.selection(CatalogSection.Movies)))
        assertEquals(0, fixture.api.calls)
        assertEquals(listOf("Current"), (fixture.history.recent(CatalogSection.Live) as CatalogHistoryListResult.Ready).items.map { it.name })
    }

    @Test fun oldAccountCannotCommitAndOrphanIsHidden() = runBlocking {
        val fixture = Fixture()
        fixture.store.records[Triple("a", CatalogSection.Live, "orphan")] =
            CatalogHistoryRecord("a", CatalogSection.Live, "orphan", 90_000)
        assertTrue((fixture.history.recent(CatalogSection.Live) as CatalogHistoryListResult.Ready).items.isEmpty())
        fixture.accounts.value = fixture.account("b", 1)
        assertFalse(fixture.history.recordStarted(fixture.selection(CatalogSection.Live)))
        assertTrue((fixture.history.recent(CatalogSection.Live) as CatalogHistoryListResult.Ready).items.isEmpty())
        assertTrue(fixture.store.records.isEmpty())
    }

    @Test fun accountReplacementDuringCatalogLookupRejectsWrite() = runBlocking {
        val fixture = Fixture()
        fixture.catalogStore.onLookup = { fixture.accounts.value = fixture.account("b", 1) }
        assertFalse(fixture.history.recordStarted(fixture.selection(CatalogSection.Live)))
        assertTrue(fixture.store.records.isEmpty())
    }

    @Test fun futureTimestampRejectedAndLogoutIsAccountScoped() = runBlocking {
        val fixture = Fixture()
        fixture.store.records[Triple("a", CatalogSection.Live, "live")] =
            CatalogHistoryRecord("a", CatalogSection.Live, "live", 100_001)
        assertEquals(CatalogHistoryListResult.Failure, fixture.history.recent(CatalogSection.Live))
        fixture.store.records[Triple("b", CatalogSection.Live, "live")] =
            CatalogHistoryRecord("b", CatalogSection.Live, "live", 90_000)
        assertTrue(fixture.history.clearActiveAccount())
        assertTrue(fixture.store.records.keys.none { it.first == "a" })
        assertTrue(fixture.store.records.keys.any { it.first == "b" })
    }

    private class Fixture {
        val accounts = AccountStore(account("a", 1))
        val catalogStore = FakeCatalogStore()
        val api = FakeApi()
        val catalog = CatalogRepository(accounts, api, catalogStore)
        val store = FakeHistoryStore()
        val history = CatalogHistoryRepository(accounts, catalog, store, object : MovieResumeClock {
            override fun wallTimeMillis() = 100_000L
            override fun elapsedTimeMillis() = 100_000L
        })
        fun selection(section: CatalogSection, id: String = if (section == CatalogSection.Live) "live" else "movie", generation: Long = 1) =
            PlaybackSelection("a", generation, section, id, "mp4")
        fun account(id: String, generation: Long) = SavedXtreamAccount(
            id, generation, ProviderEndpoint("https://provider.example", false), "user", "password", false,
        )
    }

    private class AccountStore(var value: SavedXtreamAccount?) : XtreamAccountStore {
        override fun load() = value
        override fun save(account: SavedXtreamAccount) { value = account }
        override fun clear() { value = null }
    }

    private class FakeApi : XtreamCatalogApi {
        var calls = 0
        override suspend fun categories(account: SavedXtreamAccount, section: CatalogSection): CatalogResult<CatalogCategory> {
            calls++
            return CatalogResult.Success(emptyList(), 0)
        }
        override suspend fun items(account: SavedXtreamAccount, section: CatalogSection, categoryId: String): CatalogResult<CatalogItem> {
            calls++
            return CatalogResult.Success(emptyList(), 0)
        }
    }

    private class FakeCatalogStore : CatalogStore {
        var onLookup: () -> Unit = {}
        override fun loadCategories(accountId: String, section: CatalogSection): CatalogSnapshot<CatalogCategory>? = null
        override fun loadItems(accountId: String, section: CatalogSection, categoryId: String): CatalogSnapshot<CatalogItem>? = null
        override fun loadItemsByProviderIds(accountId: String, section: CatalogSection, providerItemIds: Set<String>): List<CatalogItem> {
            val records = if (accountId == "a") {
                val id = if (section == CatalogSection.Live) "live" else "movie"
                listOf(CatalogItem(id, "category", "Current", 0, null, null, null, "mp4"))
                    .filter { it.providerId in providerItemIds }
            } else emptyList()
            onLookup()
            return records
        }
        override fun replaceCategories(accountId: String, section: CatalogSection, snapshot: CatalogSnapshot<CatalogCategory>) = Unit
        override fun replaceItems(accountId: String, section: CatalogSection, categoryId: String, snapshot: CatalogSnapshot<CatalogItem>) = Unit
        override fun clearAccount(accountId: String) = Unit
    }

    private class FakeHistoryStore : CatalogHistoryStore {
        val records = mutableMapOf<Triple<String, CatalogSection, String>, CatalogHistoryRecord>()
        override fun list(accountId: String, section: CatalogSection, limit: Int) = records.values
            .filter { it.accountId == accountId && it.section == section }.sortedByDescending { it.startedAtEpochMillis }.take(limit)
        override fun upsert(record: CatalogHistoryRecord) {
            records[Triple(record.accountId, record.section, record.providerItemId)] = record
        }
        override fun clearAccount(accountId: String) { records.keys.removeAll { it.first == accountId } }
        override fun clearOtherAccounts(accountId: String) { records.keys.removeAll { it.first != accountId } }
    }
}
