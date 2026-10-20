package dev.tyfino.foundation.ui.components

import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Controlled-frame diagnosis of the production helper; no replacement focus implementation. */
@RunWith(AndroidJUnit4::class)
class InitialFocusTest {
    @get:Rule val compose = createComposeRule()

    @Test fun aLaterInitializationFrameDoesNotUndoDirectionalNavigation() {
        var mounted by mutableStateOf(false)
        lateinit var host: View
        lateinit var input: InputModeManager
        compose.setContent {
            host = LocalView.current
            input = LocalInputModeManager.current
            TyfinoTheme {
                Column {
                    // Android needs an actual focusable child before leaving touch mode.
                    FocusVisibleButton("Host", {}, Modifier.testTag("initial-host"))
                    if (mounted) {
                        val first = rememberInitialFocusRequester()
                        FocusVisibleButton("First", {}, Modifier.testTag("initial-first").focusRequester(first))
                        FocusVisibleButton("Second", {}, Modifier.testTag("initial-second"))
                    }
                }
            }
        }
        compose.waitUntil(5_000) { compose.runOnIdle { host.hasWindowFocus() } }
        compose.runOnIdle { assertTrue("Host must accept keyboard input before mounting the helper",
            input.requestInputMode(InputMode.Keyboard)) }
        compose.waitUntil(5_000) { compose.runOnIdle { input.inputMode == InputMode.Keyboard } }

        compose.mainClock.autoAdvance = false
        try {
            compose.runOnIdle { mounted = true }
            // First frame composes/attaches the target; the next resumes the first frame waiter.
            compose.mainClock.advanceTimeByFrame()
            compose.mainClock.advanceTimeByFrame()
            compose.onNodeWithTag("initial-first").assertIsFocused()
                .performKeyInput { pressKey(Key.DirectionDown) }
            compose.onNodeWithTag("initial-second").assertIsFocused()
            // No user selection or target-forcing: only the remaining initialization frame runs.
            compose.mainClock.advanceTimeByFrame()
            compose.onNodeWithTag("initial-second").assertIsFocused()
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }
}
