package dev.tyfino.foundation.ui.screen

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalView
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import org.junit.Assert.assertTrue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.xtream.XtreamAccountSummary
import dev.tyfino.foundation.xtream.XtreamAccountsSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class XtreamAccountManagerTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var inputHost: View
    private lateinit var inputMode: InputModeManager
    private var mounted by mutableStateOf(false)


    @Before
    fun useDpadInputMode() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(false)
    }

    @After
    fun restoreTouchInputMode() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
    }

    @Test
    fun cancellationPreservesAndConfirmationRemovesOnlyTheChosenAccount() {
        val active = account("active", "one")
        val inactive = account("inactive", "two")
        var removedAccountId: String? = null

        compose.setContent { InputHost {
            MaterialTheme {
                XtreamAccountManager(
                    snapshot = XtreamAccountsSnapshot(active.accountId, listOf(active, inactive)),
                    removingAccountId = null,
                    storageError = false,
                    onRemoveAccount = { removedAccountId = it },
                    onBack = {},
                )
            }
        } }
        prepareKeyboard()

        compose.waitForIdle()
        awaitInitialFocus("xtream-manage-back")
        compose.onNodeWithTag("xtream-manage-back").assertIsFocused()
        compose.onNodeWithTag("xtream-remove-inactive").performClick()
        compose.waitForIdle()
        awaitInitialFocus("xtream-keep-account")
        compose.onNodeWithTag("xtream-keep-account").assertIsFocused()
        compose.onNodeWithTag("xtream-keep-account").performClick()
        compose.runOnIdle { assertNull(removedAccountId) }

        compose.onNodeWithTag("xtream-remove-inactive").performClick()
        compose.onNodeWithTag("xtream-confirm-remove").assertExists().performClick()
        compose.runOnIdle { assertEquals("inactive", removedAccountId) }
    }

    @Composable
    private fun InputHost(content: @Composable () -> Unit) {
        inputHost = LocalView.current
        inputMode = LocalInputModeManager.current
        MaterialTheme { FocusVisibleButton("Input setup", {}) }
        if (mounted) content()
    }

    private fun prepareKeyboard() {
        compose.waitUntil(5_000) { compose.runOnIdle { inputHost.hasWindowFocus() } }
        compose.runOnIdle { assertTrue("Mounted host must accept keyboard mode",
            inputMode.requestInputMode(InputMode.Keyboard)) }
        compose.waitUntil(5_000) { compose.runOnIdle { inputMode.inputMode == InputMode.Keyboard } }
        compose.runOnIdle { mounted = true }
    }

    private fun awaitInitialFocus(tag: String) {
        compose.waitUntil(5_000) {
            compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Focused]
        }
    }

    private fun account(id: String, number: String) = XtreamAccountSummary(
        accountId = id,
        providerOrigin = "https://provider-$number.example",
        username = "user-$number",
    )
}
