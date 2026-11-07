@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package dev.tyfino.foundation.ui.screen

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.ui.PlayerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class VisionLabTest {
    @get:Rule val compose = createComposeRule()

    @Test fun keyboardChangesStrengthAndComparisonWithoutClosing() {
        var profile by mutableStateOf(VisionLabProfile())
        var closes = 0
        lateinit var input: InputModeManager
        lateinit var root: android.view.View
        compose.setContent { input = LocalInputModeManager.current
            root = LocalView.current
            TyfinoTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
                VisionLabControls(profile, false, { profile = it }, { closes++ })
            } }
        }
        compose.waitUntil(5_000) { compose.runOnIdle { root.hasWindowFocus() } }
        compose.runOnIdle { assertTrue(input.requestInputMode(InputMode.Keyboard)) }
        compose.onNodeWithTag("vision-original").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            .assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithTag("vision-balanced").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        compose.onNodeWithTag("vision-balanced").assertIsSelected()
        compose.runOnIdle { assertEquals(.5f, profile.effectiveStrength, 0f); assertEquals(0, closes) }
        compose.onNodeWithTag("vision-more").performClick().performClick().assertIsNotEnabled()
        compose.runOnIdle { assertEquals(100, profile.strength) }
        compose.onNodeWithTag("vision-compare").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(0f, profile.effectiveStrength, 0f); assertEquals(100, profile.strength) }
        compose.onNodeWithTag("vision-compare").performClick()
        repeat(4) { compose.onNodeWithTag("vision-less").performClick() }
        compose.onNodeWithTag("vision-less").assertIsNotEnabled()
        compose.onNodeWithTag("vision-compare").assertIsNotEnabled()
        compose.runOnIdle { assertTrue(input.requestInputMode(InputMode.Keyboard)) }
        compose.onNodeWithTag("vision-close").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            .performKeyInput { pressKey(Key.Enter) }
        compose.runOnIdle { assertEquals(1, closes); assertEquals(0f, profile.effectiveStrength, 0f) }
    }

    @Test fun arabicLargeFontShortControlsKeepFailureExitReachable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resources = context.createConfigurationContext(Configuration(context.resources.configuration)
            .apply { setLocale(Locale.forLanguageTag("ar")) }).resources
        var failed by mutableStateOf(false)
        var profile by mutableStateOf(VisionLabProfile(balanced = true))
        var closes = 0
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources,
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                TyfinoTheme { Column(Modifier.width(280.dp).height(320.dp)
                    .verticalScroll(rememberScrollState())) {
                    VisionLabControls(profile, failed, { profile = it }, { closes++ })
                } }
            }
        }
        compose.onNodeWithTag("vision-more").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { failed = true }
        for (tag in listOf("vision-original", "vision-balanced", "vision-less", "vision-more", "vision-compare")) {
            compose.onNodeWithTag(tag).assertIsNotEnabled()
        }
        compose.onNodeWithTag("vision-close").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, closes); assertEquals(75, profile.strength) }
    }

    @Test fun hdrIsRejectedBeforeAllocatingGlResources() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertThrows(VideoFrameProcessingException::class.java) {
            VisionBalancedEffect(.5f).toGlShaderProgram(context, true)
        }
    }

    @Test fun settingsLaboratoryChangesDecodedPixelsAndBackReleasesDecoder() {
        var accountActions = 0
        compose.setContent { TyfinoTheme { SettingsScreen({ accountActions++ }, { accountActions++ }) } }
        compose.onNodeWithTag("vision-lab-open").performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("vision-lab-ready").fetchSemanticsNodes().isNotEmpty() ||
            compose.onAllNodesWithTag("vision-lab-failed").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("vision-lab-ready").assertExists()
        lateinit var player: ExoPlayer
        lateinit var surface: SurfaceView
        val starts = AtomicInteger()
        val releases = AtomicInteger()
        Espresso.onView(isAssignableFrom(PlayerView::class.java)).check { view, error ->
            if (error != null) throw error
            val playerView = view as PlayerView
            player = playerView.player as ExoPlayer
            surface = playerView.videoSurfaceView as SurfaceView
            player.addAnalyticsListener(object : AnalyticsListener {
                override fun onVideoDecoderInitialized(eventTime: AnalyticsListener.EventTime,
                    decoderName: String, initializedTimestampMs: Long, initializationDurationMs: Long) { starts.incrementAndGet() }
                override fun onVideoDecoderReleased(eventTime: AnalyticsListener.EventTime, decoderName: String) { releases.incrementAndGet() }
            })
        }
        val original = pixels(surface)
        // Prove the capture contains the known decoded bars, not a black/blank surface.
        assertTrue(original[0] < 10)
        assertTrue(original[1] in 175..205)
        assertTrue(original[2] > 245)
        compose.onNodeWithTag("vision-balanced").performScrollTo().performClick()
        repeat(2) { compose.onNodeWithTag("vision-more").performScrollTo().performClick() }
        var filtered = original
        compose.waitUntil(10_000) { filtered = pixels(surface); filtered[1] >= original[1] + 2 }
        assertTrue(abs(filtered[0] - original[0]) <= 3)
        assertTrue(abs(filtered[2] - original[2]) <= 3)
        compose.onNodeWithTag("vision-compare").performScrollTo().performClick()
        compose.waitUntil(10_000) { val result = pixels(surface); result.indices.all { abs(result[it] - original[it]) <= 2 } }
        compose.onNodeWithTag("vision-compare").performClick()
        repeat(4) { compose.onNodeWithTag("vision-less").performScrollTo().performClick() }
        compose.waitUntil(10_000) { val result = pixels(surface); result.indices.all { abs(result[it] - original[it]) <= 2 } }
        Espresso.onView(isAssignableFrom(PlayerView::class.java)).check { view, error ->
            if (error != null) throw error
            assertSame(player, (view as PlayerView).player)
            assertEquals("asset:///vision-sdr.mp4", player.currentMediaItem?.localConfiguration?.uri.toString())
        }
        assertEquals(0, starts.get())
        assertEquals(0, releases.get())
        Espresso.pressBack()
        compose.onNodeWithTag("vision-lab-dialog").assertDoesNotExist()
        compose.waitUntil(5_000) { releases.get() == 1 }
        compose.runOnIdle { assertEquals(0, accountActions) }
    }

    /** PixelCopy reads the actual Surface buffer, including the effect, rather than Compose semantics. */
    private fun pixels(surface: SurfaceView): IntArray {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var bitmap: Bitmap
        val done = CountDownLatch(1)
        val result = AtomicInteger(-1)
        instrumentation.runOnMainSync {
            bitmap = Bitmap.createBitmap(surface.width, surface.height, Bitmap.Config.ARGB_8888)
            PixelCopy.request(surface, bitmap, { result.set(it); done.countDown() }, Handler(Looper.getMainLooper()))
        }
        try {
            assertTrue("Surface copy timeout", done.await(3, TimeUnit.SECONDS))
            assertEquals("Surface copy failed", PixelCopy.SUCCESS, result.get())
            return intArrayOf(0, 3, 4).map { bar ->
                val x = ((bar + .5f) * bitmap.width / 8).toInt()
                val y = bitmap.height / 2
                (-2..2).sumOf { dx -> Color.red(bitmap.getPixel(x + dx, y)) } / 5
            }.toIntArray()
        } finally { bitmap.recycle() }
    }
}
