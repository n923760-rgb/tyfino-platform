package dev.tyfino.foundation.xtream

import java.util.concurrent.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XtreamTest {
    @Test
    fun canonicalizerAcceptsApprovedHostsAndRejectsSecretBearingOrAmbiguousInput() {
        assertEndpoint(
            "HTTPS://Example.COM:443/panel/",
            "https://example.com/panel",
            cleartext = false,
        )
        assertEndpoint(
            " http://example.com:8080 ",
            "http://example.com:8080",
            cleartext = true,
        )
        assertEndpoint(
            "https://مثال.إختبار/panel",
            "https://xn--mgbh0fb.xn--kgbechtv/panel",
            cleartext = false,
        )
        listOf(
            "",
            "example.com",
            "ftp://example.com",
            "https://user:secret@example.com",
            "https://example.com?username=secret",
            "https://example.com/#fragment",
            "https://example.com/a/../b",
            "https://example.com/%2e%2e/b",
        ).forEach { assertEquals(HostParseResult.Invalid, XtreamHostCanonicalizer.parse(it)) }
    }

    @Test
    fun responseClassifierPreservesEveryProviderCategory() {
        assertEquals(XtreamAuthResult.Success, classify("""{"user_info":{"auth":1,"status":"Active"}}"""))
        assertFailure(XtreamFailure.InvalidCredentials, """{"user_info":{"auth":"0","status":"Active"}}""")
        assertFailure(XtreamFailure.AccountExpired, """{"user_info":{"auth":1,"status":"Expired"}}""")
        assertFailure(XtreamFailure.AccountDisabled, """{"user_info":{"auth":1,"status":"Banned"}}""")
        assertFailure(XtreamFailure.MalformedResponse, """{}""")
        assertFailure(XtreamFailure.MalformedResponse, """{"user_info":{"auth":1}}""")
        assertFailure(XtreamFailure.UnsupportedResponse, """{"user_info":{"auth":true,"status":"Active"}}""")
        assertFailure(XtreamFailure.UnsupportedResponse, """{"user_info":{"auth":1,"status":"Unknown"}}""")
        assertEquals(
            XtreamAuthResult.Failure(XtreamFailure.InvalidCredentials),
            XtreamResponseClassifier.classify(401, ""),
        )
    }

    @Test
    fun newerLoginRejectsOlderCompletionAndCommitsOnlyLatestCredentials() = runBlocking {
        val store = FakeStore()
        val api = ControllableApi()
        val repository = XtreamRepository(store, api)
        val endpoint = endpoint("https://one.example")

        val old = async { repository.authenticate(endpoint, "same", "old", false) }
        api.firstStarted.await()
        val latest = async { repository.authenticate(endpoint, "same", "new", false) }
        assertTrue(latest.await() is XtreamOutcome.Authenticated)
        api.releaseFirst.complete(Unit)

        assertEquals(XtreamOutcome.Stale, old.await())
        assertEquals("new", store.value?.password)
    }

    @Test
    fun logoutInvalidatesPendingCompletionBeforeCredentialRemoval() = runBlocking {
        val store = FakeStore()
        val api = ControllableApi()
        val repository = XtreamRepository(store, api)
        val pending = async {
            repository.authenticate(endpoint("https://one.example"), "user", "password", false)
        }
        api.firstStarted.await()
        repository.logout()
        api.releaseFirst.complete(Unit)

        assertEquals(XtreamOutcome.Stale, pending.await())
        assertNull(store.value)
    }

    @Test
    fun equalUsernameOnDifferentHostsReceivesDifferentAccountIdentity() = runBlocking {
        val store = FakeStore()
        val repository = XtreamRepository(store, AlwaysSuccessfulApi)
        val first = repository.authenticate(
            endpoint("https://one.example"),
            "same",
            "password",
            false,
        ) as XtreamOutcome.Authenticated
        val second = repository.authenticate(
            endpoint("https://two.example"),
            "same",
            "password",
            false,
        ) as XtreamOutcome.Authenticated

        assertNotEquals(first.account.accountId, second.account.accountId)
        assertEquals(second.account.accountId, store.value?.accountId)
    }

    @Test
    fun failedReplacementDoesNotOverwriteSavedAccount() = runBlocking {
        val store = FakeStore()
        val initialRepository = XtreamRepository(store, AlwaysSuccessfulApi)
        val initial = initialRepository.authenticate(
            endpoint("https://one.example"),
            "user",
            "password",
            false,
        ) as XtreamOutcome.Authenticated
        val failing = XtreamRepository(
            store,
            FixedApi(XtreamAuthResult.Failure(XtreamFailure.InvalidCredentials)),
        )

        val outcome = failing.authenticate(
            endpoint("https://two.example"),
            "user",
            "wrong",
            false,
        )

        assertEquals(XtreamOutcome.Failure(XtreamFailure.InvalidCredentials), outcome)
        assertEquals(initial.account.accountId, store.value?.accountId)
    }

    @Test
    fun cleartextCannotBypassExplicitConsent() = runBlocking {
        val store = FakeStore()
        val outcome = XtreamRepository(store, AlwaysSuccessfulApi).authenticate(
            endpoint("http://provider.example"),
            "user",
            "password",
            cleartextConsent = false,
        )

        assertEquals(XtreamOutcome.Failure(XtreamFailure.InvalidHost), outcome)
        assertNull(store.value)
    }

    @Test(expected = CancellationException::class)
    fun cancellationPropagatesInsteadOfBecomingAuthenticationFailure() = runBlocking {
        XtreamRepository(FakeStore(), CancellingApi).authenticate(
            endpoint("https://one.example"),
            "user",
            "password",
            false,
        )
        Unit
    }

    @Test
    fun operationGateRequiresAccountGenerationOperationAndDestinationOwnership() {
        val gate = XtreamOperationGate()
        val first = gate.begin("account-a")
        val second = gate.begin("account-b")
        assertFalse(gate.isCurrent(first))
        assertTrue(gate.isCurrent(second))
        gate.invalidateAccount()
        assertFalse(gate.isCurrent(second))
        val third = gate.begin("account-a")
        gate.deactivateDestination()
        assertFalse(gate.isCurrent(third))
    }

    private fun assertEndpoint(input: String, expected: String, cleartext: Boolean) {
        val parsed = XtreamHostCanonicalizer.parse(input) as HostParseResult.Valid
        assertEquals(expected, parsed.endpoint.baseUrl)
        assertEquals(cleartext, parsed.endpoint.isCleartext)
    }

    private fun classify(body: String) = XtreamResponseClassifier.classify(200, body)

    private fun assertFailure(expected: XtreamFailure, body: String) {
        assertEquals(XtreamAuthResult.Failure(expected), classify(body))
    }

    private fun endpoint(value: String) =
        (XtreamHostCanonicalizer.parse(value) as HostParseResult.Valid).endpoint

    private class FakeStore : XtreamAccountStore {
        var value: SavedXtreamAccount? = null
        override fun load() = value
        override fun save(account: SavedXtreamAccount) {
            value = account
        }
        override fun clear() {
            value = null
        }
    }

    private class FixedApi(private val result: XtreamAuthResult) : XtreamApi {
        override suspend fun authenticate(
            endpoint: ProviderEndpoint,
            username: String,
            password: String,
        ) = result
    }

    private object AlwaysSuccessfulApi : XtreamApi {
        override suspend fun authenticate(
            endpoint: ProviderEndpoint,
            username: String,
            password: String,
        ) = XtreamAuthResult.Success
    }

    private object CancellingApi : XtreamApi {
        override suspend fun authenticate(
            endpoint: ProviderEndpoint,
            username: String,
            password: String,
        ): XtreamAuthResult = throw CancellationException("cancelled")
    }

    private class ControllableApi : XtreamApi {
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        private var calls = 0

        override suspend fun authenticate(
            endpoint: ProviderEndpoint,
            username: String,
            password: String,
        ): XtreamAuthResult {
            calls++
            if (calls == 1) {
                firstStarted.complete(Unit)
                releaseFirst.await()
            }
            return XtreamAuthResult.Success
        }
    }
}
