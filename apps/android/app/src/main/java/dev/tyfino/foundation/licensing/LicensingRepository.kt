package dev.tyfino.foundation.licensing

import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.random.Random

internal class LicensingRepository(
    private val store: LicensingStore,
    private val api: LicensingApi,
    private val clock: LicenseClock,
    private val appVersion: String,
) {
    private val operations = OperationGate()
    private val commitMutex = Mutex()

    fun localOutcome(): LicenseOutcome {
        val snapshot = store.snapshot() ?: return LicenseOutcome.Choice
        return if (OfflinePolicy.canUse(snapshot, clock)) {
            LicenseOutcome.Active(snapshot, offline = true)
        } else {
            LicenseOutcome.Failure("SESSION_INVALID", retryable = true)
        }
    }

    fun shouldRefresh(): Boolean = store.snapshot()?.let { OfflinePolicy.refreshDue(it, clock) } ?: false

    suspend fun startTrial(): LicenseOutcome = perform { owner ->
        api.startTrial(owner.installationId, appVersion)
    }

    suspend fun activate(activationCode: String): LicenseOutcome = perform { owner ->
        api.activate(owner.installationId, appVersion, activationCode)
    }

    suspend fun refresh(): LicenseOutcome {
        val snapshot = store.snapshot() ?: return LicenseOutcome.Choice
        return perform { owner -> api.refresh(owner.installationId, appVersion, snapshot.sessionToken) }
    }

    private suspend fun perform(request: suspend (OperationOwner) -> LicenseEnvelope): LicenseOutcome {
        val owner = operations.begin(store.installationId())
        var lastFailure = LicensingApiException("LICENSE_UNAVAILABLE", true)
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                val envelope = request(owner)
                return commitMutex.withLock {
                    if (!operations.isCurrent(owner, store.installationId())) return@withLock LicenseOutcome.Stale
                    val snapshot = EntitlementSnapshot(
                        kind = envelope.entitlement.kind,
                        startsAtMillis = envelope.entitlement.startsAtMillis,
                        expiresAtMillis = envelope.entitlement.expiresAtMillis,
                        offlineValidUntilMillis = envelope.entitlement.offlineValidUntilMillis,
                        serverTimeMillis = envelope.serverTimeMillis,
                        refreshAfterMillis = envelope.refreshAfterMillis,
                        sessionToken = envelope.sessionToken,
                        verifiedElapsedRealtimeMillis = clock.elapsedRealtimeMillis(),
                        verifiedBootCount = clock.bootCount(),
                    )
                    store.save(snapshot)
                    LicenseOutcome.Active(snapshot, offline = false)
                }
            } catch (failure: LicensingApiException) {
                lastFailure = failure
                if (!failure.retryable || attempt == MAX_ATTEMPTS - 1) return handleFailure(owner, failure)
                delay(RETRY_DELAYS_MILLIS[attempt] + Random.nextLong(0, RETRY_JITTER_MILLIS))
            }
        }
        return handleFailure(owner, lastFailure)
    }

    private suspend fun handleFailure(owner: OperationOwner, failure: LicensingApiException): LicenseOutcome =
        commitMutex.withLock {
            if (!operations.isCurrent(owner, store.installationId())) return@withLock LicenseOutcome.Stale
            if (failure.code in TERMINAL_CODES) store.clearEntitlement()
            val local = store.snapshot()
            if (failure.retryable && local != null && OfflinePolicy.canUse(local, clock)) {
                LicenseOutcome.Active(local, offline = true)
            } else {
                LicenseOutcome.Failure(failure.code, failure.retryable)
            }
        }

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val RETRY_JITTER_MILLIS = 250L
        val RETRY_DELAYS_MILLIS = longArrayOf(500L, 1_500L)
        val TERMINAL_CODES = setOf("SESSION_INVALID", "ENTITLEMENT_EXPIRED", "ENTITLEMENT_REVOKED")
    }
}

internal class LicensingController(private val repository: LicensingRepository) {
    private enum class RetryTarget { Trial, Refresh, ActivationEntry }

    private val actionMutex = Mutex()
    private var retryTarget = RetryTarget.Refresh
    var state: LicensingUiState = LicensingUiState.Checking
        private set

    suspend fun initialize(onState: (LicensingUiState) -> Unit) = actionMutex.withLock {
        apply(withContext(Dispatchers.IO) { repository.localOutcome() }, onState)
        if (withContext(Dispatchers.IO) { repository.shouldRefresh() }) {
            retryTarget = RetryTarget.Refresh
            apply(withContext(Dispatchers.IO) { repository.refresh() }, onState)
        }
    }

    fun showActivation(onState: (LicensingUiState) -> Unit) {
        state = LicensingUiState.ActivationEntry
        onState(state)
    }

    fun showChoice(onState: (LicensingUiState) -> Unit) {
        state = LicensingUiState.Choice
        onState(state)
    }

    suspend fun startTrial(onState: (LicensingUiState) -> Unit) = actionMutex.withLock {
        retryTarget = RetryTarget.Trial
        state = LicensingUiState.Working
        onState(state)
        apply(withContext(Dispatchers.IO) { repository.startTrial() }, onState)
    }

    suspend fun activate(code: String, onState: (LicensingUiState) -> Unit) = actionMutex.withLock {
        retryTarget = RetryTarget.ActivationEntry
        state = LicensingUiState.Working
        onState(state)
        apply(withContext(Dispatchers.IO) { repository.activate(code) }, onState)
    }

    suspend fun retry(onState: (LicensingUiState) -> Unit) = actionMutex.withLock {
        if (retryTarget == RetryTarget.ActivationEntry) {
            state = LicensingUiState.ActivationEntry
            onState(state)
            return@withLock
        }
        state = LicensingUiState.Working
        onState(state)
        val outcome = when (retryTarget) {
            RetryTarget.Trial -> withContext(Dispatchers.IO) { repository.startTrial() }
            RetryTarget.Refresh -> withContext(Dispatchers.IO) { repository.refresh() }
            RetryTarget.ActivationEntry -> return@withLock
        }
        apply(outcome, onState)
    }

    suspend fun onForeground(onState: (LicensingUiState) -> Unit) = actionMutex.withLock {
        if (withContext(Dispatchers.IO) { repository.shouldRefresh() }) {
            retryTarget = RetryTarget.Refresh
            apply(withContext(Dispatchers.IO) { repository.refresh() }, onState)
        }
    }

    private fun apply(outcome: LicenseOutcome, onState: (LicensingUiState) -> Unit) {
        state = when (outcome) {
            LicenseOutcome.Choice -> LicensingUiState.Choice
            is LicenseOutcome.Active -> LicensingUiState.Active(outcome.entitlement, outcome.offline)
            is LicenseOutcome.Failure -> LicensingUiState.Failure(outcome.code, outcome.retryable)
            LicenseOutcome.Stale -> return
        }
        onState(state)
    }
}
