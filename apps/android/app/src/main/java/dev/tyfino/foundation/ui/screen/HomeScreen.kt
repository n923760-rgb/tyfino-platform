package dev.tyfino.foundation.ui.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.tyfino.foundation.R
import dev.tyfino.foundation.notifications.NewContentNotifier
import dev.tyfino.foundation.app.AppDestination
import dev.tyfino.foundation.playback.CatalogHistoryListResult
import dev.tyfino.foundation.playback.CatalogHistoryRepository
import dev.tyfino.foundation.playback.ContinueWatchingItem
import dev.tyfino.foundation.playback.EpisodeResumeListResult
import dev.tyfino.foundation.playback.EpisodeResumeRepository
import dev.tyfino.foundation.playback.EpisodeHistoryListResult
import dev.tyfino.foundation.playback.EpisodeHistoryRepository
import dev.tyfino.foundation.playback.MovieResumeListResult
import dev.tyfino.foundation.playback.MovieResumePresentation
import dev.tyfino.foundation.playback.MovieResumeRepository
import dev.tyfino.foundation.playback.SeriesContinueWatchingItem
import dev.tyfino.foundation.playback.SeriesHistoryItem
import dev.tyfino.foundation.xtream.CatalogItem
import dev.tyfino.foundation.xtream.CatalogRepository
import dev.tyfino.foundation.xtream.CatalogSection
import dev.tyfino.foundation.xtream.XtreamAccountStore
import dev.tyfino.foundation.xtream.SeriesStore
import dev.tyfino.foundation.xtream.PublishedEpisode
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.ui.components.rememberInitialFocusRequester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class HomeShortcut(
    val destination: AppDestination,
    @StringRes val description: Int,
)

private val shortcuts = listOf(
    HomeShortcut(AppDestination.Live, R.string.home_live_description),
    HomeShortcut(AppDestination.Movies, R.string.home_movies_description),
    HomeShortcut(AppDestination.Series, R.string.home_series_description),
)

private data class HomeHistory(
    val latestMovies: List<CatalogItem> = emptyList(),
    val latestEpisodes: List<PublishedEpisode> = emptyList(),
    val live: List<CatalogItem> = emptyList(),
    val movies: List<CatalogItem> = emptyList(),
    val movieResume: List<ContinueWatchingItem> = emptyList(),
    val seriesResume: List<SeriesContinueWatchingItem> = emptyList(),
    val seriesHistory: List<SeriesHistoryItem> = emptyList(),
)

@Composable
internal fun HomeScreen(
    onOpenDestination: (AppDestination) -> Unit,
    onOpenAccountSwitcher: () -> Unit,
    onOpenSettings: () -> Unit,
    accountStore: XtreamAccountStore? = null,
    catalogRepository: CatalogRepository? = null,
    historyRepository: CatalogHistoryRepository? = null,
    movieResumeRepository: MovieResumeRepository? = null,
    episodeResumeRepository: EpisodeResumeRepository? = null,
    episodeHistoryRepository: EpisodeHistoryRepository? = null,
    seriesStore: SeriesStore? = null,
    newContentNotifier: NewContentNotifier? = null,
    onPlayLive: (CatalogItem) -> Unit = {},
    onResumeMovie: (CatalogItem) -> Unit = {},
    onOpenMovie: (CatalogItem) -> Unit = {},
    onResumeSeries: (SeriesContinueWatchingItem) -> Unit = {},
    onPlaySeriesHistory: (SeriesHistoryItem) -> Unit = {},
) {
    val initialFocus = rememberInitialFocusRequester()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var history by remember { mutableStateOf(HomeHistory()) }
    var alertsEnabled by remember(newContentNotifier) { mutableStateOf(newContentNotifier?.isEnabled() == true) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        newContentNotifier?.setEnabled(granted)
        alertsEnabled = granted
    }
    DisposableEffect(lifecycleOwner, accountStore, catalogRepository, historyRepository, movieResumeRepository, episodeResumeRepository, episodeHistoryRepository, seriesStore) {
        var generation = 0
        fun refresh() {
            val request = ++generation
            history = HomeHistory()
            if (accountStore == null || catalogRepository == null || historyRepository == null ||
                movieResumeRepository == null || episodeResumeRepository == null || episodeHistoryRepository == null || seriesStore == null
            ) return
            scope.launch {
                val owner = withContext(Dispatchers.IO) { accountStore.load()?.let { it.accountId to it.generation } }
                    ?: return@launch
                val live = (historyRepository.recent(CatalogSection.Live) as? CatalogHistoryListResult.Ready)?.items.orEmpty()
                val movies = (historyRepository.recent(CatalogSection.Movies) as? CatalogHistoryListResult.Ready)?.items.orEmpty()
                val resumes = (movieResumeRepository.continueWatching() as? MovieResumeListResult.Ready)?.records.orEmpty()
                val movieResume = MovieResumePresentation.assemble(
                    resumes,
                    catalogRepository.cachedItems(CatalogSection.Movies, resumes.mapTo(linkedSetOf()) { it.providerItemId }),
                )
                val series = (episodeResumeRepository.continueWatching() as? EpisodeResumeListResult.Ready)?.items.orEmpty()
                val seriesHistory = (episodeHistoryRepository.recent() as? EpisodeHistoryListResult.Ready)?.items.orEmpty()
                val latestMovies = catalogRepository.latestCachedMovies()
                val latestEpisodes = withContext(Dispatchers.IO) {
                    runCatching { seriesStore.latestDatedEpisodes(owner.first, owner.second, 8) }.getOrDefault(emptyList())
                }
                val currentOwner = withContext(Dispatchers.IO) { accountStore.load()?.let { it.accountId to it.generation } }
                if (request == generation && owner == currentOwner) {
                    history = HomeHistory(
                        latestMovies = latestMovies,
                        latestEpisodes = latestEpisodes,
                        live = live.take(8),
                        movies = (movies + movieResume.map { it.catalogItem }).distinctBy { it.providerId }.take(8),
                        movieResume = movieResume.take(8),
                        seriesResume = series.take(8),
                        seriesHistory = seriesHistory.distinctBy { it.episode.providerSeriesId }.take(8),
                    )
                }
            }
        }
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) refresh()
        onDispose { generation++; lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 200.dp),
            modifier = Modifier
                .width(minOf(maxWidth, 900.dp))
                .fillMaxHeight()
                .focusGroup()
                .testTag("home-screen"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, contentType = "heading") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("TYFINO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineLarge)
                    Text(
                        stringResource(R.string.home_description),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }, contentType = "latest-movies") {
                HomeRecentStrip(stringResource(R.string.home_latest_movies), history.latestMovies, onOpenMovie,
                    "home-latest-movies", onBrowse = { onOpenDestination(AppDestination.Movies) }, initialFocus = initialFocus)
            }
            item(span = { GridItemSpan(maxLineSpan) }, contentType = "latest-episodes") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("home-latest-episodes")) {
                    Text(stringResource(R.string.home_latest_episodes), style = MaterialTheme.typography.titleMedium)
                    if (history.latestEpisodes.isEmpty()) {
                        Text(stringResource(R.string.home_latest_episodes_empty), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.focusGroup()) {
                        rowItems(history.latestEpisodes, key = { "${it.episode.providerSeriesId}:${it.episode.providerEpisodeId}" }) { item ->
                            CatalogTile(
                                label = listOfNotNull(item.seriesTitle, item.episode.title).filter(String::isNotBlank).joinToString(" • "),
                                selected = false,
                                onClick = { onPlaySeriesHistory(SeriesHistoryItem(item.episode, item.seriesTitle,
                                    item.seriesGeneration, item.accountGeneration, 0L, item.seriesCoverUrl)) },
                                modifier = Modifier.width(120.dp), showArtwork = true,
                                artworkUrl = item.seriesCoverUrl, artworkAspectRatio = 2f / 3f,
                            )
                        }
                    }
                }
            }
            if (history.live.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "recent-live") {
                    HomeRecentStrip(stringResource(R.string.home_recent_live), history.live, onPlayLive, "home-recent-live", 16f / 9f)
                }
            }
            if (history.seriesHistory.isNotEmpty() || history.seriesResume.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "recent-series") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("home-recent-series")) {
                        Text(stringResource(R.string.home_recent_series), style = MaterialTheme.typography.titleMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.focusGroup()) {
                            rowItems(history.seriesHistory, key = { "${it.episode.providerSeriesId}:${it.episode.providerEpisodeId}" }) { item ->
                                CatalogTile(
                                    label = listOfNotNull(item.seriesTitle, item.episode.title).filter(String::isNotBlank).joinToString(" • "),
                                    selected = false,
                                    onClick = { onPlaySeriesHistory(item) },
                                    modifier = Modifier.width(120.dp),
                                    showArtwork = true,
                                    artworkUrl = item.seriesArtworkUrl,
                                    artworkAspectRatio = 2f / 3f,
                                )
                            }
                            rowItems(history.seriesResume.filter { resume -> history.seriesHistory.none {
                                it.episode.providerSeriesId == resume.episode.providerSeriesId
                            } }, key = { "resume:${it.episode.providerSeriesId}" }) { item ->
                                CatalogTile(
                                    label = listOfNotNull(item.seriesTitle, item.episode.title).filter(String::isNotBlank).joinToString(" • "),
                                    selected = false, onClick = { onResumeSeries(item) }, modifier = Modifier.width(120.dp),
                                    showArtwork = true, artworkUrl = item.seriesArtworkUrl, artworkAspectRatio = 2f / 3f,
                                )
                            }
                        }
                    }
                }
            }
            if (history.movies.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "recent-movies") {
                    HomeRecentStrip(stringResource(R.string.home_recent_movies), history.movies, onResumeMovie, "home-recent-movies")
                }
            }
            items(shortcuts, key = { it.destination.route }, contentType = { "shortcut" }) { shortcut ->
                HomeShortcutCard(
                    title = stringResource(shortcut.destination.labelRes),
                    description = stringResource(shortcut.description),
                    onClick = { onOpenDestination(shortcut.destination) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home-${shortcut.destination.route}"),
                )
            }
            if (newContentNotifier != null) item(span = { GridItemSpan(maxLineSpan) }, contentType = "alerts") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FocusVisibleButton(
                    label = stringResource(if (alertsEnabled) R.string.new_content_disable else R.string.new_content_enable),
                    onClick = {
                        if (alertsEnabled) {
                            newContentNotifier.setEnabled(false)
                            alertsEnabled = false
                        } else if (Build.VERSION.SDK_INT >= 33 && !newContentNotifier.canNotify()) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            newContentNotifier.setEnabled(true)
                            alertsEnabled = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("home-content-alerts"),
                )
                Text(stringResource(R.string.new_content_scope), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }, contentType = "account") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.home_account_title), style = MaterialTheme.typography.titleMedium)
                    FocusVisibleButton(
                        label = stringResource(R.string.xtream_switch_account),
                        onClick = onOpenAccountSwitcher,
                        modifier = Modifier.fillMaxWidth().testTag("open-account-switcher"),
                    )
                    FocusVisibleButton(
                        label = stringResource(R.string.open_settings),
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth().testTag("home-open-settings"),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeRecentStrip(
    title: String,
    records: List<CatalogItem>,
    onClick: (CatalogItem) -> Unit,
    tag: String,
    artworkAspectRatio: Float = 2f / 3f,
    onBrowse: (() -> Unit)? = null,
    initialFocus: FocusRequester? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag(tag)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (onBrowse != null) CatalogFilterButton(
                label = stringResource(R.string.home_browse_movies), selected = false, onClick = onBrowse,
                modifier = if (initialFocus != null) Modifier.focusRequester(initialFocus) else Modifier,
            )
        }
        if (records.isEmpty()) {
            Text(stringResource(R.string.home_latest_movies_empty), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.focusGroup()) {
            rowItems(records, key = { it.providerId }) { item ->
                CatalogTile(
                    label = item.name,
                    selected = false,
                    onClick = { onClick(item) },
                    modifier = Modifier.width(120.dp),
                    showArtwork = true,
                    artworkUrl = item.artworkUrl,
                    artworkAspectRatio = artworkAspectRatio,
                )
            }
        }
    }
}

@Composable
private fun HomeShortcutCard(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 88.dp)
            .onFocusChanged { focused = it.isFocused },
        border = BorderStroke(
            if (focused) 3.dp else 1.dp,
            if (focused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
