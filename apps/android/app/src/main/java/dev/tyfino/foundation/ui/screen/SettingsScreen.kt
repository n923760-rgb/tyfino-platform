package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.components.ProductHeader
import dev.tyfino.foundation.ui.components.ProductPanel
import dev.tyfino.foundation.ui.components.ProductSectionHeading
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.ui.components.rememberInitialFocusRequester
import dev.tyfino.foundation.xtream.ProviderUserAgent
import dev.tyfino.foundation.xtream.ProviderUserAgentPreset

@Composable
internal fun SettingsScreen(
    onOpenAccountSwitcher: () -> Unit,
    onManageAccounts: () -> Unit,
    licenseSummary: LicenseSummary? = null,
) {
    val initialFocus = rememberInitialFocusRequester()
    val context = LocalContext.current
    val userAgent = remember(context) { ProviderUserAgent(context) }
    var preset by remember(userAgent) { mutableStateOf(userAgent.selected()) }
    BoxWithConstraints(Modifier.fillMaxSize().testTag("settings-screen")) {
        val horizontalPadding = if (maxWidth < 600.dp) 16.dp else 24.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                ProductHeader(
                    title = stringResource(R.string.settings_title),
                    subtitle = stringResource(R.string.product_settings_description),
                )
                SettingsSection(
                    title = stringResource(R.string.settings_accounts_title),
                    description = stringResource(R.string.xtream_manage_accounts_description),
                    tag = "settings-accounts",
                ) {
                    FocusVisibleButton(
                        label = stringResource(R.string.xtream_switch_account),
                        onClick = onOpenAccountSwitcher,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(initialFocus)
                            .testTag("open-account-switcher"),
                    )
                    FocusVisibleButton(
                        label = stringResource(R.string.xtream_manage_accounts),
                        onClick = onManageAccounts,
                        prominent = false,
                        modifier = Modifier.fillMaxWidth().testTag("open-account-manager"),
                    )
                    Text(
                        text = stringResource(R.string.xtream_logout_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                licenseSummary?.let { LicenseSummaryPanel(it) }
                SettingsSection(
                    title = stringResource(R.string.settings_user_agent_title),
                    description = stringResource(R.string.settings_user_agent_description),
                    tag = "settings-provider",
                ) {
                    CatalogFilterButton(
                        label = stringResource(R.string.settings_user_agent_tyfino),
                        selected = preset == ProviderUserAgentPreset.Tyfino,
                        onClick = { userAgent.select(ProviderUserAgentPreset.Tyfino); preset = ProviderUserAgentPreset.Tyfino },
                        modifier = Modifier.fillMaxWidth().testTag("settings-user-agent-tyfino"),
                    )
                    CatalogFilterButton(
                        label = stringResource(R.string.settings_user_agent_vlc),
                        selected = preset == ProviderUserAgentPreset.Vlc,
                        onClick = { userAgent.select(ProviderUserAgentPreset.Vlc); preset = ProviderUserAgentPreset.Vlc },
                        modifier = Modifier.fillMaxWidth().testTag("settings-user-agent-vlc"),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    description: String,
    tag: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    ProductPanel(Modifier.fillMaxWidth().testTag("$tag-section")) {
        Column(Modifier.focusGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProductSectionHeading(title, Modifier.testTag("$tag-heading"))
            Text(description, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}
