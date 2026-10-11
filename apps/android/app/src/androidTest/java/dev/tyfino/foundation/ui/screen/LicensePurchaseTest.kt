package dev.tyfino.foundation.ui.screen

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.R
import dev.tyfino.foundation.licensing.LicensingUiState
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LicensePurchaseTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun purchaseIsExplicitAndSendsOnlyTheApprovedContactMessage() {
        var captured: Intent? = null
        var licensingActions = 0
        val launcher = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) { captured = intent }
        }
        compose.setContent { TyfinoTheme {
            LicensingScreen(LicensingUiState.Choice,
                onStartTrial = { licensingActions++ }, onShowActivation = { licensingActions++ },
                onBack = {}, onActivate = { licensingActions++ }, onRetry = { licensingActions++ },
                onOpenPurchase = { LicensePurchase.open(launcher) })
        } }
        compose.runOnIdle { assertNull(captured); assertEquals(0, licensingActions) }
        compose.onNodeWithTag("purchase-license").performScrollTo().performClick()
        compose.onNodeWithTag("license-purchase-phone").assertTextEquals(LicensePurchase.PHONE)
        compose.runOnIdle { assertNull(captured) }
        compose.onNodeWithTag("license-purchase-open").performScrollTo().performClick()
        compose.runOnIdle {
            val intent = requireNotNull(captured)
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertEquals("https", intent.data?.scheme)
            assertEquals("wa.me", intent.data?.host)
            assertEquals("/966502085865", intent.data?.path)
            assertEquals(setOf("text"), intent.data?.queryParameterNames)
            assertEquals(LicensePurchase.MESSAGE, intent.data?.getQueryParameter("text"))
            assertNull(intent.extras)
            assertEquals(0, licensingActions)
        }
        compose.onNodeWithTag("license-purchase-dialog").assertDoesNotExist()
    }

    @Test fun missingHandlerKeepsPhoneVisibleAndPreservesActivationEntry() {
        var attempts = 0
        var activations = 0
        compose.setContent { TyfinoTheme {
            LicensingScreen(LicensingUiState.ActivationEntry,
                onStartTrial = {}, onShowActivation = {}, onBack = {},
                onActivate = { activations++ }, onRetry = {},
                onOpenPurchase = { attempts++; false })
        } }
        compose.onNodeWithTag("activation-code").performScrollTo().performTextInput("TYFINO-TEST")
        compose.onNodeWithTag("purchase-license").performScrollTo().performClick()
        compose.onNodeWithTag("license-purchase-open").performScrollTo().performClick()
        compose.onNodeWithTag("license-purchase-unavailable").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("license-purchase-phone").performScrollTo().assertTextEquals(LicensePurchase.PHONE)
        compose.onNodeWithTag("license-purchase-back").performScrollTo().performClick()
        compose.onNodeWithTag("activation-code").performScrollTo().assertTextContains("TYFINO-TEST")
        compose.runOnIdle { assertEquals(1, attempts); assertEquals(0, activations) }
    }

    @Test fun purchaseDialogDisposesWhenLicensingBecomesBusyAndReturnsForFailure() {
        var state by mutableStateOf<LicensingUiState>(LicensingUiState.Choice)
        var launches = 0
        compose.setContent { TyfinoTheme {
            LicensingScreen(state, onStartTrial = {}, onShowActivation = {}, onBack = {},
                onActivate = {}, onRetry = {}, onOpenPurchase = { launches++; true })
        } }
        compose.onNodeWithTag("purchase-license").performScrollTo().performClick()
        compose.runOnIdle { state = LicensingUiState.Working }
        compose.onNodeWithTag("license-purchase-dialog").assertDoesNotExist()
        compose.onNodeWithTag("purchase-license").assertDoesNotExist()
        compose.runOnIdle { state = LicensingUiState.Checking }
        compose.onNodeWithTag("purchase-license").assertDoesNotExist()
        compose.runOnIdle { state = LicensingUiState.Failure("ENTITLEMENT_EXPIRED", false) }
        compose.onNodeWithTag("purchase-license").performScrollTo().performClick()
        compose.onNodeWithTag("license-purchase-phone").assertTextEquals(LicensePurchase.PHONE)
        compose.runOnIdle { assertEquals(0, launches) }
    }

    @Test fun contactLauncherHandlesMissingOrDeniedActivitiesWithoutCrashing() {
        for (failure in listOf(ActivityNotFoundException(), SecurityException())) {
            val launcher = object : ContextWrapper(context) {
                override fun startActivity(intent: Intent) { throw failure }
            }
            assertFalse(LicensePurchase.open(launcher))
        }
    }

    @Test fun launcherAndInAppBrandRenderTheirDistinctNativeDrawableTypes() {
        val icon = requireNotNull(context.getDrawable(R.mipmap.ic_launcher_tyfino))
        assertTrue(icon is AdaptiveIconDrawable)
        assertTrue(context.getDrawable(R.drawable.ic_app_tyfino) is VectorDrawable)
        if (Build.VERSION.SDK_INT >= 33) assertNotNull((icon as AdaptiveIconDrawable).monochrome)
        val bitmap = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, 108, 108)
        icon.draw(Canvas(bitmap))
        assertTrue(bitmap.getPixel(54, 54) != 0)
        bitmap.recycle()
    }
}
