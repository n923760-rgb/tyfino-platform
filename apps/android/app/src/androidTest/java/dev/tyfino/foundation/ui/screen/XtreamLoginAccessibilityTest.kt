package dev.tyfino.foundation.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.tyfino.foundation.xtream.XtreamFailure
import dev.tyfino.foundation.xtream.XtreamUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class XtreamLoginAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun exposesAuthenticationErrorAsAssertiveLiveRegion() {
        compose.setContent {
            MaterialTheme {
                XtreamLoginScreen(
                    state = XtreamUiState.SignedOut(XtreamFailure.InvalidCredentials),
                    onSignIn = {},
                    onConfirmCleartext = {},
                    onCancelCleartext = {},
                )
            }
        }

        compose.onNodeWithTag("xtream-error").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Assertive,
            ),
        )
    }
}
