@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package dev.tyfino.foundation.playback

import android.os.Build
import android.util.Log
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Qualifies the declared managed-emulator environment; never overrides the app's decoder selector. */
@RunWith(AndroidJUnit4::class)
class EmulatorMediaEnvironmentTest {
    @Test fun avcDefaultIsSoftwareForTheManagedEmulatorBaseline() {
        val decoder = MediaCodecSelector.DEFAULT.getDecoderInfos(MimeTypes.VIDEO_H264, false, false)
            .firstOrNull()
        assertNotNull("The software-video CI baseline needs a platform AVC decoder", decoder)
        val selected = requireNotNull(decoder)
        Log.i("TyfinoCI", "TYFINO_CI_DECODER api=${Build.VERSION.SDK_INT} name=${selected.name} software=${selected.softwareOnly}")
        assertTrue("Expected platform software AVC decoding, selected ${selected.name}", selected.softwareOnly)
        assertFalse("Goldfish host decoding is outside this headless CI baseline",
            selected.name.contains("goldfish", ignoreCase = true))
    }
}
