package dev.tyfino.foundation.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun useDpadInputMode() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(false)
        compose.activity.window.decorView.requestFocus()
    }

    @After
    fun restoreTouchInputMode() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
    }

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

    @Test
    fun accountSwitcherReceivesInitialDpadFocus() {
        compose.setContent {
            MaterialTheme {
                SettingsScreen(onOpenAccountSwitcher = {}, onManageAccounts = {})
            }
        }

        compose.onNodeWithTag("open-account-switcher").assertIsFocused()
    }
}
