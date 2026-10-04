package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.dp
import dev.tyfino.foundation.R
import dev.tyfino.foundation.licensing.LicensingUiState
import dev.tyfino.foundation.licensing.normalizeActivationCode
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.ui.components.ProductHeader
import dev.tyfino.foundation.ui.components.ProductPanel
import dev.tyfino.foundation.ui.components.productTextFieldColors
import dev.tyfino.foundation.ui.components.rememberInitialFocusRequester

@Composable
internal fun LicensingScreen(
    state: LicensingUiState,
    onStartTrial: () -> Unit,
    onShowActivation: () -> Unit,
    onBack: () -> Unit,
    onActivate: (String) -> Unit,
    onRetry: () -> Unit,
) {
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()
        .padding(horizontal = 16.dp, vertical = 16.dp).testTag("licensing-screen"),
        contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 680.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            ProductHeader(
                title = stringResource(R.string.licensing_title),
                subtitle = stringResource(R.string.licensing_description),
                eyebrow = stringResource(R.string.product_license_eyebrow))
            when (state) {
                LicensingUiState.Checking -> ProductPanel(Modifier.fillMaxWidth()) {
                    ProgressContent(R.string.licensing_checking)
                }
                LicensingUiState.Working -> ProductPanel(Modifier.fillMaxWidth()) {
                    ProgressContent(R.string.licensing_working)
                }
                LicensingUiState.Choice -> ChoiceContent(onStartTrial, onShowActivation)
                LicensingUiState.ActivationEntry -> ProductPanel(Modifier.fillMaxWidth()) {
                    ActivationContent(onBack, onActivate)
                }
                is LicensingUiState.Failure -> ProductPanel(Modifier.fillMaxWidth()) {
                    FailureContent(state, onRetry, onShowActivation)
                }
                is LicensingUiState.Active -> Unit
            }
            Text(stringResource(R.string.product_license_scope), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("license-device-scope"))
            Text(stringResource(R.string.licensing_privacy), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChoiceContent(onStartTrial: () -> Unit, onShowActivation: () -> Unit) {
    val initialFocus = rememberInitialFocusRequester()
    ProductPanel(Modifier.fillMaxWidth()) {
        ProductHeader(title = stringResource(R.string.product_activation_title), eyebrow = "",
            subtitle = stringResource(R.string.product_activation_description))
        FocusVisibleButton(stringResource(R.string.activate_now), onShowActivation,
            Modifier.fillMaxWidth().focusRequester(initialFocus).testTag("activate-now"))
    }
    ProductPanel(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.product_trial_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.product_trial_description), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        FocusVisibleButton(stringResource(R.string.start_trial), onStartTrial,
            Modifier.fillMaxWidth().testTag("start-trial"), prominent = false)
    }
}

@Composable
private fun ActivationContent(onBack: () -> Unit, onActivate: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    fun submit() {
        val normalized = normalizeActivationCode(code)
        if (normalized == null) invalid = true else {
            code = ""
            onActivate(normalized)
        }
    }
    Text(stringResource(R.string.activation_code), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.product_activation_help), color = MaterialTheme.colorScheme.onSurfaceVariant)
    CompositionLocalProvider(LocalLayoutDirection provides Ltr) {
        OutlinedTextField(
            shape = MaterialTheme.shapes.medium,
            colors = productTextFieldColors(),
            value = code,
            onValueChange = { value -> code = value.take(64); invalid = false },
            modifier = Modifier.fillMaxWidth().testTag("activation-code"),
            label = { Text(stringResource(R.string.activation_code)) },
            placeholder = { Text(stringResource(R.string.activation_code_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            isError = invalid,
            supportingText = if (invalid) {
                {
                    Text(stringResource(R.string.licensing_error_invalid_code),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
                            .testTag("activation-code-error"))
                }
            } else null)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FocusVisibleButton(stringResource(R.string.back), onBack, Modifier.weight(1f), prominent = false)
        FocusVisibleButton(stringResource(R.string.activate), { submit() }, Modifier.weight(1f).testTag("activate"))
    }
}

@Composable
private fun FailureContent(state: LicensingUiState.Failure, onRetry: () -> Unit, onShowActivation: () -> Unit) {
    Text(stringResource(R.string.licensing_error_title), style = MaterialTheme.typography.titleLarge)
    Text(errorMessage(state.code), color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }.testTag("licensing-error"))
    if (state.retryable) FocusVisibleButton(stringResource(R.string.retry), onRetry,
        Modifier.fillMaxWidth().testTag("retry-license"))
    FocusVisibleButton(stringResource(R.string.activate_now), onShowActivation,
        Modifier.fillMaxWidth(), prominent = !state.retryable)
}

@Composable
private fun ProgressContent(label: Int) {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator()
        Text(stringResource(label), color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("licensing-status"))
    }
}

@Composable
private fun errorMessage(code: String): String = stringResource(
    when (code) {
        "TRIAL_ALREADY_USED" -> R.string.licensing_error_trial_used
        "TRIAL_UNAVAILABLE" -> R.string.licensing_error_trial_unavailable
        "DEVICE_LIMIT_REACHED" -> R.string.licensing_error_device_limit
        "ACTIVATION_REJECTED" -> R.string.licensing_error_activation
        "ENTITLEMENT_EXPIRED" -> R.string.licensing_error_expired
        "ENTITLEMENT_REVOKED" -> R.string.licensing_error_revoked
        "SESSION_INVALID" -> R.string.licensing_error_session
        else -> R.string.licensing_error_default
    },
)
