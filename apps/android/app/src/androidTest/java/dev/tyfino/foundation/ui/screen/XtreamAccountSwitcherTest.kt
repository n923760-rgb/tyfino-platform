package dev.tyfino.foundation.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.tyfino.foundation.xtream.XtreamAccountSummary
import dev.tyfino.foundation.xtream.XtreamAccountsSnapshot
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class XtreamAccountSwitcherTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun activeSelectionAndAllActionsAreAccessible() {
        val active = account("active", "one")
        val inactive = account("inactive", "two")
        var selectedAccountId: String? = null
        var additions = 0
        var managementRequests = 0

        compose.setContent {
            MaterialTheme {
                XtreamAccountSwitcher(
                    snapshot = XtreamAccountsSnapshot(active.accountId, listOf(active, inactive)),
                    switchingAccountId = null,
                    storageError = false,
                    onSelectAccount = { selectedAccountId = it },
                    onAddAccount = { additions++ },
                    onManageAccounts = { managementRequests++ },
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithTag("xtream-account-active").assertIsSelected()
        compose.onNodeWithTag("xtream-account-inactive")
            .assertIsNotSelected()
            .assertIsFocused()
            .performClick()
        compose.onNodeWithTag("xtream-add-account").performClick()
        compose.onNodeWithTag("xtream-manage-accounts").performClick()

        compose.runOnIdle {
            assertEquals("inactive", selectedAccountId)
            assertEquals(1, additions)
            assertEquals(1, managementRequests)
        }
    }

    @Test
    fun mandatoryChooserCannotBeClosedWithoutSelectingOrAddingAnAccount() {
        val account = account("saved", "one")
        compose.setContent {
            MaterialTheme {
                XtreamAccountSwitcher(
                    snapshot = XtreamAccountsSnapshot(null, listOf(account)),
                    switchingAccountId = null,
                    storageError = false,
                    onSelectAccount = {},
                    onAddAccount = {},
                    onManageAccounts = {},
                    onDismiss = {},
                    dismissible = false,
                )
            }
        }

        compose.onNodeWithTag("xtream-account-saved").assertIsFocused()
        compose.onNodeWithTag("xtream-close-switcher").assertDoesNotExist()
    }

    private fun account(id: String, number: String) = XtreamAccountSummary(
        accountId = id,
        providerOrigin = "https://provider-$number.example",
        username = "user-$number",
    )
}
