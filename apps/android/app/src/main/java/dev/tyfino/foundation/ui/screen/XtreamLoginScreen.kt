package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.xtream.XtreamFailure
import dev.tyfino.foundation.xtream.XtreamInput
import dev.tyfino.foundation.xtream.XtreamUiState

@Composable
internal fun XtreamLoginScreen(
    state: XtreamUiState,
    onSignIn: (XtreamInput) -> Unit,
    onConfirmCleartext: () -> Unit,
    onCancelCleartext: () -> Unit,
) {
    var host by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .testTag("xtream-gate"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.xtream_title),
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = stringResource(R.string.xtream_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when (state) {
                XtreamUiState.Loading, XtreamUiState.Working -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .testTag("xtream-progress"),
                    )
                    Text(
                        text = stringResource(
                            if (state is XtreamUiState.Loading) {
                                R.string.xtream_loading
                            } else {
                                R.string.xtream_connecting
                            },
                        ),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .semantics { liveRegion = LiveRegionMode.Polite }
                            .testTag("xtream-status"),
                    )
                }
                is XtreamUiState.ConfirmCleartext -> {
                    Text(
                        text = stringResource(R.string.xtream_http_warning_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .semantics { liveRegion = LiveRegionMode.Assertive }
                            .testTag("xtream-http-warning"),
                    )
                    Text(
                        text = stringResource(
                            R.string.xtream_http_warning,
                            state.providerOrigin,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    FocusVisibleButton(
                        label = stringResource(R.string.xtream_http_continue),
                        onClick = onConfirmCleartext,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("xtream-confirm-http"),
                    )
                    FocusVisibleButton(
                        label = stringResource(R.string.back),
                        onClick = onCancelCleartext,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                is XtreamUiState.SignedOut -> {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text(stringResource(R.string.xtream_host)) },
                            placeholder = { Text(stringResource(R.string.xtream_host_hint)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("xtream-host"),
                        )
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text(stringResource(R.string.xtream_username)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("xtream-username"),
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.xtream_password)) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("xtream-password"),
                        )
                    }
                    state.error?.let { error ->
                        Text(
                            text = stringResource(error.messageResource()),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .semantics { liveRegion = LiveRegionMode.Assertive }
                                .testTag("xtream-error"),
                        )
                    }
                    FocusVisibleButton(
                        label = stringResource(R.string.xtream_sign_in),
                        onClick = { onSignIn(XtreamInput(host, username, password)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("xtream-sign-in"),
                    )
                    Text(
                        text = stringResource(R.string.xtream_privacy),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is XtreamUiState.SignedIn -> Unit
            }
        }
    }
}

private fun XtreamFailure.messageResource(): Int = when (this) {
    XtreamFailure.InvalidHost -> R.string.xtream_error_invalid_host
    XtreamFailure.NetworkUnavailable -> R.string.xtream_error_network
    XtreamFailure.Timeout -> R.string.xtream_error_timeout
    XtreamFailure.ProviderUnavailable -> R.string.xtream_error_provider
    XtreamFailure.InvalidCredentials -> R.string.xtream_error_credentials
    XtreamFailure.AccountExpired -> R.string.xtream_error_expired
    XtreamFailure.AccountDisabled -> R.string.xtream_error_disabled
    XtreamFailure.MalformedResponse -> R.string.xtream_error_malformed
    XtreamFailure.UnsupportedResponse -> R.string.xtream_error_unsupported
}
