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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.ui.components.rememberInitialFocusRequester

@Composable
internal fun FoundationScreen(
    onOpenAccountSwitcher: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val initialFocus = rememberInitialFocusRequester()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .testTag("foundation-screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.foundation_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.foundation_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.home_catalog_hint),
            style = MaterialTheme.typography.titleMedium,
        )
        FocusVisibleButton(
            label = stringResource(R.string.xtream_switch_account),
            onClick = onOpenAccountSwitcher,
            modifier = Modifier.fillMaxWidth().focusRequester(initialFocus).testTag("open-account-switcher"),
        )
        FocusVisibleButton(
            label = stringResource(R.string.open_settings),
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
