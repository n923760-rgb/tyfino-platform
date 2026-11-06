package dev.tyfino.foundation.ui.screen

import androidx.compose.ui.test.isFocusable
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Root
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.xtream.XtreamAccountSummary
import dev.tyfino.foundation.xtream.XtreamAccountsSnapshot
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class XtreamAccountSwitcherTest {
    @get:Rule val compose = createComposeRule()

    @Before
    fun useDpadInputMode() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(false)
    }

    @After
    fun restoreTouchInputMode() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
    }

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

        compose.waitForIdle()
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

        compose.waitForIdle()
        compose.onNodeWithTag("xtream-account-saved").assertIsFocused()
        compose.onNodeWithTag("xtream-close-switcher").assertDoesNotExist()
    }


    @Test fun snapshotRefreshRetainsDirectionalChoiceInLtr() = snapshotRefresh(LayoutDirection.Ltr)
    @Test fun snapshotRefreshRetainsDirectionalChoiceInRtl() = snapshotRefresh(LayoutDirection.Rtl)

    private fun snapshotRefresh(direction: LayoutDirection) {
        val active = account("active", "one")
        val first = account("first", "two")
        val second = account("second", "three")
        var snapshot by mutableStateOf(XtreamAccountsSnapshot(active.accountId, listOf(active, first, second)))
        var selected: String? = null
        var calls = 0
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                MaterialTheme { XtreamAccountSwitcher(snapshot, null, false,
                    { selected = it; calls++ }, {}, {}, {}) }
            }
        }
        compose.waitUntil(5_000) {
            compose.onNodeWithTag("xtream-account-first").fetchSemanticsNode()
                .config[SemanticsProperties.Focused]
        }
        compose.onNodeWithTag("xtream-account-first").assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        assertSecondFocused("after Down, before snapshot replacement")
        compose.runOnIdle {
            assertEquals(0, calls)
            snapshot = XtreamAccountsSnapshot(active.accountId, listOf(
                XtreamAccountSummary(accountId = active.accountId, providerOrigin = active.providerOrigin,
                    username = "updated-fixture"), first, second))
        }
        compose.waitForIdle()
        assertSecondFocused("after snapshot replacement")
        compose.runOnIdle { assertEquals(0, calls); assertEquals(null, selected) }
        compose.onNodeWithTag("xtream-account-second").performKeyInput { pressKey(Key.Enter) }
        compose.runOnIdle { assertEquals(1, calls); assertEquals("second", selected) }
    }

    private fun assertSecondFocused(stage: String) {
        try {
            // Preserve the original assertion/verdict before any extra observation runs.
            compose.onNodeWithTag("xtream-account-second").assertIsFocused()
        } catch (failure: AssertionError) {
            val details = mutableListOf("stage=$stage; observations are AFTER the failed verdict")
            runCatching {
                val nodes = compose.onAllNodes(isFocusable()).fetchSemanticsNodes().map { node ->
                    val tag = if (node.config.contains(SemanticsProperties.TestTag)) {
                        node.config[SemanticsProperties.TestTag]
                    } else "untagged"
                    "$tag:focused=${node.config[SemanticsProperties.Focused]},bounds=${node.boundsInRoot}"
                }
                details += "semantics=$nodes"
            }.onFailure { details += "semantics unavailable: ${it.javaClass.simpleName}" }
            val windows = mutableListOf<String>()
            runCatching {
                val dialog = object : TypeSafeMatcher<Root>() {
                    override fun describeTo(description: Description) {
                        description.appendText("account switcher dialog for post-failure observation")
                    }
                    override fun matchesSafely(root: Root): Boolean {
                        if (!isDialog().matches(root)) return false
                        val view = root.decorView
                        val state = "windowFocus=${view.hasWindowFocus()},touch=${view.isInTouchMode}," +
                            "attached=${view.isAttachedToWindow},size=${view.width}x${view.height}," +
                            "nativeFocus=${view.findFocus()?.javaClass?.simpleName}"
                        if (windows.lastOrNull() != state && windows.size < 8) windows += state
                        return true
                    }
                }
                onView(isRoot()).inRoot(dialog).check { _, error -> if (error != null) throw error }
            }.onFailure { details += "window observation unavailable: ${it.javaClass.simpleName}" }
            details += "windows=$windows"
            // Static fixture tags, geometry and focus facts only; never provider text or credentials.
            failure.addSuppressed(AssertionError(details.joinToString("; ")))
            throw failure
        }
    }

    private fun account(id: String, number: String) = XtreamAccountSummary(
        accountId = id,
        providerOrigin = "https://provider-$number.example",
        username = "user-$number",
    )
}
