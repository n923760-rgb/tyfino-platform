@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package dev.tyfino.foundation.ui.screen

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dev.tyfino.foundation.playback.BoundedRedirectDataSource

/** Creates the single foreground player without changing TYFINO's playback transport policy. */
internal object PlayerFactory {
    fun create(context: Context, cleartextConsent: Boolean): ExoPlayer {
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(BoundedRedirectDataSource.Factory(cleartextConsent))
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setRenderersFactory(DefaultRenderersFactory(context).setEnableDecoderFallback(true))
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
