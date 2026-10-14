package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.tyfino.foundation.BuildConfig
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.components.ProductPanel
import dev.tyfino.foundation.ui.components.ProductSectionHeading

/** Read-only build constants, never account, installation, entitlement or signing data. */
@Composable
internal fun SettingsAppInfo(
    versionName: String = BuildConfig.VERSION_NAME,
    versionCode: Int = BuildConfig.VERSION_CODE,
    development: Boolean = BuildConfig.DEBUG,
) {
    ProductPanel(Modifier.fillMaxWidth().testTag("settings-app-info")) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProductSectionHeading(stringResource(R.string.settings_app_info),
                Modifier.testTag("settings-app-info-heading"))
            Text(stringResource(R.string.settings_app_version, versionName, versionCode),
                Modifier.testTag("settings-app-version"), style = MaterialTheme.typography.bodyLarge)
            if (development) Text(stringResource(R.string.settings_app_development),
                Modifier.testTag("settings-app-development"), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
