package dev.tyfino.foundation.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.tyfino.foundation.licensing.LicensingUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LicensingAccessibilityTest {
    @get:Rule val compose = createComposeRule()

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
        compose.onNodeWithTag("activation-code-error").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Assertive,
            ),
        )
    }
}
