@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.ui.PlayerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisionLabSessionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun closeReleasesDecoderAndExplicitReopenStartsOriginal() = exitAndReopen(background = false)

    @Test fun stopReleasesDecoderAndResumeRequiresExplicitReopen() = exitAndReopen(background = true)

    private fun exitAndReopen(background: Boolean) {
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry(this)
            override val lifecycle: Lifecycle get() = registry
        }
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                TyfinoTheme { Column { VisionLabEntry() } }
            }
        }
        val original = openReadyPlayer()
        val originalReleases = releases(original)
        compose.onNodeWithTag("vision-balanced").performScrollTo().performClick()
        compose.onNodeWithTag("vision-more").performScrollTo().performClick()
        compose.onNodeWithTag("vision-compare").performScrollTo().performClick().assertIsSelected()
        assertStrength(75)

        if (background) {
            // Real ON_PAUSE/ON_STOP through a local lifecycle, without calling dismissal ourselves.
            compose.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        } else {
            compose.onNodeWithTag("vision-close").performScrollTo().performClick()
        }
        compose.onNodeWithTag("vision-lab-dialog").assertDoesNotExist()
        compose.waitUntil(5_000) { originalReleases.get() == 1 }

        if (background) {
            compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
            compose.onNodeWithTag("vision-lab-dialog").assertDoesNotExist()
        }
        compose.onNodeWithTag("vision-lab-open").assertIsDisplayed().assertIsEnabled()
        val reopened = openReadyPlayer()
        val reopenedReleases = releases(reopened)
        assertNotSame("A dismissed session must not retain its player", original, reopened)
        compose.onNodeWithTag("vision-original").assertIsSelected()
        compose.onNodeWithTag("vision-balanced").assertIsNotSelected()
        compose.onNodeWithTag("vision-compare").assertIsNotSelected().assertIsNotEnabled()
        assertStrength(50)
        compose.onNodeWithTag("vision-close").performScrollTo().performClick()
        compose.onNodeWithTag("vision-lab-dialog").assertDoesNotExist()
        compose.waitUntil(5_000) { reopenedReleases.get() == 1 }
        assertEquals("Old decoder must not be released twice", 1, originalReleases.get())
    }

    private fun openReadyPlayer(): ExoPlayer {
        compose.onNodeWithTag("vision-lab-open").performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("vision-lab-ready").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithTag("vision-lab-failed").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("vision-lab-ready").assertExists()
        lateinit var player: ExoPlayer
        Espresso.onView(isAssignableFrom(PlayerView::class.java)).check { view, error ->
            if (error != null) throw error
            player = (view as PlayerView).player as ExoPlayer
            assertEquals("asset:///vision-sdr.mp4", player.currentMediaItem?.localConfiguration?.uri.toString())
        }
        return player
    }

    private fun releases(player: ExoPlayer): AtomicInteger {
        val count = AtomicInteger()
        compose.runOnIdle {
            player.addAnalyticsListener(object : AnalyticsListener {
                override fun onVideoDecoderReleased(eventTime: AnalyticsListener.EventTime, decoderName: String) {
                    count.incrementAndGet()
                }
            })
        }
        return count
    }

    private fun assertStrength(value: Int) {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        compose.onNodeWithTag("vision-strength")
            .assertTextEquals(resources.getString(R.string.vision_lab_strength, value))
    }
}
