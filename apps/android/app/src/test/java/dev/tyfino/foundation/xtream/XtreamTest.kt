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
        assertEquals(2, store.portfolio.accounts.size)
    }

    @Test
    fun duplicateLoginUpdatesCredentialsAndPreservesStableIdentity() = runBlocking {
        val store = FakeStore()
        val repository = XtreamRepository(store, AlwaysSuccessfulApi)
        val endpoint = endpoint("https://one.example")
        val first = repository.authenticate(endpoint, "user", "old", false) as XtreamOutcome.Authenticated

        val updated = repository.authenticate(endpoint, "user", "new", false) as XtreamOutcome.Authenticated

        assertEquals(first.account.accountId, updated.account.accountId)
        assertEquals("new", store.value?.password)
        assertEquals(2L, store.value?.generation)
        assertEquals(1, store.portfolio.accounts.size)
    }

    @Test
    fun ninthDistinctLoginIsRejectedBeforeProviderRequestWithoutEviction() = runBlocking {
        val accounts = (1..XtreamAccountPortfolio.MAX_ACCOUNTS).map { index -> account(index) }
        val store = FakeStore().apply {
            portfolio = requireNotNull(XtreamAccountPortfolio.create(accounts, accounts.first().accountId))
        }
        val api = CountingApi()

        val outcome = XtreamRepository(store, api).authenticate(
            endpoint("https://provider-9.example"),
            "user-9",
            "password-9",
            false,
        )

        assertEquals(XtreamOutcome.Failure(XtreamFailure.AccountLimitReached), outcome)
        assertEquals(0, api.calls)
        assertEquals(accounts, store.portfolio.accounts)
    }

    @Test
    fun failedPortfolioPersistenceKeepsPreviousActiveAccount() = runBlocking {
        val store = FakeStore()
        val repository = XtreamRepository(store, AlwaysSuccessfulApi)
        repository.authenticate(endpoint("https://one.example"), "user", "password", false)
        val previous = store.portfolio
        store.failNextPortfolioSave = true

        val outcome = repository.authenticate(endpoint("https://two.example"), "user", "password", false)

        assertEquals(XtreamOutcome.Failure(XtreamFailure.LocalStorage), outcome)
        assertEquals(previous, store.portfolio)
    }

    @Test
    fun logoutRemovesOnlyActiveAccountAndLeavesNoAutomaticSelection() = runBlocking {
        val first = account(1)
        val second = account(2)
        val store = FakeStore().apply {
            portfolio = requireNotNull(XtreamAccountPortfolio.create(listOf(first, second), first.accountId))
        }

        XtreamRepository(store, AlwaysSuccessfulApi).logout()

        assertEquals(listOf(second), store.portfolio.accounts)
        assertNull(store.portfolio.activeAccountId)
    }

    @Test
    fun accountSnapshotExposesSafeSummariesInPortfolioOrder() = runBlocking {
        val first = account(1)
        val second = account(2)
        val store = FakeStore().apply {
            portfolio = requireNotNull(XtreamAccountPortfolio.create(listOf(first, second), second.accountId))
        }

        val snapshot = XtreamRepository(store, CountingApi()).accountSnapshot()

        assertEquals(second.accountId, snapshot.activeAccountId)
        assertEquals(listOf(second.accountId, first.accountId), snapshot.accounts.map { it.accountId })
        assertEquals("https://provider-2.example", snapshot.accounts.first().providerOrigin)
        assertEquals("user-2", snapshot.accounts.first().username)
    }

    @Test
    fun switchPersistsTargetBeforeExposureWithoutProviderRequest() = runBlocking {
        val first = account(1)
        val second = account(2)
        val store = FakeStore().apply {
            portfolio = requireNotNull(XtreamAccountPortfolio.create(listOf(first, second), first.accountId))
        }
        val api = CountingApi()
        val repository = XtreamRepository(store, api)

        val result = repository.switchAccount(second.accountId)

        assertEquals(XtreamSwitchResult.Switched(second.summary()), result)
        assertEquals(second.accountId, store.portfolio.activeAccountId)
        assertEquals(listOf(second, first), store.portfolio.accounts)
        assertEquals(0, api.calls)
        assertEquals(second.summary(), XtreamRepository(store, api).load())
    }

    @Test
    fun switchRejectsUnknownAccountWithoutMutationOrProviderRequest() = runBlocking {
        val first = account(1)
        val store = FakeStore().apply {
            portfolio = requireNotNull(XtreamAccountPortfolio.single(first))
        }
        val previous = store.portfolio
        val api = CountingApi()

        val result = XtreamRepository(store, api).switchAccount("missing-account-id")

        assertEquals(XtreamSwitchResult.NotFound, result)
        assertEquals(previous, store.portfolio)
        assertEquals(0, api.calls)
    }

    @Test
    fun failedSwitchPersistenceRestoresPreviousActiveAccount() = runBlocking {
        val first = account(1)
        val second = account(2)
        val store = FakeStore().apply {
            portfolio = requireNotNull(XtreamAccountPortfolio.create(listOf(first, second), first.accountId))
            failNextPortfolioSave = true
        }
        val repository = XtreamRepository(store, CountingApi())
        repository.load()

        val result = repository.switchAccount(second.accountId)

        assertEquals(XtreamSwitchResult.LocalStorage, result)
        assertEquals(first.accountId, store.portfolio.activeAccountId)
        assertEquals(first.summary(), repository.load())
    }

    @Test
    fun switchMakesPendingAuthenticationCompletionStale() = runBlocking {
        val first = account(1)
        val second = account(2)
        val store = FakeStore().apply {
            portfolio = requireNotNull(XtreamAccountPortfolio.create(listOf(first, second), first.accountId))
        }
        val api = ControllableApi()
        val repository = XtreamRepository(store, api)
        repository.load()
        val pending = async {
            repository.authenticate(first.endpoint, first.username, "replacement", false)
        }
        api.firstStarted.await()

        assertEquals(XtreamSwitchResult.Switched(second.summary()), repository.switchAccount(second.accountId))
        api.releaseFirst.complete(Unit)

        assertEquals(XtreamOutcome.Stale, pending.await())
        assertEquals(second.accountId, store.portfolio.activeAccountId)
        assertEquals(first.password, store.portfolio.accounts.single { it.accountId == first.accountId }.password)
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

    private fun account(number: Int) = SavedXtreamAccount(
        accountId = number.toString().padStart(32, '0'),
        generation = number.toLong(),
        endpoint = endpoint("https://provider-$number.example"),
        username = "user-$number",
        password = "password-$number",
        cleartextConsent = false,
    )

    private fun SavedXtreamAccount.summary() = XtreamAccountSummary(
        accountId = accountId,
        providerOrigin = endpoint.baseUrl,
        username = username,
    )

    private class FakeStore : XtreamRepositoryStore {
        var portfolio = XtreamAccountPortfolio.Empty
        val value: SavedXtreamAccount?
            get() = portfolio.activeAccount
        var failNextPortfolioSave = false

        override fun load() = value
        override fun save(account: SavedXtreamAccount) {
            portfolio = requireNotNull(XtreamAccountPortfolio.single(account))
        }
        override fun clear() {
            portfolio = XtreamAccountPortfolio.Empty
        }
        override fun loadPortfolio() = portfolio
        override fun savePortfolio(portfolio: XtreamAccountPortfolio) {
            if (failNextPortfolioSave) {
                failNextPortfolioSave = false
                throw IllegalStateException("simulated persistence failure")
            }
            this.portfolio = portfolio
        }
        override fun clearPortfolio() = clear()
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

    private class CountingApi : XtreamApi {
        var calls = 0
        override suspend fun authenticate(
            endpoint: ProviderEndpoint,
            username: String,
            password: String,
        ): XtreamAuthResult {
            calls++
            return XtreamAuthResult.Success
        }
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
