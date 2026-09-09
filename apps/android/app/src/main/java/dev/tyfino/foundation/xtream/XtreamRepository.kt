package dev.tyfino.foundation.xtream

import java.net.URI
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class XtreamRepository(
    private val store: XtreamAccountStore,
    private val api: XtreamApi,
    private val gate: XtreamOperationGate = XtreamOperationGate(),
) {
    private val commitMutex = Mutex()

    suspend fun load(): XtreamAccountSummary? = withContext(Dispatchers.IO) {
        commitMutex.withLock {
            store.load()?.also { gate.restore(it.accountId, it.generation) }?.summary()
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
                val current = store.load()
                val accountId = if (
                    current?.endpoint?.baseUrl == endpoint.baseUrl &&
                    current.username == username
                ) {
                    current.accountId
                } else {
                    UUID.randomUUID().toString().replace("-", "")
                }
                PreparedLogin(
                    owner = gate.begin(accountId),
                    currentGeneration = current?.generation ?: 0L,
                )
            }
        }

        val result = api.authenticate(endpoint, username, password)
        return withContext(Dispatchers.IO) {
            commitMutex.withLock {
                if (!gate.isCurrent(prepared.owner)) return@withLock XtreamOutcome.Stale
                when (result) {
                    XtreamAuthResult.Success -> {
                        val nextGeneration = maxOf(
                            prepared.owner.generation,
                            prepared.currentGeneration,
                        ) + 1
                        val account = SavedXtreamAccount(
                            accountId = prepared.owner.accountId,
                            generation = nextGeneration,
                            endpoint = endpoint,
                            username = username,
                            password = password,
                            cleartextConsent = cleartextConsent,
                        )
                        store.save(account)
                        if (!gate.commit(prepared.owner, nextGeneration)) {
                            return@withLock XtreamOutcome.Stale
                        }
                        XtreamOutcome.Authenticated(account.summary())
                    }
                    is XtreamAuthResult.Failure -> XtreamOutcome.Failure(result.reason)
                }
            }
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        commitMutex.withLock {
            gate.invalidateAccount()
            store.clear()
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
