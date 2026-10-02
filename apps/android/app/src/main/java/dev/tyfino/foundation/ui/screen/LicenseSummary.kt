package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.tyfino.foundation.R
import dev.tyfino.foundation.licensing.EntitlementKind
import dev.tyfino.foundation.ui.components.ProductPanel
import dev.tyfino.foundation.ui.components.ProductSectionHeading
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Display metadata from the current Active state; no token or installation identifier. */
internal data class LicenseSummary(
    val kind: EntitlementKind,
    val expiresAtMillis: Long?,
    val offline: Boolean,
)

@Composable
internal fun LicenseSummaryPanel(summary: LicenseSummary) {
    val locale = Locale.forLanguageTag(LocalLocale.current.toLanguageTag())
    val expiry = remember(summary.expiresAtMillis, locale) {
        summary.expiresAtMillis?.let { DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(it)) }
    }
    ProductPanel(Modifier.fillMaxWidth().testTag("license-summary")) {
        ProductSectionHeading(stringResource(R.string.product_license_title))
        Text(stringResource(R.string.product_license_plan), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(when (summary.kind) {
            EntitlementKind.Trial -> R.string.product_license_trial
            EntitlementKind.OneYear -> R.string.product_license_year
            EntitlementKind.Lifetime -> R.string.product_license_lifetime
        }), style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("license-summary-plan"))
        expiry?.let { Text(stringResource(R.string.product_license_expires, it),
            modifier = Modifier.testTag("license-summary-expiry")) }
        Text(stringResource(if (summary.offline) R.string.product_license_offline else R.string.product_license_active),
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("license-summary-status"))
        Text(stringResource(R.string.product_license_scope), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.product_license_transfer), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
