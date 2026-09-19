package dev.tyfino.foundation.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.tyfino.foundation.R
import dev.tyfino.foundation.playback.MovieResumeLoadResult
import dev.tyfino.foundation.playback.MovieResumeRepository
import dev.tyfino.foundation.playback.PlaybackSelection
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.ui.components.rememberInitialFocusRequester
import dev.tyfino.foundation.xtream.CatalogItem
import dev.tyfino.foundation.xtream.CatalogSection
import dev.tyfino.foundation.xtream.MovieDetails
import dev.tyfino.foundation.xtream.MovieDetailsDestination
import dev.tyfino.foundation.xtream.MovieDetailsFailure
import dev.tyfino.foundation.xtream.MovieDetailsRepository
import dev.tyfino.foundation.xtream.MovieDetailsState
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class MovieSelection(val accountId: String, val accountGeneration: Long, val item: CatalogItem)

@Composable
internal fun MovieDetailsScreen(
    selection: MovieSelection,
    repository: MovieDetailsRepository,
    resumeRepository: MovieResumeRepository,
    onBack: () -> Unit,
    onPlay: (CatalogItem) -> Unit,
) {
    var state by remember(selection) { mutableStateOf<MovieDetailsState>(MovieDetailsState.Empty) }
    var destination by remember(selection) { mutableStateOf<MovieDetailsDestination?>(null) }
    var ownerChanged by remember(selection) { mutableStateOf(false) }
    var canResume by remember(selection) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository, selection) {
        state = MovieDetailsState.Loading
        val opened = repository.open(selection.item.providerId)
        if (opened == null || opened.accountId != selection.accountId || opened.accountGeneration != selection.accountGeneration) {
            if (opened != null) repository.close(opened)
            ownerChanged = true
            return@LaunchedEffect
        }
        destination = opened
        val playback = PlaybackSelection(
            accountId = selection.accountId,
            accountGeneration = selection.accountGeneration,
            section = CatalogSection.Movies,
            providerItemId = selection.item.providerId,
            containerExtension = selection.item.containerExtension,
        )
        canResume = (resumeRepository.load(playback) as? MovieResumeLoadResult.Ready)?.record != null
        try {
            repository.details(opened) { next -> if (destination == opened) state = next }
            awaitCancellation()
        } finally {
            withContext(NonCancellable) { repository.close(opened) }
        }
    }

    MovieDetailsContent(
        selection = selection,
        state = state,
        ownerChanged = ownerChanged,
        canResume = canResume,
        onBack = onBack,
        onPlay = { onPlay(selection.item) },
        onRefresh = {
            destination?.let { current ->
                scope.launch { repository.details(current, true) { next -> if (destination == current) state = next } }
            }
        },
    )
}

@Composable
internal fun MovieDetailsContent(
    selection: MovieSelection,
    state: MovieDetailsState,
    ownerChanged: Boolean,
    canResume: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onPlay: () -> Unit,
) {
    val details = when (state) {
        is MovieDetailsState.Content -> state.details
        is MovieDetailsState.StaleContent -> state.details
        else -> null
    }
    val initialFocus = rememberInitialFocusRequester(selection.item.providerId)
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("movie-details"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item("actions") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FocusVisibleButton(
                        label = stringResource(R.string.back),
                        onClick = onBack,
                        modifier = Modifier.focusRequester(initialFocus).testTag("movie-back"),
                    )
                    if (details != null || state is MovieDetailsState.Error) {
                        FocusVisibleButton(stringResource(R.string.movie_refresh), onClick = onRefresh)
                    }
                }
                FocusVisibleButton(
                    stringResource(if (canResume) R.string.movie_resume else R.string.movie_play),
                    onClick = onPlay,
                    modifier = Modifier.testTag("movie-play"),
                )
            }
        }
        item("artwork") {
            val artwork = details?.backdropUrl ?: details?.posterUrl ?: selection.item.artworkUrl
            val landscape = details?.backdropUrl != null
            if (artwork != null) {
                Box(
                    Modifier
                        .fillMaxWidth(if (landscape) 1f else 0.55f)
                        .widthIn(max = if (landscape) 900.dp else 320.dp)
                        .heightIn(max = 420.dp),
                ) {
                    AsyncImage(
                        model = artwork,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().aspectRatio(if (landscape) 16f / 9f else 2f / 3f),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
        item("summary") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.widthIn(max = 900.dp)) {
                Text(details?.name ?: selection.item.name, style = MaterialTheme.typography.headlineMedium, maxLines = 2)
                val facts = listOfNotNull(
                    details?.releaseDate ?: selection.item.releaseYear,
                    details?.rating ?: selection.item.rating,
                    details?.duration,
                    details?.genre,
                ).filter(String::isNotBlank).joinToString(" • ")
                if (facts.isNotBlank()) Text(facts, color = MaterialTheme.colorScheme.onSurfaceVariant)
                details?.plot?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                details?.cast?.takeIf(String::isNotBlank)?.let {
                    Text(stringResource(R.string.movie_cast, it), style = MaterialTheme.typography.bodyMedium)
                }
                details?.director?.takeIf(String::isNotBlank)?.let {
                    Text(stringResource(R.string.movie_director, it), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        when {
            ownerChanged -> item("owner") { Status(R.string.movie_account_changed, true) }
            state == MovieDetailsState.Empty || state == MovieDetailsState.Loading -> item("loading") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(); Status(R.string.movie_loading)
                }
            }
            state is MovieDetailsState.Error -> item("error") { Status(state.failure.message(), true) }
            state is MovieDetailsState.StaleContent -> item("stale") {
                Text(stringResource(R.string.movie_stale, stringResource(state.failure.message())), color = MaterialTheme.colorScheme.error)
            }
            state is MovieDetailsState.Content && state.isRefreshing -> item("refreshing") { Status(R.string.movie_refreshing) }
        }
    }
}

@Composable private fun Status(@StringRes message: Int, error: Boolean = false) = Text(
    stringResource(message), color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
)

@StringRes private fun MovieDetailsFailure.message(): Int = when (this) {
    MovieDetailsFailure.NetworkUnavailable -> R.string.catalog_error_network
    MovieDetailsFailure.Timeout -> R.string.catalog_error_timeout
    MovieDetailsFailure.ProviderUnavailable -> R.string.catalog_error_provider
    MovieDetailsFailure.AuthenticationRejected -> R.string.catalog_error_authentication
    MovieDetailsFailure.MalformedResponse -> R.string.movie_error_malformed
    MovieDetailsFailure.UnsupportedResponse -> R.string.movie_error_unsupported
    MovieDetailsFailure.ResponseTooLarge -> R.string.catalog_error_too_large
    MovieDetailsFailure.LocalStorage -> R.string.catalog_error_storage
    MovieDetailsFailure.Unknown -> R.string.movie_error_unknown
}
