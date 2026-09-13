package dev.tyfino.foundation.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.tyfino.foundation.xtream.LiveEpgFailure
import dev.tyfino.foundation.xtream.LiveEpgProgram
import dev.tyfino.foundation.xtream.LiveEpgState
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveEpgDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun showsCurrentNextAndSavedScheduleAfterRefreshFailure() {
        val now = System.currentTimeMillis()
        val programs = listOf(
            LiveEpgProgram("account", "channel", "Current show", null, now - 60_000, now + 3_600_000),
            LiveEpgProgram("account", "channel", "Next show", "Description", now + 3_600_000, now + 7_200_000),
        )
        var state by mutableStateOf<LiveEpgState>(LiveEpgState.Content(programs, now, false))
        var refreshes = 0
        compose.setContent {
            MaterialTheme { LiveEpgDialog(state, { refreshes++ }, {}) }
        }
        compose.onNodeWithText("On now").assertExists()
        compose.onNodeWithText("Up next").assertExists()
        compose.onNodeWithTag("epg-program-0")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithTag("epg-program-0").assertIsFocused()
        compose.onNodeWithTag("epg-refresh").performClick()
        compose.runOnIdle { assertEquals(1, refreshes) }
        compose.runOnIdle { state = LiveEpgState.Stale(programs, now, LiveEpgFailure.Timeout) }
        compose.onNodeWithTag("epg-stale")
            .assertExists()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Assertive,
                ),
            )
    }

    @Test
    fun distinguishesEmptyFromFailureAndAllowsDismissal() {
        val now = System.currentTimeMillis()
        var state by mutableStateOf<LiveEpgState>(LiveEpgState.Empty(now, false))
        var dismissed = 0
        compose.setContent {
            MaterialTheme { LiveEpgDialog(state, {}, { dismissed++ }) }
        }
        compose.onNodeWithTag("epg-empty")
            .assertExists()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Polite,
                ),
            )
        compose.runOnIdle { state = LiveEpgState.Error(LiveEpgFailure.NetworkUnavailable) }
        compose.onNodeWithTag("epg-error")
            .assertExists()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Assertive,
                ),
            )
        compose.onNodeWithText("Close").performClick()
        compose.runOnIdle { assertEquals(1, dismissed) }
    }

    @Test
    fun exposesLoadingAsPoliteLiveRegion() {
        compose.setContent {
            MaterialTheme { LiveEpgDialog(LiveEpgState.Loading, {}, {}) }
        }

        compose.onNodeWithTag("epg-loading").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Polite,
            ),
        )
    }

    @Test
    fun programRowAnnouncesBoundedDisplayContentWithoutOwnerIds() {
        val format = SimpleDateFormat("HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val program = LiveEpgProgram(
            accountId = "secret-account",
            channelId = "secret-channel",
            title = "Current show",
            description = "Program description",
            startEpochMillis = 0L,
            endEpochMillis = 3_600_000L,
        )
        compose.setContent {
            MaterialTheme { EpgProgramRow(0, program, format) }
        }

        compose.onNodeWithTag("epg-program-0").assertContentDescriptionEquals(
            "00:00 – 01:00, Current show, Program description",
        )
    }
}
