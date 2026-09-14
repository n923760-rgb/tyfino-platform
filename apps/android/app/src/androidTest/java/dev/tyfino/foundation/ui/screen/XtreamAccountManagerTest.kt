package dev.tyfino.foundation.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.tyfino.foundation.xtream.XtreamAccountSummary
import dev.tyfino.foundation.xtream.XtreamAccountsSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class XtreamAccountManagerTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun cancellationPreservesAndConfirmationRemovesOnlyTheChosenAccount() {
        val active = account("active", "one")
        val inactive = account("inactive", "two")
        var removedAccountId: String? = null

        compose.setContent {
            MaterialTheme {
                XtreamAccountManager(
                    snapshot = XtreamAccountsSnapshot(active.accountId, listOf(active, inactive)),
                    removingAccountId = null,
                    storageError = false,
                    onRemoveAccount = { removedAccountId = it },
                    onBack = {},
                )
            }
        }

        compose.waitForIdle()
        compose.onNodeWithTag("xtream-manage-back").assertIsFocused()
        compose.onNodeWithTag("xtream-remove-inactive").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("xtream-keep-account").assertIsFocused()
        compose.onNodeWithTag("xtream-keep-account").performClick()
        compose.runOnIdle { assertNull(removedAccountId) }

        compose.onNodeWithTag("xtream-remove-inactive").performClick()
        compose.onNodeWithTag("xtream-confirm-remove").assertExists().performClick()
        compose.runOnIdle { assertEquals("inactive", removedAccountId) }
    }

    private fun account(id: String, number: String) = XtreamAccountSummary(
        accountId = id,
        providerOrigin = "https://provider-$number.example",
        username = "user-$number",
    )
}
