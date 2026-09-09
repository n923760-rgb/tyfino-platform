package dev.tyfino.foundation.playback

import dev.tyfino.foundation.xtream.CatalogSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviousLiveChannelControllerTest {
    @Test
    fun successfulStartsSwapCurrentAndPreviousChannels() {
        val controller = PreviousLiveChannelController()
        controller.recordSuccessfullyStarted(live("one"))
        controller.recordSuccessfullyStarted(live("two"))

        val firstSwap = controller.beginPrevious(live("two"))
        assertEquals("one", firstSwap?.targetProviderItemId)
        assertTrue(controller.recordSuccessfullyStarted(live("one")))

        val secondSwap = controller.beginPrevious(live("one"))
        assertEquals("two", secondSwap?.targetProviderItemId)
    }

    @Test
    fun failedStartCannotOverwriteTheLastSuccessfulPair() {
        val controller = PreviousLiveChannelController()
        controller.recordSuccessfullyStarted(live("one"))
        controller.recordSuccessfullyStarted(live("two"))

        // A failed player never calls recordSuccessfullyStarted for channel three.
        assertEquals("one", controller.beginPrevious(live("two"))?.targetProviderItemId)
        assertNull(controller.beginPrevious(live("three")))
    }

    @Test
    fun newerStartAndClearInvalidateOutstandingRequests() {
        val controller = PreviousLiveChannelController()
        controller.recordSuccessfullyStarted(live("one"))
        controller.recordSuccessfullyStarted(live("two"))
        val request = requireNotNull(controller.beginPrevious(live("two")))

        controller.recordSuccessfullyStarted(live("three"))
        assertFalse(controller.isCurrent(request))

        val newerRequest = requireNotNull(controller.beginPrevious(live("three")))
        controller.clear()
        assertFalse(controller.isCurrent(newerRequest))
        assertEquals(1L, controller.state.value.version - newerRequest.version)
    }

    @Test
    fun accountReplacementStartsWithNoPreviousChannel() {
        val controller = PreviousLiveChannelController()
        controller.recordSuccessfullyStarted(live("one", accountId = "account-a"))
        controller.recordSuccessfullyStarted(live("two", accountId = "account-a"))

        controller.recordSuccessfullyStarted(live("foreign", accountId = "account-b"))

        assertNull(controller.beginPrevious(live("foreign", accountId = "account-b")))
    }

    @Test
    fun moviesNeverEnterLiveHistory() {
        val controller = PreviousLiveChannelController()
        val movie = live("movie").copy(section = CatalogSection.Movies)

        assertFalse(controller.recordSuccessfullyStarted(movie))
        assertNull(controller.beginPrevious(movie))
    }

    private fun live(id: String, accountId: String = "account-a") = PlaybackSelection(
        accountId = accountId,
        accountGeneration = 4L,
        section = CatalogSection.Live,
        providerItemId = id,
        containerExtension = "ts",
    )
}
