package dev.tyfino.foundation.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.tyfino.foundation.xtream.LiveEpgFailure
import dev.tyfino.foundation.xtream.LiveEpgProgram
import dev.tyfino.foundation.xtream.LiveEpgState
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
        compose.onAllNodesWithText("Current show").assertCountEquals(2)
        compose.onAllNodesWithText("Next show").assertCountEquals(2)
        compose.onNodeWithTag("epg-refresh").assertIsFocused().performClick()
        compose.runOnIdle { assertEquals(1, refreshes) }
        compose.runOnIdle { state = LiveEpgState.Stale(programs, now, LiveEpgFailure.Timeout) }
        compose.onNodeWithTag("epg-stale").assertExists()
        compose.onAllNodesWithText("Current show").assertCountEquals(2)
    }

    @Test
    fun distinguishesEmptyFromFailureAndAllowsDismissal() {
        val now = System.currentTimeMillis()
        var state by mutableStateOf<LiveEpgState>(LiveEpgState.Empty(now, false))
        var dismissed = 0
        compose.setContent {
            MaterialTheme { LiveEpgDialog(state, {}, { dismissed++ }) }
        }
        compose.onNodeWithTag("epg-empty").assertExists()
        compose.runOnIdle { state = LiveEpgState.Error(LiveEpgFailure.NetworkUnavailable) }
        compose.onNodeWithTag("epg-error").assertExists()
        compose.onNodeWithText("Close").performClick()
        compose.runOnIdle { assertEquals(1, dismissed) }
    }
}
