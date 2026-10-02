package dev.tyfino.foundation.xtream

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistRepositoryTest {
    @Test fun providerFieldsAreParsedWithoutChangingLicensingAuthority() {
        val result = PlaylistParser.parse("""{"user_info":{"auth":"1","status":"Active","exp_date":"1800000000","is_trial":"0","active_cons":"1","max_connections":"2"}}""") as PlaylistResult.Ready
        assertEquals(1_800_000_000_000L, result.info.expiresAtMillis)
        assertEquals(PlaylistStatus.Active, result.info.status)
        assertEquals(false, result.info.trial)
        assertEquals(1, result.info.activeConnections)
        assertEquals(2, result.info.maxConnections)
    }

    @Test fun unknownExpiryDoesNotBecomeALifetimeClaimAndMalformedNumbersAreBounded() {
        for (expiry in listOf("null", "0", "-1", "1.5", "true", "\"999999999999999999999\"", "{}")) {
            val result = PlaylistParser.parse("""{"user_info":{"auth":1,"status":"Active","exp_date":$expiry,"max_connections":10001,"is_trial":"unknown"}}""") as PlaylistResult.Ready
            assertNull(result.info.expiresAtMillis)
            assertNull(result.info.maxConnections)
            assertNull(result.info.trial)
        }
    }

    @Test fun rejectedAndUnsupportedProviderResponsesNeverLookActive() {
        assertTrue(PlaylistParser.parse("not json") is PlaylistResult.Failure)
        assertTrue(PlaylistParser.parse("""{"user_info":{"auth":0,"status":"Active"}}""") is PlaylistResult.Failure)
        assertTrue(PlaylistParser.parse("""{"user_info":{"auth":1.5,"status":"Active"}}""") is PlaylistResult.Failure)
        assertTrue(PlaylistParser.parse("""{"user_info":{"auth":1,"status":"Unknown"}}""") is PlaylistResult.Failure)
        val expired = PlaylistParser.parse("""{"user_info":{"auth":1,"status":"Expired"}}""") as PlaylistResult.Ready
        assertEquals(PlaylistStatus.Expired, expired.info.status)
    }

    @Test fun completionAfterSwitchRemovalOrCredentialReplacementIsDiscarded() = runBlocking {
        for (replacement in listOf(account("b", 1), account("a", 2), null)) {
            val store = Store(account("a", 1))
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val repository = PlaylistRepository(store) { started.complete(Unit); release.await(); ready() }
            val pending = async { repository.read() }
            started.await()
            store.value = replacement
            release.complete(Unit)
            assertEquals(PlaylistState.Stale, pending.await())
        }
    }

    @Test fun olderRefreshCannotReplaceNewerResultAndDisplayExcludesPassword() = runBlocking {
        val store = Store(account("a", 1))
        val firstStarted = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var calls = 0
        val repository = PlaylistRepository(store) {
            if (++calls == 1) { firstStarted.complete(Unit); release.await() }
            ready()
        }
        val old = async { repository.read() }
        firstStarted.await()
        val current = repository.read() as PlaylistState.Ready
        assertEquals("https://provider.example", current.providerOrigin)
        assertFalse(current.toString().contains("fixture-secret"))
        release.complete(Unit)
        assertEquals(PlaylistState.Stale, old.await())
    }

    private fun ready() = PlaylistResult.Ready(PlaylistInfo(PlaylistStatus.Active, null, null, null, null))
    private fun account(id: String, generation: Long) = SavedXtreamAccount(id, generation,
        ProviderEndpoint("https://provider.example/panel", false), "fixture-user", "fixture-secret", false)

    private class Store(@Volatile var value: SavedXtreamAccount?) : XtreamAccountStore {
        override fun load() = value
        override fun save(account: SavedXtreamAccount) { value = account }
        override fun clear() { value = null }
    }
}
