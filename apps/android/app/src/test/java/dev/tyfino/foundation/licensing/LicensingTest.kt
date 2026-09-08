package dev.tyfino.foundation.licensing

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LicensingTest {
    @Test
    fun activationCodesNormalizeOnlyCaseWhitespaceAndSeparators() {
        val canonical = "TYF-01234-56789-ABCDE-FGHJK-MNPQRS"
        assertEquals(canonical, normalizeActivationCode(canonical.lowercase().replace('-', ' ')))
        assertNull(normalizeActivationCode("$canonical!"))
        assertNull(normalizeActivationCode("TYF-TOO-SHORT"))
    }

    @Test
    fun operationOwnerRejectsOlderGenerationAndChangedInstallation() {
        val gate = OperationGate()
        val first = gate.begin("installation-a")
        val second = gate.begin("installation-a")
        assertFalse(gate.isCurrent(first, "installation-a"))
        assertTrue(gate.isCurrent(second, "installation-a"))
        assertFalse(gate.isCurrent(second, "installation-b"))
    }

    @Test
    fun offlineAuthorityUsesMonotonicTimeAndFailsClosedAfterReboot() {
        val snapshot = snapshot(offlineUntil = 11_000L, verifiedElapsed = 1_000L, bootCount = 7)
        val clock = FakeClock(wall = 5_000L, elapsed = 3_100L, boot = 7)
        assertTrue(OfflinePolicy.canUse(snapshot, clock))
        clock.elapsed = 12_001L
        assertFalse(OfflinePolicy.canUse(snapshot, clock))
        clock.elapsed = 3_100L
        clock.boot = 8
        assertFalse(OfflinePolicy.canUse(snapshot, clock))
    }

    @Test
    fun staleNetworkResultCannotOverwriteNewerAction() = runBlocking {
        val store = FakeStore()
        val clock = FakeClock(wall = 1_000L, elapsed = 1_000L, boot = 2)
        val api = ControllableApi()
        val repository = LicensingRepository(store, api, clock, "test")

        val old = async { repository.startTrial() }
        api.trialStarted.await()
        val latest = async { repository.activate("TYF-01234-56789-ABCDE-FGHJK-MNPQRS") }
        assertTrue(latest.await() is LicenseOutcome.Active)
        api.releaseTrial.complete(Unit)

        assertEquals(LicenseOutcome.Stale, old.await())
        assertEquals(EntitlementKind.Lifetime, store.value?.kind)
    }

    private fun snapshot(
        kind: EntitlementKind = EntitlementKind.Trial,
        offlineUntil: Long = 11_000L,
        verifiedElapsed: Long = 1_000L,
        bootCount: Int = 7,
        token: String = "session",
    ) = EntitlementSnapshot(
        kind = kind,
        startsAtMillis = 1_000L,
        expiresAtMillis = null,
        offlineValidUntilMillis = offlineUntil,
        serverTimeMillis = 1_000L,
        refreshAfterMillis = 6_000L,
        sessionToken = token,
        verifiedElapsedRealtimeMillis = verifiedElapsed,
        verifiedBootCount = bootCount,
    )

    private class FakeClock(var wall: Long, var elapsed: Long, var boot: Int) : LicenseClock {
        override fun wallTimeMillis() = wall
        override fun elapsedRealtimeMillis() = elapsed
        override fun bootCount() = boot
    }

    private class FakeStore : LicensingStore {
        var id = "0123456789abcdef0123456789abcdef"
        var value: EntitlementSnapshot? = null
        override fun installationId() = id
        override fun snapshot() = value
        override fun save(snapshot: EntitlementSnapshot) { value = snapshot }
        override fun clearEntitlement() { value = null }
    }

    private class ControllableApi : LicensingApi {
        val trialStarted = CompletableDeferred<Unit>()
        val releaseTrial = CompletableDeferred<Unit>()

        override suspend fun startTrial(installationId: String, appVersion: String): LicenseEnvelope {
            trialStarted.complete(Unit)
            releaseTrial.await()
            return envelope(EntitlementKind.Trial, "trial-session")
        }

        override suspend fun activate(
            installationId: String,
            appVersion: String,
            activationCode: String,
        ): LicenseEnvelope = envelope(EntitlementKind.Lifetime, "paid-session")

        override suspend fun refresh(
            installationId: String,
            appVersion: String,
            sessionToken: String,
        ): LicenseEnvelope = envelope(EntitlementKind.Lifetime, "refreshed-session")

        override suspend fun revoke(sessionToken: String) = Unit

        private fun envelope(kind: EntitlementKind, token: String) = LicenseEnvelope(
            serverTimeMillis = 1_000L,
            entitlement = EntitlementPayload(kind, 1_000L, null, 11_000L),
            sessionToken = token,
            refreshAfterMillis = 6_000L,
        )
    }
}
