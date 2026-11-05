package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import android.view.View
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackTrackActionTest {
    @get:Rule val compose = createComposeRule()

    @Before fun directionalInput() { InstrumentationRegistry.getInstrumentation().setInTouchMode(false) }
    @After fun restoreInput() { InstrumentationRegistry.getInstrumentation().setInTouchMode(true) }

    @Test fun trackStateUpdatesWithoutMovingFocusInLtr() = exercise(LayoutDirection.Ltr)
    @Test fun trackStateUpdatesWithoutMovingFocusInRtl() = exercise(LayoutDirection.Rtl)

    private fun exercise(direction: LayoutDirection) {
        var automatic by mutableStateOf(true)
        var off by mutableStateOf(false)
        var label by mutableStateOf<String?>("English")
        lateinit var host: View
        lateinit var input: InputModeManager
        var opened = 0
        var automaticLabel = ""
        var offLabel = ""
        compose.setContent {
            host = LocalView.current
            input = LocalInputModeManager.current
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                automaticLabel = stringResource(R.string.playback_automatic)
                offLabel = stringResource(R.string.playback_off)
                TyfinoTheme { Column {
                    PlaybackTrackAction(R.drawable.ic_player_subtitles, R.string.playback_subtitles,
                        automatic, off, label, { opened++ }, Modifier.testTag("track-action"))
                    PlaybackTrackAction(R.drawable.ic_player_audio, R.string.playback_audio,
                        true, false, null, {}, Modifier.testTag("next-action"))
                } }
            }
        }
        compose.waitUntil(5_000) { compose.runOnIdle { host.hasWindowFocus() } }
        compose.runOnIdle { assertTrue(input.requestInputMode(InputMode.Keyboard)) }
        compose.waitUntil(5_000) { compose.runOnIdle { input.inputMode == InputMode.Keyboard } }
        val action = compose.onNodeWithTag("track-action")
        action.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, automaticLabel))
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        action.assertIsFocused()
        compose.runOnIdle { automatic = false }
        action.assertIsFocused().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "English"))
        compose.runOnIdle { off = true }
        action.assertIsFocused().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, offLabel))
        compose.runOnIdle { off = false; label = null }
        action.assertIsFocused().assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
        compose.runOnIdle { assertEquals(0, opened); automatic = true }
        action.assertIsFocused().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, automaticLabel))
            .performKeyInput { pressKey(Key.Enter) }
        compose.runOnIdle { assertEquals(1, opened) }
        action.performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("next-action").assertIsFocused()
        compose.runOnIdle { label = "Arabic" }
        compose.onNodeWithTag("next-action").assertIsFocused()
        compose.runOnIdle { assertEquals(1, opened) }
    }
}
