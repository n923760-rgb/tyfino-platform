package dev.tyfino.foundation.xtream

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XtreamAccountDataCleanerTest {
    @Test
    fun exactAccountIdReachesEveryPartition() = runBlocking {
        val calls = mutableListOf<Pair<Int, String>>()
        val cleaner = cleaner { index, accountId ->
            calls += index to accountId
            true
        }

        assertTrue(cleaner.clearAccount(ACCOUNT_ID))
        assertEquals((0..7).map { it to ACCOUNT_ID }, calls)
    }

    @Test
    fun failureStopsBeforeLaterPartitionsAndCanBeRetried() = runBlocking {
        var failing = true
        val calls = mutableListOf<Int>()
        val cleaner = cleaner { index, _ ->
            calls += index
            !failing || index != 3
        }

        assertFalse(cleaner.clearAccount(ACCOUNT_ID))
        assertEquals(listOf(0, 1, 2, 3), calls)
        calls.clear()
        failing = false

        assertTrue(cleaner.clearAccount(ACCOUNT_ID))
        assertEquals((0..7).toList(), calls)
    }

    @Test
    fun invalidAccountIdNeverTouchesAStore() = runBlocking {
        var calls = 0
        val cleaner = cleaner { _, _ -> calls++; true }

        assertFalse(cleaner.clearAccount(""))
        assertFalse(cleaner.clearAccount("a".repeat(129)))
        assertEquals(0, calls)
    }

    private fun cleaner(clear: suspend (Int, String) -> Boolean) = XtreamAccountDataCleaner(
        List(8) { index -> XtreamAccountPartitionCleaner { accountId -> clear(index, accountId) } },
    )

    private companion object {
        const val ACCOUNT_ID = "00000000000000000000000000000001"
    }
}
