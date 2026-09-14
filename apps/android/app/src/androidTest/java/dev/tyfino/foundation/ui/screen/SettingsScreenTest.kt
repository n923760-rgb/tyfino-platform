package dev.tyfino.foundation.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun accountActionsOpenTheirDedicatedSurfaces() {
        var switcherRequests = 0
        var managementRequests = 0
        compose.setContent {
            MaterialTheme {
                SettingsScreen(
                    onOpenAccountSwitcher = { switcherRequests++ },
                    onManageAccounts = { managementRequests++ },
                )
            }
        }

        compose.onNodeWithTag("open-account-switcher").performClick()
        compose.onNodeWithTag("open-account-manager").performClick()
        compose.runOnIdle {
            assertEquals(1, switcherRequests)
            assertEquals(1, managementRequests)
        }
    }
}
