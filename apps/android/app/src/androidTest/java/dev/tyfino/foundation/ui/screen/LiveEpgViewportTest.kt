package dev.tyfino.foundation.ui.screen

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
import androidx.compose.ui.window.DialogWindowProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Root
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import dev.tyfino.foundation.xtream.LiveEpgFailure
import dev.tyfino.foundation.xtream.LiveEpgProgram
import dev.tyfino.foundation.xtream.LiveEpgState
import java.util.Locale
import kotlin.math.roundToInt
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveEpgViewportTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private var refreshed = 0
    private var closed = 0

    @Before fun directionalInput() { instrumentation.setInTouchMode(false) }
    @After fun restoreInput() { instrumentation.setInTouchMode(true) }

    private fun fixture(): LiveEpgState {
        val now = System.currentTimeMillis()
        return LiveEpgState.Stale(listOf(
            LiveEpgProgram("fixture", "channel", "Current program", "Program description. ".repeat(20),
                now - 60_000, now + 3_600_000),
            LiveEpgProgram("fixture", "channel", "Next program", "Upcoming description. ".repeat(20),
                now + 3_600_000, now + 7_200_000)), now, LiveEpgFailure.Timeout)
    }

    @Test fun shortNativeDialogKeepsRefreshAndCloseReachableByTouch() {
        renderDialog()
        constrainWindow()
        compose.onNodeWithTag("epg-refresh").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("epg-close").performScrollTo().assertIsDisplayed()
        assertScroll()
        compose.onNodeWithTag("epg-close").performClick()
        compose.onNodeWithTag("live-epg-dialog").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, refreshed); assertEquals(1, closed) }
    }

    @Test fun nativeDialogStartsAtRefreshAndDpadDoesNotInvokeActionsUntilEnter() {
        renderDialog()
        constrainWindow()
        compose.onNodeWithTag("epg-refresh").assertIsFocused().assertIsDisplayed()
            .performKeyInput { pressKey(Key.Enter) }
        compose.runOnIdle { assertEquals(1, refreshed); assertEquals(0, closed) }
        compose.onNodeWithTag("epg-refresh").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("epg-close").assertIsFocused().assertIsDisplayed()
        assertScroll()
        compose.runOnIdle { assertEquals(1, refreshed); assertEquals(0, closed) }
        compose.onNodeWithTag("epg-close").performKeyInput { pressKey(Key.Enter) }
        compose.onNodeWithTag("live-epg-dialog").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, refreshed); assertEquals(1, closed) }
    }

    @Test fun actualArabicFontTwoRendererScrollsToBothActions() {
        val context = instrumentation.targetContext
        val resources = context.createConfigurationContext(Configuration(context.resources.configuration)
            .apply { setLocale(Locale("ar")) }).resources
        val state = fixture()
        val title = resources.getString(R.string.epg_title)
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources,
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                TyfinoTheme { Box(Modifier.width(280.dp).height(320.dp)) {
                    LiveEpgContent(state, remember { FocusRequester() }, { refreshed++ }, { closed++ })
                } }
            }
        }
        compose.onNodeWithText(title, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                val layouts = mutableListOf<TextLayoutResult>()
                assertTrue(action(layouts))
                assertEquals(2f, layouts.single().layoutInput.density.fontScale, 0f)
            }
        compose.onNodeWithTag("epg-refresh").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("epg-close").performScrollTo().assertIsDisplayed()
        assertScroll()
        compose.onNodeWithTag("epg-close").performClick()
        compose.runOnIdle { assertEquals(1, refreshed); assertEquals(1, closed) }
    }

    private fun renderDialog() {
        val state = fixture()
        compose.setContent {
            var visible by remember { mutableStateOf(true) }
            TyfinoTheme { if (visible) LiveEpgDialog(state, { refreshed++ }, { closed++; visible = false }) }
        }
        compose.waitForIdle()
    }

    private fun constrainWindow() {
        val focusedDialog = object : TypeSafeMatcher<Root>() {
            override fun describeTo(description: Description) { description.appendText("focused EPG dialog") }
            override fun matchesSafely(root: Root): Boolean = isDialog().matches(root) && root.decorView.hasWindowFocus()
        }
        onView(isRoot()).inRoot(focusedDialog).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isRoot()
            override fun getDescription(): String = "bound actual EPG window to 280x320dp"
            override fun perform(uiController: UiController, view: View) {
                val window = findWindow(view) ?: throw AssertionError("native Dialog required")
                val density = view.resources.displayMetrics.density
                window.setLayout((280 * density).roundToInt(), (320 * density).roundToInt())
                uiController.loopMainThreadUntilIdle()
            }
        })
        compose.waitForIdle()
        val bounds = compose.onNodeWithTag("live-epg-dialog").fetchSemanticsNode().boundsInRoot
        val density = instrumentation.targetContext.resources.displayMetrics.density
        assertTrue(bounds.width <= 280 * density + 1f)
        assertTrue(bounds.height <= 320 * density + 1f)
    }

    private fun findWindow(view: View): Window? {
        if (view is DialogWindowProvider) return view.window
        if (view is ViewGroup) for (i in 0 until view.childCount) findWindow(view.getChildAt(i))?.let { return it }
        return null
    }

    private fun assertScroll() {
        val range = compose.onNodeWithTag("live-epg-scroll").fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue("EPG content must overflow", range.maxValue() > 0f)
        assertTrue("Actions must be reached through outer scrolling", range.value() > 0f)
    }
}
