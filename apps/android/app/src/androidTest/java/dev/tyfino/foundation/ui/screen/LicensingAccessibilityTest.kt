package dev.tyfino.foundation.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.AnnotatedString
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.tyfino.foundation.licensing.LicensingUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LicensingAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun hardwareBackReturnsFromActivationAndDisposesEnteredCode() {
        var state: LicensingUiState by mutableStateOf(LicensingUiState.ActivationEntry)
        var backs = 0
        compose.setContent {
            MaterialTheme {
                LicensingScreen(state, onStartTrial = {}, onShowActivation = {},
                    onBack = { backs++; state = LicensingUiState.Choice },
                    onActivate = {}, onRetry = {})
            }
        }
        compose.onNodeWithTag("activation-code").performTextInput("TYFINO-TEST")
        // Dismiss the IME first: an IME-owned Back should not leave the form.
        closeSoftKeyboard()
        pressBack()
        compose.onNodeWithTag("start-trial").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("activation-code").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, backs); state = LicensingUiState.ActivationEntry }
        compose.onNodeWithTag("activation-code").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
    }

    @Test
    fun hardwareBackDuringLicensingWorkKeepsProgressWithoutInvokingActions() {
        var actions = 0
        compose.setContent {
            MaterialTheme {
                LicensingScreen(LicensingUiState.Working,
                    onStartTrial = { actions++ }, onShowActivation = { actions++ },
                    onBack = { actions++ }, onActivate = { actions++ }, onRetry = { actions++ })
            }
        }
        compose.onNodeWithTag("licensing-status").assertIsDisplayed()
        pressBack()
        compose.onNodeWithTag("licensing-status").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, actions) }
    }

    @Test
    fun exposesLicensingFailureAsAssertiveLiveRegion() {
        compose.setContent {
            MaterialTheme {
                LicensingScreen(
                    state = LicensingUiState.Failure(
                        code = "ACTIVATION_REJECTED",
                        retryable = false,
                    ),
                    onStartTrial = {},
                    onShowActivation = {},
                    onBack = {},
                    onActivate = {},
                    onRetry = {},
                )
            }
        }

        compose.onNodeWithTag("licensing-error").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Assertive,
            ),
        )
    }

    @Test
    fun exposesLicensingProgressAsPoliteLiveRegion() {
        compose.setContent {
            MaterialTheme {
                LicensingScreen(
                    state = LicensingUiState.Working,
                    onStartTrial = {},
                    onShowActivation = {},
                    onBack = {},
                    onActivate = {},
                    onRetry = {},
                )
            }
        }

        compose.onNodeWithTag("licensing-status").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Polite,
            ),
        )
    }

    @Test
    fun announcesInvalidActivationCode() {
        compose.setContent {
            MaterialTheme {
                LicensingScreen(
                    state = LicensingUiState.ActivationEntry,
                    onStartTrial = {},
                    onShowActivation = {},
                    onBack = {},
                    onActivate = {},
                    onRetry = {},
                )
            }
        }

        compose.onNodeWithTag("activate").performClick()
        compose.onNodeWithTag(
            testTag = "activation-code-error",
            useUnmergedTree = true,
        ).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Assertive,
            ),
        )
    }
}
