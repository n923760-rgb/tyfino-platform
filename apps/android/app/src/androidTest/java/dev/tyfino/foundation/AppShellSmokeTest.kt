package dev.tyfino.foundation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppShellSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun installedApplicationUsesApprovedIdentity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val applicationInfo = context.applicationInfo

        assertEquals("com.tyfino.player", context.packageName)
        assertEquals("TYFINO", applicationInfo.loadLabel(context.packageManager).toString())
        assertTrue(applicationInfo.icon != 0)
        assertTrue(applicationInfo.banner != 0)
    }

    @Test
    fun firstRunWaitsForExplicitLicensingChoice() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("start-trial").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("licensing-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("start-trial").assertIsDisplayed()
        composeRule.onNodeWithTag("start-trial").assertHasClickAction()
        composeRule.onNodeWithTag("activate-now").assertIsDisplayed().assertHasClickAction()
    }
}
