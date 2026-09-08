package dev.tyfino.foundation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppShellSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun firstRunWaitsForExplicitLicensingChoice() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("start-trial").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("licensing-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("start-trial").assertIsDisplayed()
        composeRule.onNodeWithTag("activate-now").assertIsDisplayed()
    }
}
