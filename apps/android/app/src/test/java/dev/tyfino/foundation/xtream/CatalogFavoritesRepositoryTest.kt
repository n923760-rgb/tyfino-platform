package dev.tyfino.foundation.xtream

import dev.tyfino.foundation.playback.MovieResumeClock
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogFavoritesRepositoryTest {
    @Test fun toggleAndListOnlyCurrentCachedMetadata() = runBlocking {
        val fixture = Fixture()
        val owner = FavoriteOwner("a", 1)
        assertEquals(FavoriteToggleResult.Changed(true), fixture.favorites.toggle(owner, CatalogSection.Movies, "item"))
        val ready = fixture.favorites.list(CatalogSection.Movies) as FavoritesListResult.Ready
        assertEquals(listOf("Current title"), ready.items.map { it.name })
        assertEquals(FavoriteToggleResult.Changed(false), fixture.favorites.toggle(owner, CatalogSection.Movies, "item"))
        assertTrue((fixture.favorites.list(CatalogSection.Movies) as FavoritesListResult.Ready).items.isEmpty())
        assertEquals(0, fixture.api.calls)
    }

    @Test fun missingMetadataAndStaleOwnerCannotMutate() = runBlocking {
        val fixture = Fixture()
        assertEquals(FavoriteToggleResult.Failure,
            fixture.favorites.toggle(FavoriteOwner("a", 1), CatalogSection.Movies, "missing"))
        fixture.accounts.value = fixture.account("b", 1)
        assertEquals(FavoriteToggleResult.Failure,
            fixture.favorites.toggle(FavoriteOwner("a", 1), CatalogSection.Movies, "item"))
        assertTrue(fixture.store.records.isEmpty())
    }

    @Test fun replacementDuringReadCannotPublishOrWriteAcrossAccounts() = runBlocking {
        val fixture = Fixture()
        val owner = FavoriteOwner("a", 1)
        fixture.store.onContains = { fixture.accounts.value = fixture.account("b", 1) }
        assertEquals(FavoriteToggleResult.Failure, fixture.favorites.toggle(owner, CatalogSection.Movies, "item"))
        assertTrue(fixture.store.records.isEmpty())
        fixture.accounts.value = fixture.account("a", 1)
        fixture.store.onContains = {}
        fixture.store.records[Triple("a", CatalogSection.Movies, "item")] =
            FavoriteRecord("a", CatalogSection.Movies, "item", 90_000)
        fixture.accounts.value = fixture.account("b", 1)
        assertTrue((fixture.favorites.list(CatalogSection.Movies) as FavoritesListResult.Ready).items.isEmpty())
        assertTrue(fixture.store.records.keys.any { it.first == "a" })
    }

    @Test fun orphanHiddenAndLogoutOnlyClearsActiveAccount() = runBlocking {
        val fixture = Fixture()
        fixture.store.records[Triple("a", CatalogSection.Movies, "orphan")] =
            FavoriteRecord("a", CatalogSection.Movies, "orphan", 90_000)
        assertTrue((fixture.favorites.list(CatalogSection.Movies) as FavoritesListResult.Ready).items.isEmpty())
        fixture.store.records[Triple("b", CatalogSection.Movies, "other")] =
            FavoriteRecord("b", CatalogSection.Movies, "other", 90_000)
        assertTrue(fixture.favorites.clearActiveAccount())
        assertFalse(fixture.store.records.keys.any { it.first == "a" })
        assertTrue(fixture.store.records.keys.any { it.first == "b" })
    }

    private class Fixture {
        val accounts = AccountStore(account("a", 1))
        val catalogStore = FakeCatalogStore()
        val api = FakeApi()
        val catalog = CatalogRepository(accounts, api, catalogStore)
        val store = FakeFavoriteStore()
        val favorites = CatalogFavoritesRepository(accounts, catalog, store, object : MovieResumeClock {
            override fun wallTimeMillis() = 100_000L
            override fun elapsedTimeMillis() = 100_000L
        })
        init {
            catalogStore.items = listOf(CatalogItem("item", "category", "Current title", 0, null, null, null, "mp4"))
        }
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
        var items = emptyList<CatalogItem>()
        override fun loadCategories(accountId: String, section: CatalogSection): CatalogSnapshot<CatalogCategory>? = null
        override fun loadItems(accountId: String, section: CatalogSection, categoryId: String): CatalogSnapshot<CatalogItem>? = null
        override fun loadItemsByProviderIds(accountId: String, section: CatalogSection, providerItemIds: Set<String>) =
            items.filter { it.providerId in providerItemIds && section == CatalogSection.Movies && accountId == "a" }
        override fun replaceCategories(accountId: String, section: CatalogSection, snapshot: CatalogSnapshot<CatalogCategory>) = Unit
        override fun replaceItems(accountId: String, section: CatalogSection, categoryId: String, snapshot: CatalogSnapshot<CatalogItem>) = Unit
        override fun clearAccount(accountId: String) = Unit
    }

    private class FakeFavoriteStore : FavoriteStore {
        val records = mutableMapOf<Triple<String, CatalogSection, String>, FavoriteRecord>()
        var onContains: () -> Unit = {}
        override fun list(accountId: String, section: CatalogSection, limit: Int) = records.values
            .filter { it.accountId == accountId && it.section == section }.sortedByDescending { it.updatedAtEpochMillis }.take(limit)
        override fun contains(accountId: String, section: CatalogSection, itemId: String): Boolean {
            val exists = records.containsKey(Triple(accountId, section, itemId))
            onContains()
            return exists
        }
        override fun upsert(record: FavoriteRecord) { records[Triple(record.accountId, record.section, record.providerItemId)] = record }
        override fun delete(accountId: String, section: CatalogSection, itemId: String) { records.remove(Triple(accountId, section, itemId)) }
        override fun clearAccount(accountId: String) { records.keys.removeAll { it.first == accountId } }
        override fun clearOtherAccounts(accountId: String) { records.keys.removeAll { it.first != accountId } }
    }
}
