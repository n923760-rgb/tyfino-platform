package dev.tyfino.foundation.xtream

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SQLiteLiveEpgStoreTest {
    private val now = 1_704_110_500_000L
    private fun store() = SQLiteLiveEpgStore(InstrumentationRegistry.getInstrumentation().targetContext)
    private fun program(account: String, channel: String, start: Long = now) =
        LiveEpgProgram(account, channel, "A program", null, start, start + 3_600_000)

    @Test fun accountSwitchRetainsInactiveDataAndCredentialReplacementPurgesOnlyItsData() {
        val a = "epg-${UUID.randomUUID()}"
        val b = "epg-${UUID.randomUUID()}"
        val store = store()
        try {
            store.retainOwner(a, 1)
            store.replace(a, "one", 1, LiveEpgSnapshot(1, now, listOf(program(a, "one"))), now)
            assertEquals(1, store().load(a, "one", now)!!.programs.size)
            store.retainOwner(b, 1)
            assertEquals(1, store.load(a, "one", now)!!.programs.size)
            store.replace(b, "one", 1, LiveEpgSnapshot(1, now, listOf(program(b, "one"))), now)
            store.retainOwner(b, 2)
            assertNull(store().load(b, "one", now))
            assertEquals(1, store.load(a, "one", now)!!.programs.size)
        } finally {
            store.clearAccount(a)
            store.clearAccount(b)
        }
    }

    @Test fun snapshotReplaceIsAtomicAndEndedProgramsArePruned() {
        val a = "epg-${UUID.randomUUID()}"
        val store = store()
        try {
            store.retainOwner(a, 1)
            store.replace(a, "one", 1, LiveEpgSnapshot(1, now, listOf(
                program(a, "one", now - 7_200_000), program(a, "one"),
            )), now)
            assertEquals(1, store.load(a, "one", now)!!.programs.size)
            try {
                store.replace(a, "one", 1, LiveEpgSnapshot(2, now, listOf(
                    LiveEpgProgram(a, "one", "bad", null, now, now),
                )), now)
                throw AssertionError("invalid replacement accepted")
            } catch (_: IllegalArgumentException) {
                assertEquals(1L, store().load(a, "one", now)!!.generation)
            }
            store.replace(a, "one", 1, LiveEpgSnapshot(2, now, emptyList()), now)
            assertTrue(store().load(a, "one", now)!!.programs.isEmpty())
        } finally { store.clearAccount(a) }
    }

    @Test fun onlyTwentyMostRecentlyAccessedChannelsRemain() {
        val a = "epg-${UUID.randomUUID()}"
        val store = store()
        try {
            store.retainOwner(a, 1)
            (1..21).forEach { index ->
                val channel = "channel-$index"
                store.replace(a, channel, 1, LiveEpgSnapshot(1, now, listOf(program(a, channel))),
                    now + index)
            }
            assertNull(store().load(a, "channel-1", now + 22))
            assertEquals(1, store().load(a, "channel-21", now + 22)!!.programs.size)
        } finally { store.clearAccount(a) }
    }
}
