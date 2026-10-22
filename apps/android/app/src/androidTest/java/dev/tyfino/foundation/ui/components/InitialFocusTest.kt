package dev.tyfino.foundation.ui.components

import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
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
        var focusKey by mutableStateOf(0)
        lateinit var host: View
        lateinit var input: InputModeManager
        val transitions = mutableListOf<String>()
        compose.setContent {
            host = LocalView.current
            input = LocalInputModeManager.current
            TyfinoTheme {
                Column {
                    // Android needs an actual focusable child before leaving touch mode.
                    FocusVisibleButton("Host", {}, Modifier.testTag("initial-host"))
                    if (mounted) {
                        val first = rememberInitialFocusRequester(focusKey)
                        FocusVisibleButton("First", {}, Modifier.testTag("initial-first").focusRequester(first)
                            .onFocusChanged { transitions += "first=${it.isFocused}@${compose.mainClock.currentTime}" })
                        FocusVisibleButton("Second", {}, Modifier.testTag("initial-second")
                            .onFocusChanged { transitions += "second=${it.isFocused}@${compose.mainClock.currentTime}" })
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
                .performKeyInput { pressKey(Key.DirectionDown, pressDurationMillis = 0) }
            assertSecondFocused("immediately after Down", transitions)
            // No user selection or target-forcing: only the remaining initialization frame runs.
            compose.mainClock.advanceTimeByFrame()
            assertSecondFocused("after the remaining initialization frame", transitions)

            // A new content owner still receives its own initial target using the same requester.
            compose.runOnIdle { focusKey++ }
            compose.mainClock.advanceTimeByFrame()
            compose.mainClock.advanceTimeByFrame()
            compose.onNodeWithTag("initial-first").assertIsFocused()
                .performKeyInput { pressKey(Key.DirectionDown, pressDurationMillis = 0) }
            assertSecondFocused("after Down for the replacement key", transitions)
            compose.mainClock.advanceTimeByFrame()
            assertSecondFocused("after initialization for the replacement key", transitions)
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }

    private fun assertSecondFocused(stage: String, transitions: List<String>) {
        val focused = compose.onNodeWithTag("initial-second").fetchSemanticsNode()
            .config[SemanticsProperties.Focused]
        val trace = compose.runOnIdle { transitions.joinToString() }
        assertTrue("$stage: second must own focus; transitions=[$trace]", focused)
    }
}
