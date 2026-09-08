package dev.tyfino.foundation.licensing

internal enum class EntitlementKind { Trial, OneYear, Lifetime }

internal data class EntitlementSnapshot(
    val kind: EntitlementKind,
    val startsAtMillis: Long,
    val expiresAtMillis: Long?,
    val offlineValidUntilMillis: Long,
    val serverTimeMillis: Long,
    val refreshAfterMillis: Long,
    val sessionToken: String,
    val verifiedElapsedRealtimeMillis: Long,
    val verifiedBootCount: Int,
)

internal data class LicenseEnvelope(
    val serverTimeMillis: Long,
    val entitlement: EntitlementPayload,
    val sessionToken: String,
    val refreshAfterMillis: Long,
)

internal data class EntitlementPayload(
    val kind: EntitlementKind,
    val startsAtMillis: Long,
    val expiresAtMillis: Long?,
    val offlineValidUntilMillis: Long,
)

internal sealed interface LicenseOutcome {
    data object Choice : LicenseOutcome
    data class Active(val entitlement: EntitlementSnapshot, val offline: Boolean) : LicenseOutcome
    data class Failure(val code: String, val retryable: Boolean) : LicenseOutcome
    data object Stale : LicenseOutcome
}

internal sealed interface LicensingUiState {
    data object Checking : LicensingUiState
    data object Choice : LicensingUiState
    data object ActivationEntry : LicensingUiState
    data object Working : LicensingUiState
    data class Active(val entitlement: EntitlementSnapshot, val offline: Boolean) : LicensingUiState
    data class Failure(val code: String, val retryable: Boolean) : LicensingUiState
}

internal data class OperationOwner(val installationId: String, val generation: Long)

internal class OperationGate {
    private var generation = 0L

    @Synchronized
    fun begin(installationId: String): OperationOwner = OperationOwner(installationId, ++generation)

    @Synchronized
    fun isCurrent(owner: OperationOwner, currentInstallationId: String): Boolean =
        owner.generation == generation && owner.installationId == currentInstallationId
}

internal fun normalizeActivationCode(value: String): String? {
    val compact = value.uppercase().replace(Regex("[\\s-]"), "")
    if (!Regex("^TYF[0-9A-HJKMNP-TV-Z]{26}$").matches(compact)) return null
    val raw = compact.drop(3)
    return "TYF-${raw.substring(0, 5)}-${raw.substring(5, 10)}-${raw.substring(10, 15)}-" +
        "${raw.substring(15, 20)}-${raw.substring(20)}"
}

internal interface LicenseClock {
    fun wallTimeMillis(): Long
    fun elapsedRealtimeMillis(): Long
    fun bootCount(): Int
}

internal object OfflinePolicy {
    fun canUse(snapshot: EntitlementSnapshot, clock: LicenseClock): Boolean {
        if (snapshot.verifiedBootCount < 0 || snapshot.verifiedBootCount != clock.bootCount()) return false
        val elapsed = (clock.elapsedRealtimeMillis() - snapshot.verifiedElapsedRealtimeMillis).coerceAtLeast(0L)
        val authoritativeNow = maxOf(clock.wallTimeMillis(), snapshot.serverTimeMillis + elapsed)
        val finiteExpiry = snapshot.expiresAtMillis ?: Long.MAX_VALUE
        return authoritativeNow < minOf(snapshot.offlineValidUntilMillis, finiteExpiry)
    }

    fun refreshDue(snapshot: EntitlementSnapshot, clock: LicenseClock): Boolean {
        if (snapshot.verifiedBootCount < 0 || snapshot.verifiedBootCount != clock.bootCount()) return true
        val elapsed = (clock.elapsedRealtimeMillis() - snapshot.verifiedElapsedRealtimeMillis).coerceAtLeast(0L)
        return maxOf(clock.wallTimeMillis(), snapshot.serverTimeMillis + elapsed) >= snapshot.refreshAfterMillis
    }
}
