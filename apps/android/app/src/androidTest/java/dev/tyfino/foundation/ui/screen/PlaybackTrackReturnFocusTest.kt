package dev.tyfino.foundation.ui.screen

import android.content.res.Configuration
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
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

/** Real action/Dialog integration, without media or replacement focus-restoration code. */
@RunWith(AndroidJUnit4::class)
class PlaybackTrackReturnFocusTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private lateinit var host: View

    @Before fun directionalInput() { instrumentation.setInTouchMode(false) }
    @After fun restoreInput() { instrumentation.setInTouchMode(true) }

    @Test fun backCloseAndSelectionReturnToLauncherInEnglish() = exercise(LayoutDirection.Ltr, "en")
    @Test fun backCloseAndSelectionReturnToLauncherInArabic() = exercise(LayoutDirection.Rtl, "ar")

    private fun exercise(direction: LayoutDirection, language: String) {
        val context = instrumentation.targetContext
        val resources = context.createConfigurationContext(
            Configuration(context.resources.configuration).apply { setLocale(Locale(language)) },
        ).resources
        var visible by mutableStateOf(false)
        var automatic by mutableStateOf(true)
        var off by mutableStateOf(false)
        var opened = 0
        var dismissed = 0
        var automaticCalls = 0
        var offCalls = 0
        var trackCalls = 0
        var exits = 0
        lateinit var input: InputModeManager
        compose.setContent {
            host = LocalView.current
            input = LocalInputModeManager.current
            CompositionLocalProvider(LocalResources provides resources, LocalLayoutDirection provides direction) {
                TyfinoTheme {
                    BackHandler { exits++ }
                    PlaybackTrackAction(R.drawable.ic_player_subtitles, R.string.playback_subtitles,
                        automatic, off, null, { opened++; visible = true }, Modifier.testTag("return-launcher"))
                    if (visible) {
                        TrackPickerDialog(
                            title = stringResource(R.string.playback_subtitles),
                            options = emptyList(), automaticSelected = automatic, includeOff = true,
                            offSelected = off, noTracksMessage = stringResource(R.string.playback_no_subtitles),
                            onAutomatic = { automaticCalls++; automatic = true; off = false; visible = false },
                            onOff = { offCalls++; automatic = false; off = true; visible = false },
                            onTrack = { trackCalls++ },
                            onDismiss = { dismissed++; visible = false },
                        )
                    }
                }
            }
        }
        compose.waitUntil(5_000) { compose.runOnIdle { host.hasWindowFocus() } }
        compose.runOnIdle { assertTrue(input.requestInputMode(InputMode.Keyboard)) }
        compose.waitUntil(5_000) { compose.runOnIdle { input.inputMode == InputMode.Keyboard } }
        // Establish only the origin; never force focus after a Dialog closes.
        compose.onNodeWithTag("return-launcher")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }.assertIsFocused()
        openFromLauncher()
        awaitFocused("track-picker-automatic")
        pressBack()
        assertReturned()
        compose.runOnIdle { assertEquals(1, opened); assertEquals(1, dismissed) }
        assertNoSelectionOrExit(automaticCalls, offCalls, trackCalls, exits)

        openFromLauncher()
        awaitFocused("track-picker-automatic")
        compose.onNodeWithTag("track-picker-automatic").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("track-picker-off").assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("track-picker-close").assertIsFocused()
        assertNoSelectionOrExit(automaticCalls, offCalls, trackCalls, exits)
        compose.onNodeWithTag("track-picker-close").performKeyInput { pressKey(Key.Enter) }
        assertReturned()
        compose.runOnIdle { assertEquals(2, opened); assertEquals(2, dismissed) }
        assertNoSelectionOrExit(automaticCalls, offCalls, trackCalls, exits)

        openFromLauncher()
        awaitFocused("track-picker-automatic")
        compose.onNodeWithTag("track-picker-automatic").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("track-picker-off").assertIsFocused()
        assertNoSelectionOrExit(automaticCalls, offCalls, trackCalls, exits)
        compose.onNodeWithTag("track-picker-off").performKeyInput { pressKey(Key.Enter) }
        assertReturned()
        compose.onNodeWithTag("return-launcher").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, resources.getString(R.string.playback_off)))
        compose.runOnIdle { assertEquals(1, offCalls); assertEquals(0, automaticCalls) }

        openFromLauncher()
        awaitFocused("track-picker-off")
        compose.onNodeWithTag("track-picker-off").performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithTag("track-picker-automatic").assertIsFocused()
        compose.runOnIdle { assertEquals(1, offCalls); assertEquals(0, automaticCalls) }
        compose.onNodeWithTag("track-picker-automatic").performKeyInput { pressKey(Key.Enter) }
        assertReturned()
        compose.onNodeWithTag("return-launcher").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, resources.getString(R.string.playback_automatic)))
        compose.runOnIdle {
            assertEquals(4, opened); assertEquals(2, dismissed)
            assertEquals(1, offCalls); assertEquals(1, automaticCalls)
            assertEquals(0, trackCalls); assertEquals(0, exits)
        }
    }

    private fun assertNoSelectionOrExit(automatic: Int, off: Int, track: Int, exits: Int) {
        assertEquals(0, automatic); assertEquals(0, off); assertEquals(0, track); assertEquals(0, exits)
    }

    private fun openFromLauncher() {
        compose.onNodeWithTag("return-launcher").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
    }

    private fun assertReturned() {
        compose.onNodeWithTag("track-picker").assertDoesNotExist()
        compose.waitUntil(5_000) { compose.runOnIdle { host.hasWindowFocus() } }
        awaitFocused("return-launcher")
    }

    private fun awaitFocused(tag: String) {
        compose.waitUntil(5_000) {
            compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Focused]
        }
        compose.onNodeWithTag(tag).assertIsFocused()
    }
}
