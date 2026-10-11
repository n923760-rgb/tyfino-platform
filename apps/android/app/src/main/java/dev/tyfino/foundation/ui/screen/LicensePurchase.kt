package dev.tyfino.foundation.ui.screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.ui.components.ProductHeader
import dev.tyfino.foundation.ui.components.rememberInitialFocusRequester

/** An explicit external contact request, never a payment or licensing operation. */
internal object LicensePurchase {
    const val PHONE = "+966 50 432 7750"
    const val MESSAGE = "أرغب بشراء كود تفعيل تطبيق TYFINO"

    fun intent(): Intent = Intent(Intent.ACTION_VIEW,
        "https://wa.me/966504327750".toUri().buildUpon()
            .appendQueryParameter("text", MESSAGE).build())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun open(context: Context): Boolean = try {
        context.startActivity(intent())
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

@Composable
internal fun LicensePurchaseAction(onOpenPurchase: () -> Boolean) {
    var contactVisible by remember { mutableStateOf(false) }
    FocusVisibleButton(stringResource(R.string.license_purchase), { contactVisible = true },
        Modifier.fillMaxWidth().testTag("purchase-license"), prominent = false)
    if (contactVisible) Dialog(onDismissRequest = { contactVisible = false }) {
        var unavailable by remember { mutableStateOf(false) }
        val initialFocus = rememberInitialFocusRequester()
        Surface(Modifier.fillMaxWidth().heightIn(max = 680.dp).testTag("license-purchase-dialog"),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ProductHeader(stringResource(R.string.license_purchase),
                    subtitle = stringResource(R.string.license_purchase_description))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(LicensePurchase.PHONE, style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag("license-purchase-phone"))
                }
                Text(stringResource(R.string.license_purchase_other_device),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (unavailable) Text(stringResource(R.string.license_purchase_unavailable),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
                        .testTag("license-purchase-unavailable"))
                FocusVisibleButton(stringResource(R.string.license_purchase_open), {
                    if (onOpenPurchase()) contactVisible = false else unavailable = true
                }, Modifier.fillMaxWidth().focusRequester(initialFocus).testTag("license-purchase-open"))
                FocusVisibleButton(stringResource(R.string.back), { contactVisible = false },
                    Modifier.fillMaxWidth().testTag("license-purchase-back"), prominent = false)
            }
        }
    }
}
