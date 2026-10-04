package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.components.FocusIconButton
import dev.tyfino.foundation.ui.components.ProductHeader
import dev.tyfino.foundation.ui.components.ProductPanel
import dev.tyfino.foundation.ui.components.productTextFieldColors
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
    onBack: (() -> Unit)? = null,
) {
    var host by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember(state) { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing).imePadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .testTag("xtream-gate"),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProductHeader(
                    title = stringResource(R.string.xtream_title),
                    subtitle = stringResource(R.string.xtream_description),
                    eyebrow = stringResource(R.string.product_login_eyebrow),
                )
                ProductPanel(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.product_login_help),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium)
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
                        onBack?.let { back ->
                            FocusVisibleButton(
                                label = stringResource(R.string.back),
                                onClick = back,
                                modifier = Modifier.fillMaxWidth().testTag("xtream-add-back"),
                            )
                        }
                    }
                    is XtreamUiState.ConfirmCleartext, is XtreamUiState.SignedOut -> {
                        val inputStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            textDirection = TextDirection.Ltr,
                        )
                        run {
                            OutlinedTextField(
                                shape = MaterialTheme.shapes.medium,
                                colors = productTextFieldColors(),
                                value = host,
                                onValueChange = {
                                    host = it
                                    if (state is XtreamUiState.ConfirmCleartext) onCancelCleartext()
                                },
                                label = { Text(stringResource(R.string.xtream_host), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                placeholder = { Text(stringResource(R.string.xtream_host_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                textStyle = inputStyle,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("xtream-host"),
                            )
                            OutlinedTextField(
                                shape = MaterialTheme.shapes.medium,
                                colors = productTextFieldColors(),
                                value = username,
                                onValueChange = {
                                    username = it
                                    if (state is XtreamUiState.ConfirmCleartext) onCancelCleartext()
                                },
                                label = { Text(stringResource(R.string.xtream_username), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                textStyle = inputStyle,
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("xtream-username"),
                            )
                            OutlinedTextField(
                                shape = MaterialTheme.shapes.medium,
                                colors = productTextFieldColors(),
                                value = password,
                                onValueChange = {
                                    password = it
                                    if (state is XtreamUiState.ConfirmCleartext) onCancelCleartext()
                                },
                                label = { Text(stringResource(R.string.xtream_password), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                textStyle = inputStyle,
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    FocusIconButton(
                                        icon = if (passwordVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility,
                                        description = stringResource(if (passwordVisible) R.string.product_hide_password else R.string.product_show_password),
                                        onClick = { passwordVisible = !passwordVisible },
                                        modifier = Modifier.testTag("xtream-password-visibility"),
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (state is XtreamUiState.ConfirmCleartext) onConfirmCleartext()
                                    else onSignIn(XtreamInput(host, username, password))
                                }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("xtream-password"),
                            )
                        }
                        (state as? XtreamUiState.SignedOut)?.error?.let { error ->
                            Text(
                                text = stringResource(error.messageResource()),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .semantics { liveRegion = LiveRegionMode.Assertive }
                                    .testTag("xtream-error"),
                            )
                        }
                        if (state is XtreamUiState.ConfirmCleartext) {
                            Text(
                                text = stringResource(R.string.xtream_http_warning_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .semantics { liveRegion = LiveRegionMode.Assertive }
                                    .testTag("xtream-http-warning"),
                            )
                            Text(
                                text = stringResource(R.string.xtream_http_warning, state.providerOrigin),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FocusVisibleButton(
                            label = stringResource(
                                if (state is XtreamUiState.ConfirmCleartext) R.string.xtream_http_continue
                                else R.string.xtream_sign_in,
                            ),
                            onClick = {
                                if (state is XtreamUiState.ConfirmCleartext) onConfirmCleartext()
                                else onSignIn(XtreamInput(host, username, password))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(
                                    if (state is XtreamUiState.ConfirmCleartext) "xtream-confirm-http"
                                    else "xtream-sign-in",
                                ),
                        )
                        onBack?.let { back ->
                            FocusVisibleButton(
                                label = stringResource(R.string.back),
                                onClick = back,
                                modifier = Modifier.fillMaxWidth().testTag("xtream-add-back"),
                            )
                        }
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
    XtreamFailure.AccountLimitReached -> R.string.xtream_error_account_limit
    XtreamFailure.LocalStorage -> R.string.xtream_error_local_storage
}
