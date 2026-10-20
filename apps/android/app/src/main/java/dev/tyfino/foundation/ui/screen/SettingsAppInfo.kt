package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
            AppInfoText(stringResource(R.string.settings_app_version, versionName, versionCode),
                "settings-app-version")
            if (development) AppInfoText(stringResource(R.string.settings_app_development),
                "settings-app-development", secondary = true)
        }
    }
}

/** Readable remote-focus stops, not buttons: no click/Enter action or persisted state. */
@Composable
private fun AppInfoText(text: String, tag: String, secondary: Boolean = false) {
    var focused by remember { mutableStateOf(false) }
    Text(text, Modifier.fillMaxWidth().testTag(tag).onFocusChanged { focused = it.isFocused }
        .border(2.dp, if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
            MaterialTheme.shapes.small)
        .focusable().padding(8.dp),
        style = if (secondary) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
        color = if (secondary) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
}
