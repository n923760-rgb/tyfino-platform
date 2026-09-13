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
    fun cancelPreservesAccountAndOnlyExplicitConfirmationRemovesIt() {
        var removals = 0
        compose.setContent {
            MaterialTheme {
                SettingsScreen(
                    onOpenAccountSwitcher = {},
                    onRemoveXtreamAccount = { removals++ },
                )
            }
        }

        compose.onNodeWithTag("xtream-logout").performClick()
        compose.onNodeWithTag("xtream-keep-account").performClick()
        compose.runOnIdle { assertEquals(0, removals) }

        compose.onNodeWithTag("xtream-logout").performClick()
        compose.onNodeWithTag("xtream-confirm-remove").performClick()
        compose.runOnIdle { assertEquals(1, removals) }
    }
}
