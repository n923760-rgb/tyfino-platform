package dev.tyfino.foundation.ui.screen

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveRetryPolicyTest {
    @Test
    fun transientLiveErrorsHaveThreeBoundedDelays() {
        for (error in listOf(
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW,
        )) {
            assertEquals(1_000L, liveRetryDelayMillis(error, 0))
            assertEquals(2_000L, liveRetryDelayMillis(error, 1))
            assertEquals(4_000L, liveRetryDelayMillis(error, 2))
            assertNull(liveRetryDelayMillis(error, 3))
            assertNull(liveRetryDelayMillis(error, 100))
        }
    }

    @Test
    fun decoderAndAccessFailuresDoNotRetry() {
        assertNull(liveRetryDelayMillis(PlaybackException.ERROR_CODE_DECODER_INIT_FAILED, 0))
        assertNull(liveRetryDelayMillis(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS, 0))
    }
}
