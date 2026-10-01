package dev.tyfino.foundation.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.R
import dev.tyfino.foundation.licensing.EntitlementKind
import dev.tyfino.foundation.licensing.LicensingUiState
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import dev.tyfino.foundation.xtream.CatalogSection
import dev.tyfino.foundation.xtream.XtreamUiState
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommercialExperienceTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun compactArabicLicensingKeepsBothChoicesExplicitAndReachable() {
        var activations = 0
        var trials = 0
        compose.setContent {
            CompactArabic {
                LicensingScreen(LicensingUiState.Choice,
                    onStartTrial = { trials++ }, onShowActivation = { activations++ },
                    onBack = {}, onActivate = {}, onRetry = {})
            }
        }
        compose.runOnIdle { assertEquals(0, activations); assertEquals(0, trials) }
        compose.onNodeWithTag("activate-now").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("start-trial").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, activations); assertEquals(1, trials) }
    }

    @Test fun compactArabicHomeShortcutsEmitTheirExactDestination() {
        val destinations = mutableListOf<CatalogSection>()
        compose.setContent {
            CompactArabic {
                HomeScreen(onOpenAccountSwitcher = {}, onOpenSettings = {},
                    onOpenCatalog = { destinations += it })
            }
        }
        CatalogSection.entries.forEach { section ->
            val tag = "home-browse-${section.name.lowercase()}"
            compose.onNodeWithTag("home-screen").performScrollToNode(hasTestTag(tag))
            compose.onNodeWithTag(tag).assertIsDisplayed().performClick()
        }
        compose.runOnIdle { assertEquals(CatalogSection.entries.toList(), destinations) }
    }

    @Test fun annualLicenseDisplaysServerExpiryAndVerifiedStatus() {
        val expiry = 1_893_456_000_000L
        compose.setContent { TyfinoTheme { LicenseSummaryPanel(LicenseSummary(EntitlementKind.OneYear, expiry, false)) } }
        compose.onNodeWithTag("license-summary-plan").assertTextEquals(context.getString(R.string.product_license_year))
        val locale = context.resources.configuration.locales[0]
        val date = DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(expiry))
        compose.onNodeWithTag("license-summary-expiry").assertTextEquals(context.getString(R.string.product_license_expires, date))
        compose.onNodeWithTag("license-summary-status").assertTextEquals(context.getString(R.string.product_license_active))
    }

    @Test fun lifetimeOfflineLicenseShowsNoFiniteExpiryOrFreshVerificationClaim() {
        compose.setContent { TyfinoTheme { LicenseSummaryPanel(LicenseSummary(EntitlementKind.Lifetime, null, true)) } }
        compose.onNodeWithTag("license-summary-plan").assertTextEquals(context.getString(R.string.product_license_lifetime))
        compose.onNodeWithTag("license-summary-expiry").assertDoesNotExist()
        compose.onNodeWithTag("license-summary-status").assertTextEquals(context.getString(R.string.product_license_offline))
    }

    @Test fun trialLicenseRetainsTrialLabel() {
        compose.setContent { TyfinoTheme { LicenseSummaryPanel(LicenseSummary(EntitlementKind.Trial, 1_893_456_000_000L, false)) } }
        compose.onNodeWithTag("license-summary-plan").assertTextEquals(context.getString(R.string.product_license_trial))
    }

    @Test fun passwordVisibilityIsExplicitAndResetsWhenGateStateChanges() {
        var state by mutableStateOf<XtreamUiState>(XtreamUiState.SignedOut())
        compose.setContent { TyfinoTheme {
            XtreamLoginScreen(state, onSignIn = {}, onConfirmCleartext = {}, onCancelCleartext = {})
        } }
        val password = compose.onNodeWithTag("xtream-password")
        password.performScrollTo().performTextInput("fixture-password")
        password.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        compose.onNodeWithTag("xtream-password-visibility").performScrollTo().performClick()
        password.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Password))
        compose.runOnIdle { state = XtreamUiState.ConfirmCleartext("http://provider.example") }
        password.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
    }

    @Test fun passwordImeRespectsPendingCleartextConsent() {
        var signIns = 0
        var confirmations = 0
        compose.setContent { TyfinoTheme {
            XtreamLoginScreen(XtreamUiState.ConfirmCleartext("http://provider.example"),
                onSignIn = { signIns++ }, onConfirmCleartext = { confirmations++ }, onCancelCleartext = {})
        } }
        compose.runOnIdle { assertEquals(0, signIns); assertEquals(0, confirmations) }
        compose.onNodeWithTag("xtream-password").performScrollTo().performImeAction()
        compose.runOnIdle { assertEquals(0, signIns); assertEquals(1, confirmations) }
    }

    @Composable private fun CompactArabic(content: @Composable () -> Unit) {
        val resources = context.createConfigurationContext(
            Configuration(context.resources.configuration).apply { setLocale(Locale("ar")) }
        ).resources
        CompositionLocalProvider(LocalResources provides resources,
            LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalDensity provides Density(context.resources.displayMetrics.density, 1.6f)) {
            TyfinoTheme { Box(Modifier.width(280.dp).height(360.dp)) { content() } }
        }
    }
}
