@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package dev.tyfino.foundation.ui.screen

import android.content.res.Configuration
import android.view.View
import android.view.ViewTreeObserver
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.R
import dev.tyfino.foundation.playback.PlaybackSelection
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import dev.tyfino.foundation.xtream.CatalogSection
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackAspectTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val focusTrace = mutableListOf<String>()
    private var dialogView: View? = null
    private var removeFocusObservers: (() -> Unit)? = null
    private var owner by mutableStateOf(PlaybackSelection("fixture-a", 1L, CatalogSection.Live, "one", "ts"))

    @Before fun useDirectionalInput() { instrumentation.setInTouchMode(false) }
    @After fun restoreTouchInput() {
        instrumentation.runOnMainSync { removeFocusObservers?.invoke() }
        instrumentation.setInTouchMode(true)
    }

    @Test fun touchUpdatesTheSameNativePlayerViewAndFrameGeometryWithoutReplacingItsPlayer() {
        render()
        val initialView = nativeView()
        val initialPlayer = initialView.player
        assertTrue(initialPlayer != null)
        for ((mode, expected) in listOf(PlaybackAspectMode.Fit to AspectRatioFrameLayout.RESIZE_MODE_FIT,
            PlaybackAspectMode.Zoom to AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
            PlaybackAspectMode.Stretch to AspectRatioFrameLayout.RESIZE_MODE_FILL)) {
            choose(mode)
            onView(isAssignableFrom(PlayerView::class.java)).check { view, error ->
                if (error != null) throw error
                val current = view as PlayerView
                assertSame(initialView, current)
                assertSame(initialPlayer, current.player)
                assertEquals(expected, current.resizeMode)
                assertEquals(0, current.player!!.mediaItemCount)
                val frame = current.findViewById<AspectRatioFrameLayout>(androidx.media3.ui.R.id.exo_content_frame)
                frame.setAspectRatio(16f / 9f)
                val exact = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY)
                frame.measure(exact, exact)
                when (mode) {
                    PlaybackAspectMode.Fit -> { assertEquals(200, frame.measuredWidth); assertTrue(frame.measuredHeight in 112..113) }
                    PlaybackAspectMode.Zoom -> { assertTrue(frame.measuredWidth in 355..356); assertEquals(200, frame.measuredHeight) }
                    PlaybackAspectMode.Stretch -> { assertEquals(200, frame.measuredWidth); assertEquals(200, frame.measuredHeight) }
                }
            }
            compose.onNodeWithTag("playback-aspect-dialog").assertDoesNotExist()
        }
    }

    @Test fun directionalNavigationSelectsOnlyOnEnterAndCancelRetainsTheCurrentMode() {
        render()
        val video = nativeView()
        compose.onNodeWithTag("playback-aspect").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            .performKeyInput { pressKey(Key.Enter) }
        compose.onNodeWithTag("aspect-mode-Fit").assertIsSelected().assertIsFocused()
        observeDialogWindow()
        recordFocus("before Down")
        compose.onNodeWithTag("aspect-mode-Fit").performKeyInput { pressKey(Key.DirectionDown) }
        recordFocus("after Down")
        assertZoomFocused("after Down from Fit")
        compose.runOnIdle { assertEquals(AspectRatioFrameLayout.RESIZE_MODE_FIT, video.resizeMode) }
        compose.onNodeWithTag("aspect-mode-Zoom").performKeyInput { pressKey(Key.Enter) }
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, nativeView().resizeMode)
        compose.onNodeWithTag("playback-aspect").performClick()
        compose.onNodeWithTag("aspect-mode-Zoom").assertIsSelected()
        assertZoomFocused("after reopening the selected Zoom mode")
        pressBack()
        compose.onNodeWithTag("playback-aspect-dialog").assertDoesNotExist()
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, nativeView().resizeMode)
    }

    @Test fun changingContentAccountGenerationOrSectionResetsPresentationToFit() {
        render()
        choose(PlaybackAspectMode.Stretch)
        compose.runOnIdle { owner = owner.copy(providerItemId = "two") }
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_FIT, nativeView().resizeMode)
        choose(PlaybackAspectMode.Zoom)
        compose.runOnIdle { owner = owner.copy(accountId = "fixture-b") }
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_FIT, nativeView().resizeMode)
        choose(PlaybackAspectMode.Stretch)
        compose.runOnIdle { owner = owner.copy(accountGeneration = 2L) }
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_FIT, nativeView().resizeMode)
        choose(PlaybackAspectMode.Zoom)
        compose.runOnIdle { owner = owner.copy(section = CatalogSection.Movies, containerExtension = "mp4") }
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_FIT, nativeView().resizeMode)
    }

    @Test fun actualArabicLargeTextRendererKeepsChoicesAndCloseReachable() {
        val context = instrumentation.targetContext
        val resources = context.createConfigurationContext(Configuration(context.resources.configuration)
            .apply { setLocale(Locale("ar")) }).resources
        val title = resources.getString(R.string.playback_picture_size)
        var chosen: PlaybackAspectMode? = null
        var closed = 0
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources,
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                TyfinoTheme {
                    Box(Modifier.width(280.dp).height(320.dp)) {
                        PlaybackAspectContent(PlaybackAspectMode.Zoom, remember { FocusRequester() },
                            { chosen = it }, { closed++ })
                    }
                }
            }
        }
        compose.onNodeWithText(title, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                val layouts = mutableListOf<TextLayoutResult>()
                assertTrue(action(layouts)); assertEquals(2f, layouts.single().layoutInput.density.fontScale, 0f)
            }
        compose.onNodeWithTag("aspect-mode-Zoom").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("aspect-mode-Stretch").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("aspect-close").performScrollTo().assertIsDisplayed().performClick()
        val range = compose.onNodeWithTag("playback-aspect-scroll").fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue(range.maxValue() > 0f); assertTrue(range.value() > 0f)
        compose.runOnIdle { assertEquals(PlaybackAspectMode.Stretch, chosen); assertEquals(1, closed) }
    }

    private fun assertZoomFocused(stage: String) {
        val focused = compose.onNodeWithTag("aspect-mode-Zoom").fetchSemanticsNode()
            .config[SemanticsProperties.Focused]
        val trace = compose.runOnIdle { focusTrace.joinToString("; ") }
        assertTrue("Picture-size focus $stage: Zoom must own focus; trace=[$trace]", focused)
    }

    /** Observe the real production dialog; listeners never request focus or consume input. */
    private fun observeDialogWindow() {
        onView(isRoot()).inRoot(isDialog()).check { view, error ->
            if (error != null) throw error
            val root = requireNotNull(view)
            dialogView = root
            val observer = root.viewTreeObserver
            val window = ViewTreeObserver.OnWindowFocusChangeListener { focused ->
                trace("window=$focused touch=${root.isInTouchMode} native=${root.findFocus()?.javaClass?.simpleName}")
            }
            val native = ViewTreeObserver.OnGlobalFocusChangeListener { old, current ->
                trace("native=${old?.javaClass?.simpleName}->${current?.javaClass?.simpleName}")
            }
            val touch = ViewTreeObserver.OnTouchModeChangeListener { inTouchMode -> trace("touch=$inTouchMode") }
            observer.addOnWindowFocusChangeListener(window)
            observer.addOnGlobalFocusChangeListener(native)
            observer.addOnTouchModeChangeListener(touch)
            removeFocusObservers = {
                if (observer.isAlive) {
                    observer.removeOnWindowFocusChangeListener(window)
                    observer.removeOnGlobalFocusChangeListener(native)
                    observer.removeOnTouchModeChangeListener(touch)
                }
            }
        }
    }

    private fun recordFocus(stage: String) {
        val nodes = compose.onAllNodes(isFocusable()).fetchSemanticsNodes().map { node ->
            val tag = if (node.config.contains(SemanticsProperties.TestTag)) node.config[SemanticsProperties.TestTag] else "untagged"
            val focused = node.config[SemanticsProperties.Focused]
            "$tag=$focused@${node.boundsInRoot}"
        }
        compose.runOnIdle {
            val root = dialogView
            trace("$stage window=${root?.hasWindowFocus()} touch=${root?.isInTouchMode} nodes=$nodes")
        }
    }

    private fun trace(value: String) {
        // Static fixture tags, geometry and input/focus facts only; no media/provider data.
        val entry = "${android.os.SystemClock.uptimeMillis()}:$value"
        focusTrace += entry
        Log.i("TyfinoAspectFocusTest", entry)
    }

    private fun render() {
        compose.setContent {
            val context = LocalContext.current
            val player = remember { PlayerFactory.create(context, false, CatalogSection.Live) }
            DisposableEffect(player) { onDispose { player.release() } }
            var mode by rememberPlaybackAspectMode(owner)
            var dialog by remember { mutableStateOf(false) }
            TyfinoTheme {
                Box(Modifier.fillMaxSize()) {
                    PlaybackVideoView(player, mode, Modifier.fillMaxSize())
                    Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                        PlaybackAspectAction(mode) { dialog = true }
                    }
                }
                if (dialog) PlaybackAspectDialog(mode, { mode = it; dialog = false }, { dialog = false })
            }
        }
        compose.waitForIdle()
    }

    private fun choose(mode: PlaybackAspectMode) {
        compose.onNodeWithTag("playback-aspect").performClick()
        compose.onNodeWithTag("aspect-mode-$mode").performScrollTo().performClick()
        compose.waitForIdle()
    }

    private fun nativeView(): PlayerView {
        lateinit var result: PlayerView
        onView(isAssignableFrom(PlayerView::class.java)).check { view, error ->
            if (error != null) throw error
            result = view as PlayerView
        }
        return result
    }
}
