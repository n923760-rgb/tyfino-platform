package dev.tyfino.foundation.xtream

internal data class XtreamInput(
    val host: String,
    val username: String,
    val password: String,
)

internal data class ProviderEndpoint(
    val baseUrl: String,
    val isCleartext: Boolean,
)

internal data class SavedXtreamAccount(
    val accountId: String,
    val generation: Long,
    val endpoint: ProviderEndpoint,
    val username: String,
    val password: String,
    val cleartextConsent: Boolean,
)

internal data class XtreamAccountSummary(
    val accountId: String,
    val providerOrigin: String,
    val username: String,
)

internal enum class XtreamFailure {
    InvalidHost,
    NetworkUnavailable,
    Timeout,
    ProviderUnavailable,
    InvalidCredentials,
    AccountExpired,
    AccountDisabled,
    MalformedResponse,
    UnsupportedResponse,
}

internal sealed interface XtreamAuthResult {
    data object Success : XtreamAuthResult
    data class Failure(val reason: XtreamFailure) : XtreamAuthResult
}

internal sealed interface XtreamOutcome {
    data class Authenticated(val account: XtreamAccountSummary) : XtreamOutcome
    data class Failure(val reason: XtreamFailure) : XtreamOutcome
    data object Stale : XtreamOutcome
}

internal sealed interface XtreamUiState {
    data object Loading : XtreamUiState
    data class SignedOut(val error: XtreamFailure? = null) : XtreamUiState
    data class ConfirmCleartext(val providerOrigin: String) : XtreamUiState
    data object Working : XtreamUiState
    data class SignedIn(val account: XtreamAccountSummary) : XtreamUiState
}

internal data class XtreamOperationOwner(
    val accountId: String,
    val generation: Long,
    val operationId: Long,
    val destinationEpoch: Long,
)

internal class XtreamOperationGate {
    private var generation = 0L
    private var operationId = 0L
    private var destinationEpoch = 0L
    private var candidateAccountId: String? = null
    private var acceptingResults = true

    @Synchronized
    fun restore(accountId: String, restoredGeneration: Long) {
        candidateAccountId = accountId
        generation = maxOf(generation, restoredGeneration)
        operationId++
        acceptingResults = true
    }

    @Synchronized
    fun begin(accountId: String): XtreamOperationOwner {
        acceptingResults = true
        candidateAccountId = accountId
        operationId++
        return XtreamOperationOwner(accountId, generation, operationId, destinationEpoch)
    }

    @Synchronized
    fun isCurrent(owner: XtreamOperationOwner): Boolean =
        acceptingResults &&
            candidateAccountId == owner.accountId &&
            generation == owner.generation &&
            operationId == owner.operationId &&
            destinationEpoch == owner.destinationEpoch

    @Synchronized
    fun commit(owner: XtreamOperationOwner, committedGeneration: Long): Boolean {
        if (!isCurrent(owner)) return false
        generation = committedGeneration
        candidateAccountId = owner.accountId
        return true
    }

    @Synchronized
    fun invalidateAccount() {
        generation++
        operationId++
        candidateAccountId = null
    }

    @Synchronized
    fun deactivateDestination() {
        acceptingResults = false
        destinationEpoch++
        operationId++
    }
}
