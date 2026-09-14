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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.xtream.XtreamAccountSummary
import dev.tyfino.foundation.xtream.XtreamAccountsSnapshot

@Composable
internal fun XtreamAccountManager(
    snapshot: XtreamAccountsSnapshot?,
    removingAccountId: String?,
    storageError: Boolean,
    onRemoveAccount: (String) -> Unit,
    onBack: () -> Unit,
) {
    var confirmation by remember { mutableStateOf<XtreamAccountSummary?>(null) }
    val busy = removingAccountId != null
    val backFocus = remember { FocusRequester() }
    val confirmationFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { backFocus.requestAfterManagerDialogFrames() }

    Dialog(onDismissRequest = { if (!busy) onBack() }) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 720.dp).testTag("xtream-account-manager"),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.xtream_manage_accounts), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.xtream_manage_accounts_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (snapshot == null && !storageError) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally).testTag("xtream-accounts-loading"),
                    )
                }
                snapshot?.accounts?.forEach { account ->
                    val active = account.accountId == snapshot.activeAccountId
                    Text(
                        text = stringResource(
                            if (active) R.string.xtream_account_active else R.string.xtream_account_inactive,
                            account.username,
                            account.providerOrigin,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    FocusVisibleButton(
                        label = stringResource(R.string.xtream_remove_named_account, account.username),
                        onClick = { confirmation = account },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().testTag("xtream-remove-${account.accountId}"),
                    )
                }
                if (busy) {
                    Text(
                        stringResource(R.string.xtream_removing_account),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                            .testTag("xtream-removing-account"),
                    )
                }
                if (storageError) {
                    Text(
                        stringResource(R.string.xtream_remove_error_local_storage),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
                            .testTag("xtream-remove-error"),
                    )
                }
                FocusVisibleButton(
                    label = stringResource(R.string.back),
                    onClick = onBack,
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(backFocus)
                        .testTag("xtream-manage-back"),
                )
            }
        }
    }
    confirmation?.let { account ->
        LaunchedEffect(account.accountId) { confirmationFocus.requestAfterManagerDialogFrames() }
        Dialog(onDismissRequest = { if (!busy) confirmation = null }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 12.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(stringResource(R.string.xtream_confirm_remove_title), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.xtream_confirm_remove_named_message, account.username, account.providerOrigin))
                    FocusVisibleButton(
                        label = stringResource(R.string.xtream_keep_account),
                        onClick = { confirmation = null },
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(confirmationFocus)
                            .testTag("xtream-keep-account"),
                    )
                    FocusVisibleButton(
                        label = stringResource(R.string.xtream_confirm_remove),
                        onClick = {
                            confirmation = null
                            onRemoveAccount(account.accountId)
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().testTag("xtream-confirm-remove"),
                    )
                }
            }
        }
    }
}

private suspend fun FocusRequester.requestAfterManagerDialogFrames() {
    repeat(2) {
        withFrameNanos { }
        requestFocus()
    }
}
