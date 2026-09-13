package dev.tyfino.foundation.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackTrackSelectionAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun exposesAutomaticAndOffSelectionState() {
        compose.setContent {
            MaterialTheme {
                TrackPickerDialog(
                    title = "Subtitles",
                    options = emptyList(),
                    automaticSelected = true,
                    includeOff = true,
                    offSelected = false,
                    noTracksMessage = "No subtitles available",
                    onAutomatic = {},
                    onOff = {},
                    onTrack = {},
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithText("Automatic — selected").assertIsSelected()
        compose.onNodeWithText("Off").assertIsNotSelected()
    }
}
