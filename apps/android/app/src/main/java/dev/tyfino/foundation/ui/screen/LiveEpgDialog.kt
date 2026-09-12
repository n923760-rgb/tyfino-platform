package dev.tyfino.foundation.ui.screen

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
                                Text(stringResource(R.string.epg_loading))
                            }
                        }
                        is LiveEpgState.Error -> item {
                            Text(stringResource(R.string.epg_error), modifier = Modifier.testTag("epg-error"))
                        }
                        is LiveEpgState.Empty -> item {
                            Text(stringResource(R.string.epg_empty), modifier = Modifier.testTag("epg-empty"))
                        }
                        is LiveEpgState.Stale -> item {
                            Text(stringResource(R.string.epg_stale), modifier = Modifier.testTag("epg-stale"))
                        }
                        is LiveEpgState.Content -> if (state.refreshing) item {
                            Text(stringResource(R.string.epg_loading))
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
                        itemsIndexed(programs) { _, program ->
                            EpgProgramRow(program, timeFormat)
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
private fun EpgProgramRow(program: LiveEpgProgram, timeFormat: DateFormat) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "${timeFormat.format(Date(program.startEpochMillis))} – ${timeFormat.format(Date(program.endEpochMillis))}",
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            program.title?.takeIf(String::isNotBlank) ?: stringResource(R.string.epg_untitled),
            style = MaterialTheme.typography.bodyLarge,
        )
        program.description?.takeIf(String::isNotBlank)?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
