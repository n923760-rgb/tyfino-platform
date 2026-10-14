package dev.tyfino.foundation.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import dev.tyfino.foundation.BuildConfig
import dev.tyfino.foundation.R
import dev.tyfino.foundation.xtream.ProviderUserAgent
import dev.tyfino.foundation.xtream.ProviderUserAgentPreset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun actualSettingsExposeCurrentBuildWithoutInvokingAccountActions() {
        var requests = 0
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        compose.setContent { TyfinoTheme { Box(Modifier.width(280.dp).height(320.dp)) {
            SettingsScreen({ requests++ }, { requests++ })
        } } }
        compose.onNodeWithTag("settings-app-info-heading").performScrollTo().assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Heading, Unit))
        compose.onNodeWithTag("settings-app-version").performScrollTo().assertIsDisplayed()
            .assertTextEquals(resources.getString(R.string.settings_app_version,
                BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
        compose.onNodeWithTag("settings-app-development").performScrollTo().assertIsDisplayed()
            .assertTextEquals(resources.getString(R.string.settings_app_development))
        compose.runOnIdle { assertEquals(0, requests) }
    }

    @Test fun arabicFontTwoSettingsKeepBuildInformationReachableAndUseActualTextDensity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resources = context.createConfigurationContext(Configuration(context.resources.configuration)
            .apply { setLocale(Locale.forLanguageTag("ar")) }).resources
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources,
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                TyfinoTheme { Box(Modifier.width(280.dp).height(320.dp)) {
                    SettingsScreen({}, {})
                } }
            }
        }
        compose.onNodeWithTag("settings-app-info-heading").performScrollTo().assertIsDisplayed()
            .assertTextEquals(resources.getString(R.string.settings_app_info))
        compose.onNodeWithTag("settings-app-version").performScrollTo().assertIsDisplayed()
            .assertTextEquals(resources.getString(R.string.settings_app_version,
                BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                val layouts = mutableListOf<TextLayoutResult>()
                assertTrue(action(layouts))
                assertEquals(2f, layouts.single().layoutInput.density.fontScale, 0f)
            }
        compose.onNodeWithTag("settings-app-development").performScrollTo().assertIsDisplayed()
    }

    @Test fun nonDebugPresentationOmitsDevelopmentNoticeWithoutClaimingReleaseQualification() {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        compose.setContent { TyfinoTheme { SettingsAppInfo("2.0.0", 42, development = false) } }
        compose.onNodeWithTag("settings-app-version")
            .assertTextEquals(resources.getString(R.string.settings_app_version, "2.0.0", 42))
        compose.onNodeWithTag("settings-app-development").assertDoesNotExist()
    }

    @Before
    fun useDpadInputMode() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(false)
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
            Dialog(onDismissRequest = {}) {
                MaterialTheme {
                    SettingsScreen(onOpenAccountSwitcher = {}, onManageAccounts = {})
                }
            }
        }

        // A Dialog owns a separate window. Compose idleness can precede that window's
        // focus callback, which is when the screen requests its initial D-pad focus.
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("open-account-switcher")
                .fetchSemanticsNodes()
                .any { it.config[SemanticsProperties.Focused] }
        }
        compose.onNodeWithTag("open-account-switcher").assertIsFocused()
    }

    @Test
    fun userAgentChoicePersistsAcrossSettingsVisits() {
        val store = ProviderUserAgent(InstrumentationRegistry.getInstrumentation().targetContext)
        val original = store.selected()
        try {
            compose.setContent {
                MaterialTheme { SettingsScreen(onOpenAccountSwitcher = {}, onManageAccounts = {}) }
            }
            compose.onNodeWithTag("settings-user-agent-vlc").performScrollTo().performClick()
            compose.runOnIdle {
                assertEquals(ProviderUserAgentPreset.Vlc, store.selected())
                assertEquals("VLC/3.0.0", store.header())
            }
        } finally {
            store.select(original)
        }
    }

    @Test
    fun compactArabicSettingsKeepBothSectionsAndActionsReachable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply { setLocale(Locale("ar")) }
        val arabicResources = context.createConfigurationContext(configuration).resources
        val density = Density(context.resources.displayMetrics.density, 1.6f)
        val store = ProviderUserAgent(context)
        val original = store.selected()
        var switcherRequests = 0
        var managementRequests = 0
        try {
            compose.setContent {
                CompositionLocalProvider(
                    LocalResources provides arabicResources,
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalDensity provides density,
                ) {
                    TyfinoTheme {
                        Box(Modifier.width(280.dp).height(360.dp)) {
                            SettingsScreen(
                                onOpenAccountSwitcher = { switcherRequests++ },
                                onManageAccounts = { managementRequests++ },
                            )
                        }
                    }
                }
            }

            val accounts = compose.onNodeWithTag("settings-accounts-heading")
            val provider = compose.onNodeWithTag("settings-provider-heading")
            val heading = SemanticsMatcher.expectValue(SemanticsProperties.Heading, Unit)
            accounts.assert(heading).assertTextEquals("الحسابات")
            provider.assert(heading).assertTextEquals("توافق المزوّد")
            assertTrue(accounts.fetchSemanticsNode().positionInRoot.y < provider.fetchSemanticsNode().positionInRoot.y)
            accounts.performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("open-account-switcher").performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithTag("open-account-manager").performScrollTo().assertIsDisplayed().performClick()
            provider.performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("settings-user-agent-vlc").performScrollTo().assertIsDisplayed()
                .performClick().assertIsSelected()
            compose.onNodeWithTag("settings-user-agent-tyfino").assertIsNotSelected()
            compose.runOnIdle {
                assertEquals(1, switcherRequests)
                assertEquals(1, managementRequests)
                assertEquals(ProviderUserAgentPreset.Vlc, store.selected())
            }
        } finally {
            store.select(original)
        }
    }

    @Test
    fun wideSettingsContentIsCappedAndCentered() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val density = Density(context.resources.displayMetrics.density / 3f, 1f)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                TyfinoTheme {
                    Box(Modifier.width(960.dp).height(640.dp)) {
                        SettingsScreen(onOpenAccountSwitcher = {}, onManageAccounts = {})
                    }
                }
            }
        }

        val screen = compose.onNodeWithTag("settings-screen").fetchSemanticsNode().boundsInRoot
        val section = compose.onNodeWithTag("settings-accounts-section").fetchSemanticsNode().boundsInRoot
        assertEquals(960f, screen.width / density.density, 1f)
        assertEquals(840f, section.width / density.density, 1f)
        assertEquals(section.left - screen.left, screen.right - section.right, 1f)
        compose.onNodeWithTag("open-account-switcher").assertIsDisplayed()
        compose.onNodeWithTag("open-account-manager").assertIsDisplayed()
    }
}
