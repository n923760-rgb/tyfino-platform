@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package dev.tyfino.foundation.ui.screen

import android.app.ActivityManager
import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dev.tyfino.foundation.playback.BoundedRedirectDataSource
import dev.tyfino.foundation.xtream.CatalogSection

private const val LIVE_MIN_BUFFER_MS = 15_000
private const val LIVE_MAX_BUFFER_MS = 30_000
private const val LIVE_PLAYBACK_BUFFER_MS = 1_500
private const val LIVE_REBUFFER_MS = 3_000
private const val VOD_MIN_BUFFER_MS = 30_000
private const val VOD_MAX_BUFFER_MS = 60_000
private const val VOD_PLAYBACK_BUFFER_MS = 2_000
private const val VOD_REBUFFER_MS = 4_000
private const val LOW_MEMORY_TARGET_BYTES = 16 * 1024 * 1024

/** Creates the single foreground player without changing TYFINO's playback transport policy. */
internal object PlayerFactory {
    fun create(context: Context, cleartextConsent: Boolean, section: CatalogSection): ExoPlayer {
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(BoundedRedirectDataSource.Factory(cleartextConsent))
        val lowMemory = (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true
        val boundedBuffer = section == CatalogSection.Live || lowMemory
        val loadControl = DefaultLoadControl.Builder().apply {
            if (boundedBuffer) {
                setBufferDurationsMs(LIVE_MIN_BUFFER_MS, LIVE_MAX_BUFFER_MS, LIVE_PLAYBACK_BUFFER_MS, LIVE_REBUFFER_MS)
                setTargetBufferBytes(LOW_MEMORY_TARGET_BYTES)
            } else {
                setBufferDurationsMs(VOD_MIN_BUFFER_MS, VOD_MAX_BUFFER_MS, VOD_PLAYBACK_BUFFER_MS, VOD_REBUFFER_MS)
            }
        }.build()
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setRenderersFactory(DefaultRenderersFactory(context).setEnableDecoderFallback(true))
            .setLoadControl(loadControl)
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    true,
                )
                setHandleAudioBecomingNoisy(true)
            }
    }
}
