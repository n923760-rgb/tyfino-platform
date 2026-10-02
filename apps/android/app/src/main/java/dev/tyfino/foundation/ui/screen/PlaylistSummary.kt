package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.ui.components.ProductPanel
import dev.tyfino.foundation.ui.components.ProductSectionHeading
import dev.tyfino.foundation.xtream.PlaylistRepository
import dev.tyfino.foundation.xtream.PlaylistState
import dev.tyfino.foundation.xtream.PlaylistStatus
import dev.tyfino.foundation.xtream.XtreamFailure
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
internal fun PlaylistSummary(repository: PlaylistRepository) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    var state by remember(repository) { mutableStateOf<PlaylistState>(PlaylistState.Loading) }
    var revision by remember(repository) { mutableStateOf(0) }
    DisposableEffect(repository, lifecycle, revision) {
        var generation = 0
        var job: Job? = null
        fun refresh() {
            val request = ++generation
            job?.cancel()
            state = PlaylistState.Loading
            job = scope.launch {
                val next = repository.read()
                if (generation == request && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) state = next
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
            if (event == Lifecycle.Event.ON_PAUSE) { generation++; job?.cancel(); state = PlaylistState.Loading }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) refresh()
        onDispose { generation++; job?.cancel(); lifecycle.removeObserver(observer) }
    }
    PlaylistSummaryPanel(state, onRefresh = { revision++ })
}

@Composable
internal fun PlaylistSummaryPanel(state: PlaylistState, onRefresh: () -> Unit) {
    val locale = Locale.forLanguageTag(LocalLocale.current.toLanguageTag())
    ProductPanel(Modifier.fillMaxWidth().testTag("playlist-summary")) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ProductSectionHeading(stringResource(R.string.playlist_title))
            Text(stringResource(R.string.playlist_boundary), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            when (state) {
                PlaylistState.Loading -> Text(stringResource(R.string.playlist_loading))
                PlaylistState.NoAccount, PlaylistState.Stale -> Text(stringResource(R.string.playlist_no_account))
                is PlaylistState.Failure -> Text(stringResource(when (state.reason) {
                    XtreamFailure.NetworkUnavailable -> R.string.xtream_error_network
                    XtreamFailure.Timeout -> R.string.xtream_error_timeout
                    XtreamFailure.InvalidCredentials -> R.string.xtream_error_credentials
                    XtreamFailure.InvalidHost -> R.string.xtream_error_invalid_host
                    XtreamFailure.MalformedResponse -> R.string.xtream_error_malformed
                    XtreamFailure.UnsupportedResponse -> R.string.xtream_error_unsupported
                    else -> R.string.playlist_unavailable
                }))
                is PlaylistState.Ready -> {
                    Text(stringResource(R.string.playlist_connection), style = MaterialTheme.typography.labelLarge)
                    Text(state.providerOrigin, modifier = Modifier.testTag("playlist-provider"))
                    Text(stringResource(R.string.playlist_username, state.username), modifier = Modifier.testTag("playlist-username"))
                    Text(stringResource(when (state.info.status) {
                        PlaylistStatus.Active -> R.string.playlist_active
                        PlaylistStatus.Expired -> R.string.playlist_expired
                        PlaylistStatus.Disabled -> R.string.playlist_disabled
                    }), modifier = Modifier.testTag("playlist-status"))
                    val expiry = remember(state.info.expiresAtMillis, locale) {
                        state.info.expiresAtMillis?.let {
                            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(it))
                        }
                    } ?: stringResource(R.string.playlist_not_provided)
                    Text(stringResource(R.string.playlist_expiry, expiry), modifier = Modifier.testTag("playlist-expiry"))
                    state.info.trial?.let { Text(stringResource(if (it) R.string.playlist_trial else R.string.playlist_paid)) }
                    state.info.maxConnections?.let { maximum ->
                        Text(stringResource(R.string.playlist_connections,
                            state.info.activeConnections?.toString() ?: stringResource(R.string.playlist_not_provided), maximum))
                    }
                    Text(stringResource(R.string.playlist_provider_report), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (state != PlaylistState.Loading) FocusVisibleButton(
                stringResource(R.string.playlist_refresh), onRefresh, prominent = false,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).testTag("playlist-refresh"),
            )
        }
    }
}
