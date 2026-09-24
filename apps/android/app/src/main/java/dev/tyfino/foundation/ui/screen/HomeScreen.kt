package dev.tyfino.foundation.ui.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.tyfino.foundation.R
import dev.tyfino.foundation.notifications.NewContentNotifier
import dev.tyfino.foundation.playback.CatalogHistoryListResult
import dev.tyfino.foundation.playback.CatalogHistoryRepository
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
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.ui.components.FocusIconButton
import dev.tyfino.foundation.ui.components.rememberInitialFocusRequester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class HomeHistory(
    val latestMovies: List<CatalogItem> = emptyList(),
    val latestSeries: List<CatalogItem> = emptyList(),
    val live: List<CatalogItem> = emptyList(),
    val movies: List<CatalogItem> = emptyList(),
    val resumeMovies: List<CatalogItem> = emptyList(),
    val seriesResume: List<SeriesContinueWatchingItem> = emptyList(),
    val seriesHistory: List<SeriesHistoryItem> = emptyList(),
)

private sealed interface HomeFeatured {
    val title: String
    val artworkUrl: String?

    data class ResumeMovie(val item: CatalogItem) : HomeFeatured {
        override val title = item.name
        override val artworkUrl = item.artworkUrl
    }
    data class ResumeSeries(val item: SeriesContinueWatchingItem) : HomeFeatured {
        override val title = item.seriesTitle
        override val artworkUrl = item.seriesArtworkUrl
    }
    data class NewMovie(val item: CatalogItem) : HomeFeatured {
        override val title = item.name
        override val artworkUrl = item.artworkUrl
    }
    data class NewSeries(val item: CatalogItem) : HomeFeatured {
        override val title = item.name
        override val artworkUrl = item.artworkUrl
    }
}

private fun HomeHistory.featured(): HomeFeatured? =
    resumeMovies.firstOrNull()?.let { HomeFeatured.ResumeMovie(it) }
        ?: seriesResume.firstOrNull()?.let { HomeFeatured.ResumeSeries(it) }
        ?: latestMovies.firstOrNull()?.let { HomeFeatured.NewMovie(it) }
        ?: latestSeries.firstOrNull()?.let { HomeFeatured.NewSeries(it) }

@Composable
internal fun HomeScreen(
    onOpenAccountSwitcher: () -> Unit,
    onOpenSettings: () -> Unit,
    accountStore: XtreamAccountStore? = null,
    catalogRepository: CatalogRepository? = null,
    historyRepository: CatalogHistoryRepository? = null,
    movieResumeRepository: MovieResumeRepository? = null,
    episodeResumeRepository: EpisodeResumeRepository? = null,
    episodeHistoryRepository: EpisodeHistoryRepository? = null,
    newContentNotifier: NewContentNotifier? = null,
    onPlayLive: (CatalogItem) -> Unit = {},
    onResumeMovie: (CatalogItem) -> Unit = {},
    onOpenMovie: (CatalogItem) -> Unit = {},
    onOpenSeries: (CatalogItem) -> Unit = {},
    onResumeSeries: (SeriesContinueWatchingItem) -> Unit = {},
    onPlaySeriesHistory: (SeriesHistoryItem) -> Unit = {},
) {
    val initialFocus = rememberInitialFocusRequester()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var history by remember { mutableStateOf(HomeHistory()) }
    var historyLoaded by remember { mutableStateOf(false) }
    var alertsEnabled by remember(newContentNotifier) { mutableStateOf(newContentNotifier?.isEnabled() == true) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        newContentNotifier?.setEnabled(granted)
        alertsEnabled = granted
    }
    DisposableEffect(lifecycleOwner, accountStore, catalogRepository, historyRepository, movieResumeRepository, episodeResumeRepository, episodeHistoryRepository) {
        var generation = 0
        fun refresh() {
            val request = ++generation
            history = HomeHistory()
            historyLoaded = false
            alertsEnabled = newContentNotifier?.let { it.isEnabled() && it.canNotify() } == true
            if (accountStore == null || catalogRepository == null || historyRepository == null ||
                movieResumeRepository == null || episodeResumeRepository == null || episodeHistoryRepository == null
            ) {
                historyLoaded = true
                return
            }
            scope.launch {
                val owner = withContext(Dispatchers.IO) { accountStore.load()?.let { it.accountId to it.generation } }
                if (owner == null) {
                    if (request == generation) historyLoaded = true
                    return@launch
                }
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
                val latestSeries = catalogRepository.latestCachedSeries()
                val currentOwner = withContext(Dispatchers.IO) { accountStore.load()?.let { it.accountId to it.generation } }
                if (request == generation && owner == currentOwner) {
                    history = HomeHistory(
                        latestMovies = latestMovies,
                        latestSeries = latestSeries,
                        live = live.take(8),
                        movies = (movies + movieResume.map { it.catalogItem }).distinctBy { it.providerId }.take(8),
                        resumeMovies = movieResume.map { it.catalogItem }.take(8),
                        seriesResume = series.take(8),
                        seriesHistory = seriesHistory.distinctBy { it.episode.providerSeriesId }.take(8),
                    )
                    historyLoaded = true
                }
            }
        }
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) refresh()
        onDispose { generation++; lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val featured = history.featured()
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 200.dp),
            modifier = Modifier
                .width(minOf(maxWidth, 900.dp))
                .fillMaxHeight()
                .focusGroup()
                .testTag("home-screen"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, contentType = "heading") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("TYFINO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineSmall)
                        }
                        if (newContentNotifier != null) FocusIconButton(
                            icon = R.drawable.ic_notifications,
                            description = stringResource(if (alertsEnabled) R.string.new_content_disable else R.string.new_content_enable),
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
                            modifier = Modifier
                                .then(if (featured == null) Modifier.focusRequester(initialFocus) else Modifier)
                                .testTag("home-content-alerts"),
                            selected = alertsEnabled,
                        )
                    }
                    Text(
                        stringResource(R.string.home_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (featured != null) item(span = { GridItemSpan(maxLineSpan) }, contentType = "featured") {
                HomeHero(
                    featured = featured,
                    onClick = {
                        when (featured) {
                            is HomeFeatured.ResumeMovie -> onResumeMovie(featured.item)
                            is HomeFeatured.ResumeSeries -> onResumeSeries(featured.item)
                            is HomeFeatured.NewMovie -> onOpenMovie(featured.item)
                            is HomeFeatured.NewSeries -> onOpenSeries(featured.item)
                        }
                    },
                    modifier = Modifier.focusRequester(initialFocus),
                )
            }
            if (historyLoaded && history.latestMovies.isEmpty() && history.latestSeries.isEmpty() &&
                history.live.isEmpty() && history.movies.isEmpty() &&
                history.seriesHistory.isEmpty() && history.seriesResume.isEmpty()
            ) item(span = { GridItemSpan(maxLineSpan) }, contentType = "empty-content") {
                Text(stringResource(R.string.home_empty_sections),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("home-empty-content"))
            }
            if (history.latestMovies.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }, contentType = "latest-movies") {
                HomeRecentStrip(stringResource(R.string.home_latest_movies), history.latestMovies, onOpenMovie, "home-latest-movies")
            }
            if (history.latestSeries.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }, contentType = "latest-series") {
                HomeRecentStrip(stringResource(R.string.home_latest_series), history.latestSeries, onOpenSeries, "home-latest-series")
            }
            if (history.live.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }, contentType = "recent-live") {
                HomeRecentStrip(stringResource(R.string.home_recent_live), history.live, onPlayLive,
                    "home-recent-live", 16f / 9f)
            }
            if (history.seriesHistory.isNotEmpty() || history.seriesResume.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }, contentType = "recent-series") {
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
            if (history.movies.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }, contentType = "recent-movies") {
                HomeRecentStrip(stringResource(R.string.home_recent_movies), history.movies, onResumeMovie, "home-recent-movies")
            }
            item(span = { GridItemSpan(maxLineSpan) }, contentType = "account") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.home_account_title), style = MaterialTheme.typography.titleMedium)
                    FocusVisibleButton(
                        label = stringResource(R.string.xtream_switch_account),
                        onClick = onOpenAccountSwitcher,
                        modifier = Modifier.fillMaxWidth()
                            .then(if (newContentNotifier == null && featured == null) Modifier.focusRequester(initialFocus) else Modifier)
                            .testTag("open-account-switcher"),
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
private fun HomeHero(featured: HomeFeatured, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val request = remember(context, featured.artworkUrl) {
        featured.artworkUrl?.let { ImageRequest.Builder(context).data(it).crossfade(150).build() }
    }
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("home-featured")) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = if (maxWidth < 600.dp) 220.dp else 300.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (request != null) AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
                Color.Black.copy(alpha = 0.32f), Color.Black.copy(alpha = 0.94f),
            ))))
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(if (featured is HomeFeatured.ResumeMovie || featured is HomeFeatured.ResumeSeries)
                        R.string.home_featured_continue else R.string.home_featured_new),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
                Text(featured.title, style = MaterialTheme.typography.headlineSmall, color = Color.White,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                FocusVisibleButton(
                    label = stringResource(if (featured is HomeFeatured.ResumeMovie || featured is HomeFeatured.ResumeSeries)
                        R.string.continue_watching_resume else R.string.home_featured_details),
                    onClick = onClick,
                    modifier = modifier.testTag("home-featured-action"),
                )
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
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag(tag)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
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
