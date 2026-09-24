package dev.tyfino.foundation.ui.screen

import androidx.media3.common.PlaybackException

private val LIVE_RETRY_DELAYS_MILLIS = longArrayOf(1_000L, 2_000L, 4_000L)

internal fun liveRetryDelayMillis(errorCode: Int, attemptsMade: Int): Long? {
    if (errorCode != PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED &&
        errorCode != PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT &&
        errorCode != PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW
    ) return null
    return LIVE_RETRY_DELAYS_MILLIS.getOrNull(attemptsMade)
}
