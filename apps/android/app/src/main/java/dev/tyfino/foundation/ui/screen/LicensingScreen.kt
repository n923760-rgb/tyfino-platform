package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tyfino.foundation.R
import dev.tyfino.foundation.licensing.LicensingUiState
import dev.tyfino.foundation.licensing.normalizeActivationCode
import dev.tyfino.foundation.ui.components.FocusVisibleButton

@Composable
internal fun LicensingScreen(
    state: LicensingUiState,
    onStartTrial: () -> Unit,
    onShowActivation: () -> Unit,
    onBack: () -> Unit,
    onActivate: (String) -> Unit,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .testTag("licensing-screen"),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 620.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text("TYFINO", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                when (state) {
                    LicensingUiState.Checking -> ProgressContent(R.string.licensing_checking)
                    LicensingUiState.Working -> ProgressContent(R.string.licensing_working)
                    LicensingUiState.Choice -> ChoiceContent(onStartTrial, onShowActivation)
                    LicensingUiState.ActivationEntry -> ActivationContent(onBack, onActivate)
                    is LicensingUiState.Failure -> FailureContent(state, onRetry, onShowActivation)
                    is LicensingUiState.Active -> Unit
                }
            }
        }
    }
}

@Composable
private fun ChoiceContent(onStartTrial: () -> Unit, onShowActivation: () -> Unit) {
    Heading()
    FocusVisibleButton(
        label = stringResource(R.string.start_trial),
        onClick = onStartTrial,
        modifier = Modifier.fillMaxWidth().testTag("start-trial"),
    )
    FocusVisibleButton(
        label = stringResource(R.string.activate_now),
        onClick = onShowActivation,
        modifier = Modifier.fillMaxWidth().testTag("activate-now"),
    )
    PrivacyNote()
}

@Composable
private fun ActivationContent(onBack: () -> Unit, onActivate: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    Heading()
    CompositionLocalProvider(LocalLayoutDirection provides Ltr) {
        OutlinedTextField(
            value = code,
            onValueChange = { value -> code = value.take(64); invalid = false },
            modifier = Modifier.fillMaxWidth().testTag("activation-code"),
            label = { Text(stringResource(R.string.activation_code)) },
            placeholder = { Text(stringResource(R.string.activation_code_hint)) },
            singleLine = true,
            isError = invalid,
            supportingText = if (invalid) {
                { Text(stringResource(R.string.licensing_error_invalid_code)) }
            } else {
                null
            },
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FocusVisibleButton(
            label = stringResource(R.string.back),
            onClick = onBack,
            modifier = Modifier.weight(1f),
        )
        FocusVisibleButton(
            label = stringResource(R.string.activate),
            onClick = {
                val normalized = normalizeActivationCode(code)
                if (normalized == null) invalid = true else {
                    code = ""
                    onActivate(normalized)
                }
            },
            modifier = Modifier.weight(1f).testTag("activate"),
        )
    }
    PrivacyNote()
}

@Composable
private fun FailureContent(
    state: LicensingUiState.Failure,
    onRetry: () -> Unit,
    onShowActivation: () -> Unit,
) {
    Text(stringResource(R.string.licensing_error_title), style = MaterialTheme.typography.headlineMedium)
    Text(errorMessage(state.code), color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (state.retryable) {
        FocusVisibleButton(stringResource(R.string.retry), onRetry, Modifier.fillMaxWidth().testTag("retry-license"))
    }
    FocusVisibleButton(stringResource(R.string.activate_now), onShowActivation, Modifier.fillMaxWidth())
    PrivacyNote()
}

@Composable
private fun ProgressContent(label: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        CircularProgressIndicator()
        Text(stringResource(label), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Heading() {
    Text(stringResource(R.string.licensing_title), style = MaterialTheme.typography.headlineLarge)
    Text(stringResource(R.string.licensing_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun PrivacyNote() {
    Text(
        text = stringResource(R.string.licensing_privacy),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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
