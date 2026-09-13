package dev.tyfino.foundation.xtream

import java.net.URI
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class XtreamRepository(
    private val store: XtreamRepositoryStore,
    private val api: XtreamApi,
    private val gate: XtreamOperationGate = XtreamOperationGate(),
) {
    private val commitMutex = Mutex()

    suspend fun load(): XtreamAccountSummary? = withContext(Dispatchers.IO) {
        commitMutex.withLock {
            store.load()?.also { gate.restore(it.accountId, it.generation) }?.summary()
        }
    }

    suspend fun accountSnapshot(): XtreamAccountsSnapshot = withContext(Dispatchers.IO) {
        commitMutex.withLock {
            val portfolio = store.loadPortfolio()
            XtreamAccountsSnapshot(
                activeAccountId = portfolio.activeAccountId,
                accounts = portfolio.accounts.map { it.summary() },
            )
        }
    }

    suspend fun switchAccount(accountId: String): XtreamSwitchResult = withContext(Dispatchers.IO) {
        commitMutex.withLock {
            val current = store.loadPortfolio()
            val target = current.accounts.firstOrNull { it.accountId == accountId }
                ?: return@withLock XtreamSwitchResult.NotFound
            if (current.activeAccountId == accountId) {
                return@withLock XtreamSwitchResult.AlreadyActive(target.summary())
            }
            val next = requireNotNull(current.activate(accountId))
            gate.invalidateAccount()
            try {
                store.savePortfolio(next)
            } catch (_: Exception) {
                current.activeAccount?.let { gate.restore(it.accountId, it.generation) }
                    ?: gate.invalidateAccount()
                return@withLock XtreamSwitchResult.LocalStorage
            }
            gate.restore(target.accountId, target.generation)
            XtreamSwitchResult.Switched(target.summary())
        }
    }

    suspend fun authenticate(
        endpoint: ProviderEndpoint,
        username: String,
        password: String,
        cleartextConsent: Boolean,
    ): XtreamOutcome {
        if (endpoint.isCleartext && !cleartextConsent) {
            return XtreamOutcome.Failure(XtreamFailure.InvalidHost)
        }
        val prepared = withContext(Dispatchers.IO) {
            commitMutex.withLock {
                val portfolio = store.loadPortfolio()
                val duplicate = portfolio.accounts.firstOrNull {
                    it.endpoint.baseUrl == endpoint.baseUrl && it.username == username
                }
                if (duplicate == null && portfolio.accounts.size == XtreamAccountPortfolio.MAX_ACCOUNTS) {
                    return@withLock null
                }
                val accountId = duplicate?.accountId ?: UUID.randomUUID().toString().replace("-", "")
                PreparedLogin(
                    owner = gate.begin(accountId),
                    currentGeneration = duplicate?.generation ?: 0L,
                    portfolio = portfolio,
                )
            }
        } ?: return XtreamOutcome.Failure(XtreamFailure.AccountLimitReached)

        val result = api.authenticate(endpoint, username, password)
        return withContext(Dispatchers.IO) {
            commitMutex.withLock {
                if (!gate.isCurrent(prepared.owner)) return@withLock XtreamOutcome.Stale
                when (result) {
                    XtreamAuthResult.Success -> {
                        val generationFloor = maxOf(
                            prepared.owner.generation,
                            prepared.currentGeneration,
                        )
                        if (generationFloor == Long.MAX_VALUE) {
                            return@withLock XtreamOutcome.Failure(XtreamFailure.LocalStorage)
                        }
                        val nextGeneration = generationFloor + 1L
                        val account = SavedXtreamAccount(
                            accountId = prepared.owner.accountId,
                            generation = nextGeneration,
                            endpoint = endpoint,
                            username = username,
                            password = password,
                            cleartextConsent = cleartextConsent,
                        )
                        val mutation = prepared.portfolio.upsertAuthenticated(account)
                        val nextPortfolio = when (mutation) {
                            is XtreamPortfolioUpsert.Added -> mutation.portfolio
                            is XtreamPortfolioUpsert.Updated -> mutation.portfolio
                            XtreamPortfolioUpsert.LimitReached -> {
                                return@withLock XtreamOutcome.Failure(XtreamFailure.AccountLimitReached)
                            }
                            XtreamPortfolioUpsert.Invalid -> {
                                return@withLock XtreamOutcome.Failure(XtreamFailure.LocalStorage)
                            }
                        }
                        val committedAccount = requireNotNull(nextPortfolio.activeAccount)
                        val committed = try {
                            gate.commit(prepared.owner, committedAccount.generation) {
                                store.savePortfolio(nextPortfolio)
                            }
                        } catch (_: Exception) {
                            prepared.portfolio.activeAccount?.let {
                                gate.restore(it.accountId, it.generation)
                            } ?: gate.invalidateAccount()
                            return@withLock XtreamOutcome.Failure(XtreamFailure.LocalStorage)
                        }
                        if (!committed) {
                            return@withLock XtreamOutcome.Stale
                        }
                        XtreamOutcome.Authenticated(committedAccount.summary())
                    }
                    is XtreamAuthResult.Failure -> XtreamOutcome.Failure(result.reason)
                }
            }
        }
    }

    suspend fun logout() {
        gate.invalidateAccount()
        withContext(Dispatchers.IO) {
            commitMutex.withLock {
                val portfolio = store.loadPortfolio()
                val activeAccountId = portfolio.activeAccountId ?: return@withLock
                val remaining = requireNotNull(portfolio.remove(activeAccountId))
                store.savePortfolio(remaining)
            }
        }
    }

    fun deactivateDestination() = gate.deactivateDestination()

    private fun SavedXtreamAccount.summary() = XtreamAccountSummary(
        accountId = accountId,
        providerOrigin = URI(endpoint.baseUrl).let { uri ->
            URI(uri.scheme, null, uri.host, uri.port, null, null, null).toASCIIString()
        },
        username = username,
    )

    private data class PreparedLogin(
        val owner: XtreamOperationOwner,
        val currentGeneration: Long,
        val portfolio: XtreamAccountPortfolio,
    )
}

internal class XtreamController(private val repository: XtreamRepository) {
    var state: XtreamUiState = XtreamUiState.Loading
        private set
    private var pendingCleartext: Pair<ProviderEndpoint, XtreamInput>? = null

    suspend fun initialize(publish: (XtreamUiState) -> Unit) {
        val account = repository.load()
        setState(
            account?.let(XtreamUiState::SignedIn) ?: XtreamUiState.SignedOut(),
            publish,
        )
    }

    suspend fun signIn(input: XtreamInput, publish: (XtreamUiState) -> Unit) {
        if (input.username.isBlank() || input.password.isEmpty()) {
            setState(XtreamUiState.SignedOut(XtreamFailure.InvalidCredentials), publish)
            return
        }
        val endpoint = when (val parsed = XtreamHostCanonicalizer.parse(input.host)) {
            HostParseResult.Invalid -> {
                setState(XtreamUiState.SignedOut(XtreamFailure.InvalidHost), publish)
                return
            }
            is HostParseResult.Valid -> parsed.endpoint
        }
        if (endpoint.isCleartext) {
            pendingCleartext = endpoint to input
            setState(
                XtreamUiState.ConfirmCleartext(safeOrigin(endpoint.baseUrl)),
                publish,
            )
            return
        }
        authenticate(endpoint, input, cleartextConsent = false, publish)
    }

    suspend fun confirmCleartext(publish: (XtreamUiState) -> Unit) {
        val pending = pendingCleartext ?: return
        pendingCleartext = null
        authenticate(pending.first, pending.second, cleartextConsent = true, publish)
    }

    fun cancelCleartext(publish: (XtreamUiState) -> Unit) {
        pendingCleartext = null
        setState(XtreamUiState.SignedOut(), publish)
    }

    suspend fun logout(publish: (XtreamUiState) -> Unit) {
        pendingCleartext = null
        repository.logout()
        setState(XtreamUiState.SignedOut(), publish)
    }

    fun deactivate() {
        pendingCleartext = null
        repository.deactivateDestination()
    }

    private suspend fun authenticate(
        endpoint: ProviderEndpoint,
        input: XtreamInput,
        cleartextConsent: Boolean,
        publish: (XtreamUiState) -> Unit,
    ) {
        setState(XtreamUiState.Working, publish)
        when (
            val outcome = repository.authenticate(
                endpoint = endpoint,
                username = input.username,
                password = input.password,
                cleartextConsent = cleartextConsent,
            )
        ) {
            is XtreamOutcome.Authenticated -> setState(
                XtreamUiState.SignedIn(outcome.account),
                publish,
            )
            is XtreamOutcome.Failure -> setState(
                XtreamUiState.SignedOut(outcome.reason),
                publish,
            )
            XtreamOutcome.Stale -> Unit
        }
    }

    private fun setState(value: XtreamUiState, publish: (XtreamUiState) -> Unit) {
        state = value
        publish(value)
    }

    private fun safeOrigin(baseUrl: String): String = URI(baseUrl).let { uri ->
        URI(uri.scheme, null, uri.host, uri.port, null, null, null).toASCIIString()
    }
}
