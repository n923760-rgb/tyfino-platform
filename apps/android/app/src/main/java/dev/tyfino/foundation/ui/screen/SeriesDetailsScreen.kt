package dev.tyfino.foundation.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.xtream.CatalogItem
import dev.tyfino.foundation.xtream.SeriesDetailsRepository
import dev.tyfino.foundation.xtream.SeriesDestination
import dev.tyfino.foundation.xtream.SeriesEpisode
import dev.tyfino.foundation.xtream.SeriesFailure
import dev.tyfino.foundation.xtream.SeriesSeason
import dev.tyfino.foundation.xtream.SeriesState
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class SeriesSelection(
    val accountId: String,
    val accountGeneration: Long,
    val item: CatalogItem,
)

@Composable
internal fun SeriesDetailsScreen(
    selection: SeriesSelection,
    repository: SeriesDetailsRepository,
    onBack: () -> Unit,
    onEpisode: (SeriesEpisode, Long) -> Unit,
) {
    var state by remember(selection) { mutableStateOf<SeriesState>(SeriesState.Empty) }
    var destination by remember(selection) { mutableStateOf<SeriesDestination?>(null) }
    var ownerChanged by remember(selection) { mutableStateOf(false) }
    var preferredSeason by rememberSaveable(selection.item.providerId) { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository, selection) {
        state = SeriesState.Loading
        val opened = repository.open(selection.item.providerId)
        if (opened == null || opened.accountId != selection.accountId ||
            opened.accountGeneration != selection.accountGeneration
        ) {
            if (opened != null) repository.close(opened)
            ownerChanged = true
            return@LaunchedEffect
        }
        destination = opened
        try {
            repository.details(opened) { next -> if (destination == opened) state = next }
            awaitCancellation()
        } finally {
            withContext(NonCancellable) { repository.close(opened) }
        }
    }

    SeriesDetailsContent(
        selection = selection,
        state = state,
        ownerChanged = ownerChanged,
        preferredSeason = preferredSeason,
        onSeasonSelected = { preferredSeason = it },
        onBack = onBack,
        onEpisode = onEpisode,
        onRefresh = {
            val current = destination
            if (current != null) {
                scope.launch {
                    repository.details(current, forceRefresh = true) { next ->
                        if (destination == current) state = next
                    }
                }
            }
        },
    )
}

@Composable
internal fun SeriesDetailsContent(
    selection: SeriesSelection,
    state: SeriesState,
    ownerChanged: Boolean,
    preferredSeason: Int?,
    onSeasonSelected: (Int) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onEpisode: (SeriesEpisode, Long) -> Unit = { _, _ -> },
) {
    val details = when (state) {
        is SeriesState.Content -> state.details
        is SeriesState.EmptyContent -> state.details
        is SeriesState.StaleContent -> state.details
        else -> null
    }
    val selectedSeason = details?.let { SeriesPresentation.selectedSeason(it, preferredSeason) }
    val visibleEpisodes = remember(details, selectedSeason) {
        details?.let { SeriesPresentation.episodesForSeason(it, selectedSeason) }.orEmpty()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("series-details"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "actions") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FocusVisibleButton(label = stringResource(R.string.back), onClick = onBack)
                if (details != null || state is SeriesState.Error) {
                    FocusVisibleButton(label = stringResource(R.string.series_refresh), onClick = onRefresh)
                }
            }
        }
        item(key = "summary") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = details?.let { SeriesPresentation.displayName(it, selection.item.name) }
                        ?: selection.item.name,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 2,
                )
                val overview = details?.summary?.plot
                if (!overview.isNullOrBlank()) {
                    Text(overview, style = MaterialTheme.typography.bodyMedium, maxLines = 4)
                }
                val supporting = listOfNotNull(
                    details?.summary?.releaseDate ?: selection.item.releaseYear,
                    details?.summary?.rating ?: selection.item.rating,
                ).joinToString(" • ")
                if (supporting.isNotBlank()) {
                    Text(supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        when {
            ownerChanged -> item(key = "account-changed") {
                StatusText(R.string.series_account_changed, error = true)
            }
            state == SeriesState.Empty || state == SeriesState.Loading -> item(key = "loading") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                    StatusText(R.string.series_loading)
                }
            }
            state is SeriesState.Error -> item(key = "error") {
                StatusText(state.failure.messageResource(), error = true)
            }
            details != null -> {
                if (state is SeriesState.StaleContent) item(key = "stale") {
                    Text(
                        text = stringResource(R.string.series_stale, stringResource(state.failure.messageResource())),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (state is SeriesState.Content && state.isRefreshing ||
                    state is SeriesState.EmptyContent && state.isRefreshing
                ) item(key = "refreshing") { StatusText(R.string.series_refreshing) }
                if (details.episodes.isEmpty()) item(key = "empty") {
                    StatusText(R.string.series_no_episodes)
                } else {
                    item(key = "season-heading") {
                        Text(stringResource(R.string.series_seasons), style = MaterialTheme.typography.titleLarge)
                    }
                    item(key = "season-selector") {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().testTag("series-seasons"),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(details.seasons, key = SeriesSeason::seasonNumber) { season ->
                                SeasonChoice(
                                    season = season,
                                    selected = selectedSeason == season.seasonNumber,
                                    onClick = { onSeasonSelected(season.seasonNumber) },
                                )
                            }
                        }
                    }
                    if (visibleEpisodes.isEmpty()) item(key = "season-empty") {
                        StatusText(R.string.series_no_episodes)
                    }
                    items(
                        items = visibleEpisodes,
                        key = { "${it.seasonNumber}:${it.providerEpisodeId}" },
                    ) { episode ->
                        val generation = when (state) {
                            is SeriesState.Content -> state.generation
                            is SeriesState.StaleContent -> state.generation
                            else -> 0L
                        }
                        EpisodeRow(episode, generation > 0L) { onEpisode(episode, generation) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonChoice(season: SeriesSeason, selected: Boolean, onClick: () -> Unit) {
    val label = SeriesPresentation.acceptedSeasonLabel(season)
        ?: stringResource(R.string.series_season_number, season.seasonNumber)
    var focused by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = Modifier
            .widthIn(min = 108.dp)
            .heightIn(min = 56.dp)
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = label },
        border = BorderStroke(
            if (focused) 3.dp else 1.dp,
            if (focused) MaterialTheme.colorScheme.onSurface
            else if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Text(label, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun EpisodeRow(episode: SeriesEpisode, hasPublishedGeneration: Boolean, onClick: () -> Unit) {
    val label = episode.title?.takeIf(String::isNotBlank)
        ?: episode.episodeNumber?.let { stringResource(R.string.series_episode_number, it) }
        ?: stringResource(R.string.series_episode_order, episode.providerOrder + 1)
    val seasonLabel = stringResource(R.string.series_season_number, episode.seasonNumber)
    val numberLabel = episode.episodeNumber?.let { stringResource(R.string.series_episode_number, it) }
    val playable = hasPublishedGeneration && SeriesPresentation.playableMetadataAvailable(episode)
    val reason = stringResource(if (playable) R.string.series_play_episode else R.string.series_playback_unavailable)
    val accessibilityLabel = listOfNotNull(label, seasonLabel, numberLabel, reason).distinct().joinToString(", ")
    var focused by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        enabled = playable,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp)
            .heightIn(min = 88.dp)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .semantics {
                contentDescription = accessibilityLabel
                if (!playable) disabled()
            },
        border = BorderStroke(
            if (focused) 3.dp else 1.dp,
            if (focused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 2)
            if (numberLabel != null && numberLabel != label) {
                Text(numberLabel, style = MaterialTheme.typography.bodySmall)
            }
            Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusText(@StringRes message: Int, error: Boolean = false) {
    Text(
        text = stringResource(message),
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@StringRes
private fun SeriesFailure.messageResource(): Int = when (this) {
    SeriesFailure.NetworkUnavailable -> R.string.catalog_error_network
    SeriesFailure.Timeout -> R.string.catalog_error_timeout
    SeriesFailure.ProviderUnavailable -> R.string.catalog_error_provider
    SeriesFailure.AuthenticationRejected -> R.string.catalog_error_authentication
    SeriesFailure.MalformedResponse -> R.string.series_error_malformed
    SeriesFailure.UnsupportedResponse -> R.string.series_error_unsupported
    SeriesFailure.ResponseTooLarge -> R.string.catalog_error_too_large
    SeriesFailure.LocalStorage -> R.string.catalog_error_storage
    SeriesFailure.Unknown -> R.string.series_error_unknown
}
