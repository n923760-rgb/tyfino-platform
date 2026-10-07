package dev.tyfino.foundation.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import dev.tyfino.foundation.xtream.XtreamAccountSummary
import dev.tyfino.foundation.xtream.XtreamAccountsSnapshot
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class XtreamRemovalConfirmationTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val username = "اختبار".repeat(200)
    private val active = account("active", "active")
    private val target = account("target", "target")
    private val removed = mutableListOf<String>()

    @Before fun useKeyboardInput() {
        instrumentation.setInTouchMode(false)
    }

    @After fun restoreTouchInput() {
        instrumentation.setInTouchMode(true)
    }

    @Test fun largeArabicConfirmationScrollsAndCancelPreservesTheExactAccount() {
        renderManager()
        compose.onNodeWithTag("xtream-remove-target")
            .performScrollTo().assertIsDisplayed().performClick()
        assertArabicConfirmation()
        compose.onNodeWithTag("xtream-keep-account")
            .performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("xtream-remove-confirmation").assertDoesNotExist()
        compose.runOnIdle { assertTrue(removed.isEmpty()) }

        compose.onNodeWithTag("xtream-remove-target")
            .performScrollTo().assertIsDisplayed().performClick()
        assertArabicConfirmation()
        compose.onNodeWithTag("xtream-confirm-remove")
            .performScrollTo().assertIsDisplayed()
        assertConfirmationScrolled()
        compose.onNodeWithTag("xtream-confirm-remove").performClick()
        compose.onNodeWithTag("xtream-remove-confirmation").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf("target"), removed) }
    }

    @Test fun directionalInputStartsAtKeepAndRemovesOnlyAfterExplicitEnter() {
        renderManager()
        compose.onNodeWithTag("xtream-remove-target")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            .assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        assertArabicConfirmation()
        compose.onNodeWithTag("xtream-keep-account").assertIsFocused()
        compose.runOnIdle { assertTrue(removed.isEmpty()) }
        compose.onNodeWithTag("xtream-keep-account")
            .performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("xtream-confirm-remove").assertIsFocused().assertIsDisplayed()
        assertConfirmationScrolled()
        compose.runOnIdle { assertTrue(removed.isEmpty()) }
        compose.onNodeWithTag("xtream-confirm-remove").performKeyInput { pressKey(Key.Enter) }
        compose.onNodeWithTag("xtream-remove-confirmation").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf("target"), removed) }
    }

    @Test fun actualConfirmationRendererRetainsArabicFontScaleInAShortViewport() {
        val resources = arabicResources()
        val message = resources.getString(
            R.string.xtream_confirm_remove_named_message,
            target.username,
            target.providerOrigin,
        )
        var kept = 0
        var confirmed = 0
        compose.setContent {
            CompositionLocalProvider(
                LocalResources provides resources,
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(LocalDensity.current.density, 2f),
            ) {
                TyfinoTheme {
                    Box(Modifier.width(280.dp).height(320.dp)) {
                        XtreamRemovalConfirmationContent(
                            account = target,
                            busy = false,
                            initialFocus = remember { FocusRequester() },
                            onKeep = { kept++ },
                            onRemove = { confirmed++ },
                        )
                    }
                }
            }
        }
        // Read the real Text layout input; an outer test density is not enough for a Dialog root.
        compose.onNodeWithText(message, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                val layouts = mutableListOf<TextLayoutResult>()
                assertTrue(action(layouts))
                assertEquals(2f, layouts.single().layoutInput.density.fontScale, 0f)
            }
        compose.onNodeWithTag("xtream-keep-account")
            .performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("xtream-confirm-remove")
            .performScrollTo().assertIsDisplayed()
        assertConfirmationScrolled()
        compose.runOnIdle { assertEquals(1, kept); assertEquals(0, confirmed) }
        compose.onNodeWithTag("xtream-confirm-remove").performClick()
        compose.runOnIdle { assertEquals(1, kept); assertEquals(1, confirmed) }
    }

    private fun renderManager() {
        val resources = arabicResources()
        compose.setContent {
            CompositionLocalProvider(
                LocalResources provides resources,
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                TyfinoTheme {
                    XtreamAccountManager(
                        snapshot = XtreamAccountsSnapshot(active.accountId, listOf(active, target)),
                        removingAccountId = null,
                        storageError = false,
                        onRemoveAccount = { removed += it },
                        onBack = {},
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("xtream-remove-target").assertExists()
    }

    private fun arabicResources() = instrumentation.targetContext.let { context ->
        context.createConfigurationContext(
            Configuration(context.resources.configuration).apply { setLocale(Locale("ar")) },
        ).resources
    }

    private fun assertArabicConfirmation() {
        compose.onNodeWithText(arabicResources().getString(R.string.xtream_confirm_remove_title))
            .assertExists()
    }

    private fun assertConfirmationScrolled() {
        val range = compose.onNodeWithTag("xtream-remove-confirmation-scroll")
            .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue("Long confirmation content must overflow its viewport", range.maxValue() > 0f)
        assertTrue("The actual confirmation viewport must scroll to its actions", range.value() > 0f)
    }

    private fun account(id: String, origin: String) = XtreamAccountSummary(
        accountId = id,
        providerOrigin = "https://fixture-$origin-with-a-long-provider-name.example:8443",
        username = username,
    )
}
