package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.xtream.LiveEpgProgram
import dev.tyfino.foundation.xtream.LiveEpgState
import java.text.DateFormat
import java.util.Date

@Composable
internal fun LiveEpgDialog(
    state: LiveEpgState,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    val locale = LocalConfiguration.current.locales[0]
    val timeFormat = remember(locale) { DateFormat.getTimeInstance(DateFormat.SHORT, locale) }
    val now = System.currentTimeMillis()
    val programs = when (state) {
        is LiveEpgState.Content -> state.programs
        is LiveEpgState.Stale -> state.programs
        else -> emptyList()
    }
    val current = programs.firstOrNull { it.startEpochMillis <= now && it.endEpochMillis > now }
    val next = programs.firstOrNull { it.startEpochMillis > now }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 620.dp).testTag("live-epg-dialog"),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.epg_title), style = MaterialTheme.typography.headlineSmall)
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (state) {
                        LiveEpgState.Loading -> item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator()
                                EpgLoadingStatus()
                            }
                        }
                        is LiveEpgState.Error -> item {
                            Text(
                                text = stringResource(R.string.epg_error),
                                modifier = Modifier
                                    .semantics { liveRegion = LiveRegionMode.Assertive }
                                    .testTag("epg-error"),
                            )
                        }
                        is LiveEpgState.Empty -> item {
                            Text(
                                text = stringResource(R.string.epg_empty),
                                modifier = Modifier
                                    .semantics { liveRegion = LiveRegionMode.Polite }
                                    .testTag("epg-empty"),
                            )
                        }
                        is LiveEpgState.Stale -> item {
                            Text(
                                text = stringResource(R.string.epg_stale),
                                modifier = Modifier
                                    .semantics { liveRegion = LiveRegionMode.Assertive }
                                    .testTag("epg-stale"),
                            )
                        }
                        is LiveEpgState.Content -> if (state.refreshing) item {
                            EpgLoadingStatus()
                        }
                    }
                    if (programs.isNotEmpty()) {
                        item {
                            Text(stringResource(R.string.epg_current), style = MaterialTheme.typography.titleMedium)
                            Text(current?.title?.takeIf(String::isNotBlank) ?: stringResource(R.string.epg_no_current))
                        }
                        item {
                            Text(stringResource(R.string.epg_next), style = MaterialTheme.typography.titleMedium)
                            Text(next?.title?.takeIf(String::isNotBlank) ?: stringResource(R.string.epg_no_next))
                        }
                        item { Text(stringResource(R.string.epg_schedule), style = MaterialTheme.typography.titleMedium) }
                        itemsIndexed(programs) { index, program ->
                            EpgProgramRow(index, program, timeFormat)
                        }
                    } else if (state is LiveEpgState.Stale) {
                        item { Text(stringResource(R.string.epg_empty)) }
                    }
                }
                FocusVisibleButton(
                    label = stringResource(R.string.epg_refresh),
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth().focusRequester(firstFocus).testTag("epg-refresh"),
                )
                FocusVisibleButton(
                    label = stringResource(R.string.playback_close),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    LaunchedEffect(Unit) { firstFocus.requestFocus() }
}

@Composable
private fun EpgLoadingStatus() {
    Text(
        text = stringResource(R.string.epg_loading),
        modifier = Modifier
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag("epg-loading"),
    )
}

@Composable
internal fun EpgProgramRow(index: Int, program: LiveEpgProgram, timeFormat: DateFormat) {
    var isFocused by remember { mutableStateOf(false) }
    val timeLabel = "${timeFormat.format(Date(program.startEpochMillis))} – ${timeFormat.format(Date(program.endEpochMillis))}"
    val title = program.title?.takeIf(String::isNotBlank) ?: stringResource(R.string.epg_untitled)
    val description = program.description?.takeIf(String::isNotBlank)
    val accessibilityLabel = listOfNotNull(timeLabel, title, description).joinToString(", ")
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .semantics { contentDescription = accessibilityLabel }
            .testTag("epg-program-$index"),
        shape = MaterialTheme.shapes.medium,
        color = if (isFocused) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            if (isFocused) 3.dp else 1.dp,
            if (isFocused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                timeLabel,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
            )
            description?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
