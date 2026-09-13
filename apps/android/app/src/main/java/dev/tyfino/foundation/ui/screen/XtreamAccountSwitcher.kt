package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.xtream.XtreamAccountsSnapshot

@Composable
internal fun XtreamAccountSwitcher(
    snapshot: XtreamAccountsSnapshot?,
    switchingAccountId: String?,
    storageError: Boolean,
    onSelectAccount: (String) -> Unit,
    onAddAccount: () -> Unit,
    onManageAccounts: () -> Unit,
    onDismiss: () -> Unit,
) {
    val busy = switchingAccountId != null
    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .testTag("xtream-account-switcher"),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.xtream_accounts_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.xtream_accounts_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (snapshot == null && !storageError) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .testTag("xtream-accounts-loading"),
                    )
                } else {
                    snapshot.accounts.forEach { account ->
                        val active = account.accountId == snapshot.activeAccountId
                        val label = stringResource(
                            if (active) R.string.xtream_account_active else R.string.xtream_account_inactive,
                            account.username,
                            account.providerOrigin,
                        )
                        FocusVisibleButton(
                            label = label,
                            onClick = { onSelectAccount(account.accountId) },
                            enabled = !busy && !active,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { selected = active }
                                .testTag("xtream-account-${account.accountId}"),
                        )
                    }
                }
                if (busy) {
                    Text(
                        text = stringResource(R.string.xtream_switching_account),
                        modifier = Modifier
                            .semantics { liveRegion = LiveRegionMode.Polite }
                            .testTag("xtream-switching-account"),
                    )
                }
                if (storageError) {
                    Text(
                        text = stringResource(R.string.xtream_switch_error_local_storage),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .semantics { liveRegion = LiveRegionMode.Assertive }
                            .testTag("xtream-switch-error"),
                    )
                }
                FocusVisibleButton(
                    label = stringResource(R.string.xtream_add_account),
                    onClick = onAddAccount,
                    enabled = !busy && snapshot != null,
                    modifier = Modifier.fillMaxWidth().testTag("xtream-add-account"),
                )
                FocusVisibleButton(
                    label = stringResource(R.string.xtream_manage_accounts),
                    onClick = onManageAccounts,
                    enabled = !busy && snapshot != null,
                    modifier = Modifier.fillMaxWidth().testTag("xtream-manage-accounts"),
                )
                FocusVisibleButton(
                    label = stringResource(R.string.playback_close),
                    onClick = onDismiss,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().testTag("xtream-close-switcher"),
                )
            }
        }
    }
}
