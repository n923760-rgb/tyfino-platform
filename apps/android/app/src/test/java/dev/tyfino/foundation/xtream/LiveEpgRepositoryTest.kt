package dev.tyfino.foundation.xtream

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveEpgRepositoryTest {
    @Test fun freshCacheAvoidsProviderAndExplicitRefreshReplacesIt() = runBlocking {
        val f = Fixture()
        f.store.retainOwner("a", 1)
        f.store.replace("a", "one", 1, f.snapshot("old"), f.clock.now)
        val destination = f.repository.open("one")!!
        val states = mutableListOf<LiveEpgState>()
        f.repository.guide(destination, publish = states::add)
        assertEquals(0, f.api.calls)
        assertEquals("old", (states.last() as LiveEpgState.Content).programs.single().title)
        f.api.response = LiveEpgResult.Success(listOf(f.program("new")), 0)
        f.repository.guide(destination, forceRefresh = true, publish = states::add)
        assertEquals(1, f.api.calls)
        assertEquals("new", f.store.load("a", "one", f.clock.now)!!.programs.single().title)
    }

    @Test fun providerFailureKeepsStaleSnapshotAndAccountRemovalPurgesIt() = runBlocking {
        val f = Fixture()
        f.store.retainOwner("a", 1)
        f.store.replace("a", "one", 1, f.snapshot("old"), f.clock.now)
        val destination = f.repository.open("one")!!
        f.clock.now += 31L * 60 * 1_000
        val states = mutableListOf<LiveEpgState>()
        f.repository.guide(destination, publish = states::add)
        assertTrue(states.last() is LiveEpgState.Stale)
        assertEquals("old", f.store.load("a", "one", f.clock.now)!!.programs.single().title)
        assertTrue(f.repository.clearActiveAccount())
        assertEquals(null, f.store.load("a", "one", f.clock.now))
    }

    @Test fun replacedChannelCannotPublishOrPersistLateResponse() = runBlocking {
        val f = Fixture()
        val deferred = CompletableDeferred<LiveEpgResult>()
        f.api.next = deferred
        val old = f.repository.open("one")!!
        val states = mutableListOf<LiveEpgState>()
        val job = launch { f.repository.guide(old, publish = states::add) }
        f.api.started.await()
        f.repository.open("two")!!
        deferred.complete(LiveEpgResult.Success(listOf(f.program("old")), 0))
        job.join()
        assertTrue(states.last() is LiveEpgState.Loading)
        assertEquals(null, f.store.load("a", "one", f.clock.now))
    }

    @Test fun accountAndCredentialGenerationReplacementCannotCommitOldData() = runBlocking {
        val f = Fixture()
        val deferred = CompletableDeferred<LiveEpgResult>()
        f.api.next = deferred
        val old = f.repository.open("one")!!
        val job = launch { f.repository.guide(old, publish = {}) }
        f.api.started.await()
        f.accounts.account = f.account("a", 2)
        deferred.complete(LiveEpgResult.Success(listOf(f.program("old")), 0))
        job.join()
        assertEquals(null, f.store.load("a", "one", f.clock.now))
        assertFalse(f.repository.open(" ") != null)
        assertTrue(f.repository.open("one") != null)
        assertEquals(2L, f.store.retained!!.second)
    }

    @Test fun unownedOrCredentialBearingResultNeverReplacesCache() = runBlocking {
        val f = Fixture()
        f.store.retainOwner("a", 1)
        f.store.replace("a", "one", 1, f.snapshot("old"), f.clock.now)
        val destination = f.repository.open("one")!!
        f.api.response = LiveEpgResult.Success(listOf(f.program("wrong", channelId = "two")), 0)
        f.repository.guide(destination, forceRefresh = true, publish = {})
        assertEquals("old", f.store.load("a", "one", f.clock.now)!!.programs.single().title)
        f.api.response = LiveEpgResult.Success(listOf(f.program("pass")), 0)
        f.repository.guide(destination, forceRefresh = true, publish = {})
        assertEquals("old", f.store.load("a", "one", f.clock.now)!!.programs.single().title)
    }

    private class Fixture {
        val clock = FakeClock()
        val accounts = Accounts(account("a", 1))
        val api = FakeApi()
        val store = FakeStore()
        val repository = LiveEpgRepository(accounts, api, store, clock)
        fun account(id: String, generation: Long) = SavedXtreamAccount(
            id, generation, ProviderEndpoint("https://provider.example", false), "user", "pass", false)
        fun program(title: String, channelId: String = "one") = LiveEpgProgram(
            "a", channelId, title, null, clock.now - 1_000, clock.now + 3L * 60 * 60 * 1_000)
        fun snapshot(title: String) = LiveEpgSnapshot(1, clock.now, listOf(program(title)))
    }

    private class FakeClock : CatalogClock {
        var now = 1_704_110_500_000L
        override fun wallTimeMillis() = now
        override fun elapsedTimeMillis() = now
    }

    private class Accounts(var account: SavedXtreamAccount?) : XtreamAccountStore {
        override fun load() = account
        override fun save(account: SavedXtreamAccount) { this.account = account }
        override fun clear() { account = null }
    }

    private class FakeApi : LiveEpgApi {
        var calls = 0
        var response: LiveEpgResult = LiveEpgResult.Failure(LiveEpgFailure.ProviderUnavailable)
        var next: CompletableDeferred<LiveEpgResult>? = null
        val started = CompletableDeferred<Unit>()
        override suspend fun shortGuide(account: SavedXtreamAccount, channelId: String): LiveEpgResult {
            calls++
            started.complete(Unit)
            return next?.await() ?: response
        }
    }

    private class FakeStore : LiveEpgStore {
        var retained: Pair<String, Long>? = null
        val snapshots = mutableMapOf<Pair<String, String>, LiveEpgSnapshot>()
        override fun retainOwner(accountId: String, accountGeneration: Long) {
            if (retained != (accountId to accountGeneration)) snapshots.clear()
            retained = accountId to accountGeneration
        }
        override fun load(accountId: String, channelId: String, nowEpochMillis: Long) =
            snapshots[accountId to channelId]
        override fun replace(accountId: String, channelId: String, accountGeneration: Long,
            snapshot: LiveEpgSnapshot, nowEpochMillis: Long) {
            snapshots[accountId to channelId] = snapshot
        }
        override fun clearAccount(accountId: String) {
            snapshots.keys.removeAll { it.first == accountId }
        }
    }
}
