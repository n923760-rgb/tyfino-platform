package dev.tyfino.foundation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppShellSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun shellStartsAndNavigatesToSettings() {
        composeRule.onNodeWithTag("app-shell").assertIsDisplayed()
        composeRule.onNodeWithTag("foundation-screen").assertIsDisplayed()

        composeRule.onNodeWithTag("destination-settings").performClick()

        composeRule.onNodeWithTag("settings-screen").assertIsDisplayed()
    }
}
