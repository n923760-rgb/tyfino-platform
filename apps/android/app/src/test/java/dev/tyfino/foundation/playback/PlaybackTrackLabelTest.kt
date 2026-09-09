package dev.tyfino.foundation.playback

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackTrackLabelTest {
    @Test
    fun languageNameTakesPriority() {
        assertEquals(
            "Arabic",
            PlaybackTrackLabel.resolve(
                language = "ar",
                mediaLabel = "Provider label",
                fallback = "Audio track",
                displayLocale = Locale.ENGLISH,
            ),
        )
    }

    @Test
    fun mediaLabelAndFallbackAreUsedWhenLanguageIsUnavailable() {
        assertEquals(
            "Director commentary",
            PlaybackTrackLabel.resolve(
                language = "und",
                mediaLabel = "Director commentary",
                fallback = "Audio track",
                displayLocale = Locale.ENGLISH,
            ),
        )
        assertEquals(
            "Subtitle track",
            PlaybackTrackLabel.resolve(
                language = null,
                mediaLabel = "  ",
                fallback = "Subtitle track",
                displayLocale = Locale.ENGLISH,
            ),
        )
    }

    @Test
    fun untrustedLabelsAreNormalizedAndBoundedWithoutSplittingUnicode() {
        val result = PlaybackTrackLabel.resolve(
            language = null,
            mediaLabel = "\u0000  " + "😀".repeat(90) + "\n",
            fallback = "Audio track",
            displayLocale = Locale.ENGLISH,
        )

        assertEquals(80, result.codePointCount(0, result.length))
        assertTrue(result.endsWith("…"))
        assertTrue(result.startsWith("😀"))
    }
}
