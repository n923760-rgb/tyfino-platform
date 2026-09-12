package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.components.FocusVisibleButton

@Composable
internal fun SettingsScreen(onRemoveXtreamAccount: () -> Unit) {
    var confirmRemoval by remember { mutableStateOf(false) }
    val cancelFocus = remember { FocusRequester() }
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
        Text(
            text = stringResource(R.string.xtream_logout_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FocusVisibleButton(
            label = stringResource(R.string.xtream_logout),
            onClick = { confirmRemoval = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("xtream-logout"),
        )
    }
    if (confirmRemoval) {
        AlertDialog(
            onDismissRequest = { confirmRemoval = false },
            title = { Text(stringResource(R.string.xtream_confirm_remove_title)) },
            text = { Text(stringResource(R.string.xtream_confirm_remove_message)) },
            confirmButton = {
                FocusVisibleButton(
                    label = stringResource(R.string.xtream_confirm_remove),
                    onClick = {
                        if (confirmRemoval) {
                            confirmRemoval = false
                            onRemoveXtreamAccount()
                        }
                    },
                    modifier = Modifier.testTag("xtream-confirm-remove"),
                )
            },
            dismissButton = {
                FocusVisibleButton(
                    label = stringResource(R.string.xtream_keep_account),
                    onClick = { confirmRemoval = false },
                    modifier = Modifier.focusRequester(cancelFocus).testTag("xtream-keep-account"),
                )
            },
        )
        LaunchedEffect(Unit) {
            withFrameNanos { }
            cancelFocus.requestFocus()
        }
    }
}
