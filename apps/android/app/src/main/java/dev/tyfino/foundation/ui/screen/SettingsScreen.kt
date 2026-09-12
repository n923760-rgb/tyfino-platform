package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.components.FocusVisibleButton

@Composable
internal fun SettingsScreen(onRemoveXtreamAccount: () -> Unit) {
    var confirmRemoval by remember { mutableStateOf(false) }
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
        val keepAccountFocus = remember { FocusRequester() }
        Dialog(onDismissRequest = { confirmRemoval = false }) {
            val dialogView = LocalView.current
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        stringResource(R.string.xtream_confirm_remove_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(stringResource(R.string.xtream_confirm_remove_message))
                    FocusVisibleButton(
                        label = stringResource(R.string.xtream_keep_account),
                        onClick = { confirmRemoval = false },
                        modifier = Modifier
                            .focusRequester(keepAccountFocus)
                            .fillMaxWidth()
                            .testTag("xtream-keep-account"),
                    )
                    FocusVisibleButton(
                        label = stringResource(R.string.xtream_confirm_remove),
                        onClick = {
                            if (confirmRemoval) {
                                confirmRemoval = false
                                onRemoveXtreamAccount()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("xtream-confirm-remove"),
                    )
                }
            }
            LaunchedEffect(dialogView, keepAccountFocus) {
                while (!dialogView.hasWindowFocus()) {
                    withFrameNanos { }
                }
                withFrameNanos { }
                keepAccountFocus.requestFocus()
            }
        }
    }
}
