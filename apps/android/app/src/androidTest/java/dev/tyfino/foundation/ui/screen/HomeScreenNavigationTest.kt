package dev.tyfino.foundation.ui.screen

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.tyfino.foundation.app.AppDestination
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenNavigationTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun homeShortcutsAndAccountActionsOpenTheIntendedDestinations() {
        var destination: AppDestination? = null
        var openedAccountSwitcher = false
        var openedSettings = false
        composeRule.setContent {
            TyfinoTheme {
                HomeScreen(
                    onOpenDestination = { destination = it },
                    onOpenAccountSwitcher = { openedAccountSwitcher = true },
                    onOpenSettings = { openedSettings = true },
                )
            }
        }

        for (expected in listOf(AppDestination.Live, AppDestination.Movies, AppDestination.Series)) {
            composeRule.onNodeWithTag("home-screen").performScrollToNode(hasTestTag("home-${expected.route}"))
            composeRule.onNodeWithTag("home-${expected.route}")
                .assertHasClickAction().performClick()
            assertEquals(expected, destination)
        }
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
