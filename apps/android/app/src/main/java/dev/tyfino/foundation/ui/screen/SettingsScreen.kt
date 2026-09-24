package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.ui.components.rememberInitialFocusRequester
import dev.tyfino.foundation.xtream.ProviderUserAgent
import dev.tyfino.foundation.xtream.ProviderUserAgentPreset

@Composable
internal fun SettingsScreen(
    onOpenAccountSwitcher: () -> Unit,
    onManageAccounts: () -> Unit,
) {
    val initialFocus = rememberInitialFocusRequester()
    val context = LocalContext.current
    val userAgent = remember(context) { ProviderUserAgent(context) }
    var preset by remember(userAgent) { mutableStateOf(userAgent.selected()) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .testTag("settings-screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.settings_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(stringResource(R.string.settings_user_agent_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.settings_user_agent_description), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Text(
            text = stringResource(R.string.xtream_logout_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            modifier = Modifier
                .fillMaxWidth()
                .testTag("open-account-manager"),
        )
    }
}
