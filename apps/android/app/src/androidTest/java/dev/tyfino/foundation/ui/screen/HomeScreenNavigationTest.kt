package dev.tyfino.foundation.ui.screen

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenNavigationTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun emptyHomeHidesShelvesAndKeepsAccountActionsReachable() {
        var openedAccountSwitcher = false
        var openedSettings = false
        composeRule.setContent {
            TyfinoTheme {
                HomeScreen(
                    onOpenAccountSwitcher = { openedAccountSwitcher = true },
                    onOpenSettings = { openedSettings = true },
                )
            }
        }

        for (tag in listOf("home-latest-movies", "home-latest-series", "home-recent-live",
            "home-recent-series", "home-recent-movies")) {
            composeRule.onNodeWithTag(tag).assertDoesNotExist()
        }
        composeRule.onNodeWithTag("home-screen").performScrollToNode(hasTestTag("home-empty-content"))
        composeRule.onNodeWithTag("home-empty-content").assertExists()
        for (route in listOf("live", "movies", "series")) {
            composeRule.onNodeWithTag("home-$route").assertDoesNotExist()
        }
        composeRule.onNodeWithTag("open-account-switcher").assertIsDisplayed()
        composeRule.onNodeWithTag("home-open-settings").assertIsDisplayed()
        composeRule.onNodeWithTag("home-screen").performScrollToNode(hasTestTag("open-account-switcher"))
        composeRule.onNodeWithTag("open-account-switcher")
            .assertHasClickAction().performClick()
        composeRule.onNodeWithTag("home-screen").performScrollToNode(hasTestTag("home-open-settings"))
        composeRule.onNodeWithTag("home-open-settings")
            .assertHasClickAction().performClick()
        assertEquals(true, openedAccountSwitcher)
        assertEquals(true, openedSettings)
    }
}
