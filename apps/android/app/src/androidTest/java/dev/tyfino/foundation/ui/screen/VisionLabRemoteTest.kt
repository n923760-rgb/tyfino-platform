package dev.tyfino.foundation.ui.screen

import android.content.res.Configuration
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Actual entry/Dialog/media journey: only the initial origin receives a test focus request. */
@RunWith(AndroidJUnit4::class)
class VisionLabRemoteTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private lateinit var host: View

    @Before fun directionalInput() { instrumentation.setInTouchMode(false) }
    @After fun restoreInput() { instrumentation.setInTouchMode(true) }

    @Test fun remoteComparisonCloseAndBackRestoreEntryInEnglish() = exercise(LayoutDirection.Ltr, "en")
    @Test fun remoteComparisonCloseAndBackRestoreEntryInArabic() = exercise(LayoutDirection.Rtl, "ar")

    private fun exercise(direction: LayoutDirection, language: String) {
        val context = instrumentation.targetContext
        val resources = context.createConfigurationContext(Configuration(context.resources.configuration)
            .apply { setLocale(Locale(language)) }).resources
        lateinit var input: InputModeManager
        var hostExits = 0
        compose.setContent {
            host = LocalView.current
            input = LocalInputModeManager.current
            CompositionLocalProvider(LocalResources provides resources, LocalLayoutDirection provides direction) {
                TyfinoTheme {
                    BackHandler { hostExits++ }
                    Column { VisionLabEntry() }
                }
            }
        }
        fun strength(value: Int) = compose.onNodeWithTag("vision-strength")
            .assertTextEquals(resources.getString(R.string.vision_lab_strength, value))

        compose.waitUntil(5_000) { compose.runOnIdle { host.hasWindowFocus() } }
        compose.runOnIdle { assertTrue(input.requestInputMode(InputMode.Keyboard)) }
        compose.waitUntil(5_000) { compose.runOnIdle { input.inputMode == InputMode.Keyboard } }
        compose.onNodeWithTag("vision-lab-open")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }.assertIsFocused()
        openFromEntry()
        compose.onNodeWithTag("vision-original").assertIsSelected()
        val towardBalanced = if (direction == LayoutDirection.Ltr) Key.DirectionRight else Key.DirectionLeft
        compose.onNodeWithTag("vision-original").performKeyInput { pressKey(towardBalanced) }
        compose.onNodeWithTag("vision-balanced").assertIsFocused().assertIsNotSelected()
        compose.onNodeWithTag("vision-original").assertIsSelected()
        strength(50)
        compose.onNodeWithTag("vision-balanced").performKeyInput { pressKey(Key.Enter) }.assertIsSelected()
        compose.onNodeWithTag("vision-balanced").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("vision-more").assertIsFocused()
        strength(50)
        compose.onNodeWithTag("vision-more").performKeyInput { pressKey(Key.Enter) }
        strength(75)
        compose.onNodeWithTag("vision-more").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("vision-compare").assertIsFocused().assertIsNotSelected()
            .performKeyInput { pressKey(Key.Enter) }.assertIsSelected()
        strength(75)
        compose.onNodeWithTag("vision-compare").performKeyInput { pressKey(Key.Enter) }.assertIsNotSelected()
        strength(75)
        compose.onNodeWithTag("vision-compare").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("vision-close").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        assertReturned()
        compose.runOnIdle { assertEquals(0, hostExits) }

        openFromEntry()
        compose.onNodeWithTag("vision-original").assertIsSelected()
        compose.onNodeWithTag("vision-balanced").assertIsNotSelected()
        compose.onNodeWithTag("vision-compare").assertIsNotSelected().assertIsNotEnabled()
        strength(50)
        pressBack()
        assertReturned()
        compose.runOnIdle { assertEquals(0, hostExits) }
    }

    private fun openFromEntry() {
        compose.onNodeWithTag("vision-lab-open").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        compose.waitForIdle()
        awaitKeyboardDialogWindow()
        awaitFocused("vision-original")
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("vision-lab-ready").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithTag("vision-lab-failed").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("vision-lab-ready").assertExists()
        compose.onNodeWithTag("vision-original").assertIsFocused()
    }

    private fun assertReturned() {
        compose.onNodeWithTag("vision-lab-dialog").assertDoesNotExist()
        compose.waitUntil(5_000) { compose.runOnIdle { host.hasWindowFocus() } }
        awaitFocused("vision-lab-open")
    }

    private fun awaitFocused(tag: String) {
        compose.waitUntil(5_000) {
            compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Focused]
        }
        compose.onNodeWithTag(tag).assertIsFocused()
    }
}
