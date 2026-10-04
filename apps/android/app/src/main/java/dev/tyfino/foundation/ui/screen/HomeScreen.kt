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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import dev.tyfino.foundation.ui.components.ProductHeader
import dev.tyfino.foundation.ui.components.ProductPanel
import dev.tyfino.foundation.ui.components.ProductSectionHeading
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import dev.tyfino.foundation.playback.ContinueWatchingItem
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
import java.text.NumberFormat
import java.util.Locale

private data class HomeHistory(
    val latestMovies: List<CatalogItem> = emptyList(),
    val latestSeries: List<CatalogItem> = emptyList(),
    val live: List<CatalogItem> = emptyList(),
    val movies: List<CatalogItem> = emptyList(),
    val resumeMovies: List<ContinueWatchingItem> = emptyList(),
    val seriesResume: List<SeriesContinueWatchingItem> = emptyList(),
    val seriesHistory: List<SeriesHistoryItem> = emptyList(),
)

private sealed interface HomeFeatured {
    val title: String
    val artworkUrl: String?

    data class NewMovie(val item: CatalogItem) : HomeFeatured {
        override val title = item.name
        override val artworkUrl = item.artworkUrl
    }
    data class NewSeries(val item: CatalogItem) : HomeFeatured {
        override val title = item.name
        override val artworkUrl = item.artworkUrl
    }
}

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
    onOpenCatalog: (CatalogSection) -> Unit = {},
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
                        resumeMovies = movieResume.take(8),
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
    val highlights = remember(history.latestMovies, history.latestSeries) {
        HomePresentation.highlights(history.latestMovies, history.latestSeries)
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val expanded = maxWidth >= 840.dp
        val posterWidth = if (expanded) 180.dp else 128.dp
        val channelWidth = if (expanded) 236.dp else 176.dp
        val resumeWidth = if (expanded) 300.dp else 240.dp
        LazyColumn(
            modifier = Modifier
                .width(minOf(maxWidth, 1280.dp))
                .fillMaxHeight()
                .focusGroup()
                .testTag("home-screen"),
            contentPadding = PaddingValues(horizontal = if (expanded) 48.dp else 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(contentType = "heading") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProductHeader(
                            title = stringResource(R.string.home_title),
                            subtitle = stringResource(R.string.product_home_description),
                            modifier = Modifier.weight(1f),
                        )
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
                                .testTag("home-content-alerts"),
                            selected = alertsEnabled,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().focusGroup()) {
                        FocusVisibleButton(
                            label = stringResource(R.string.xtream_switch_account),
                            prominent = false,
                            onClick = onOpenAccountSwitcher,
                            modifier = Modifier.weight(1f)
                                .then(if (highlights.isEmpty()) Modifier.focusRequester(initialFocus) else Modifier)
                                .testTag("open-account-switcher"),
                        )
                        FocusVisibleButton(
                            label = stringResource(R.string.destination_settings),
                            prominent = false,
                            onClick = onOpenSettings,
                            modifier = Modifier.weight(1f).testTag("home-open-settings"),
                        )
                    }
                }
            }
            if (highlights.isNotEmpty()) item(contentType = "featured") {
                HomeShowcase(
                    highlights, onOpenMovie, onOpenSeries,
                    Modifier.focusRequester(initialFocus),
                )
            }
            item(contentType = "browse") {
                HomeBrowseSections(onOpenCatalog = onOpenCatalog)
            }
            if (history.resumeMovies.isNotEmpty() || history.seriesResume.isNotEmpty()) item(contentType = "continue") {
                HomeContinueWatching(history.resumeMovies, history.seriesResume,
                    onResumeMovie, onResumeSeries, resumeWidth)
            }
            if (historyLoaded && history.latestMovies.isEmpty() && history.latestSeries.isEmpty() &&
                history.live.isEmpty() && history.movies.isEmpty() &&
                history.seriesHistory.isEmpty() && history.seriesResume.isEmpty()
            ) item(contentType = "empty-content") {
                ProductPanel(modifier = Modifier.fillMaxWidth().testTag("home-empty-content")) {
                    Text(stringResource(R.string.home_empty_sections),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (history.latestMovies.isNotEmpty()) item(contentType = "latest-movies") {
                HomeRecentStrip(stringResource(R.string.home_latest_movies), history.latestMovies, onOpenMovie,
                    "home-latest-movies", posterWidth)
            }
            if (history.latestSeries.isNotEmpty()) item(contentType = "latest-series") {
                HomeRecentStrip(stringResource(R.string.home_latest_series), history.latestSeries, onOpenSeries,
                    "home-latest-series", posterWidth)
            }
            if (history.live.isNotEmpty()) item(contentType = "recent-live") {
                HomeRecentStrip(stringResource(R.string.home_recent_live), history.live, onPlayLive,
                    "home-recent-live", channelWidth, 16f / 9f)
            }
            if (history.movies.isNotEmpty()) item(contentType = "recent-movies") {
                HomeRecentStrip(stringResource(R.string.home_recent_movies), history.movies, onResumeMovie,
                    "home-recent-movies", posterWidth)
            }
            if (history.seriesHistory.isNotEmpty() || history.seriesResume.isNotEmpty()) item(contentType = "recent-series") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("home-recent-series")) {
                        ProductSectionHeading(stringResource(R.string.home_recent_series))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.focusGroup()) {
                            rowItems(history.seriesHistory, key = { "${it.episode.providerSeriesId}:${it.episode.providerEpisodeId}" }) { item ->
                                CatalogTile(
                                    label = listOfNotNull(item.seriesTitle, item.episode.title).filter(String::isNotBlank).joinToString(" • "),
                                    selected = false,
                                    onClick = { onPlaySeriesHistory(item) },
                                    modifier = Modifier.width(posterWidth),
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
                                    selected = false, onClick = { onResumeSeries(item) }, modifier = Modifier.width(posterWidth),
                                    showArtwork = true, artworkUrl = item.seriesArtworkUrl, artworkAspectRatio = 2f / 3f,
                                )
                            }
                        }
                    }
            }
        }
    }
}

@Composable
internal fun HomeShowcase(
    highlights: List<HomeHighlight>,
    onOpenMovie: (CatalogItem) -> Unit,
    onOpenSeries: (CatalogItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (highlights.isEmpty()) return
    var selected by remember(highlights) { mutableStateOf(0) }
    val current = highlights[selected.coerceIn(highlights.indices)]
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.testTag("home-showcase")) {
        ProductSectionHeading(stringResource(R.string.home_showcase_title))
        HomeHero(
            if (current.section == CatalogSection.Movies) HomeFeatured.NewMovie(current.item)
            else HomeFeatured.NewSeries(current.item),
            onClick = {
                if (current.section == CatalogSection.Movies) onOpenMovie(current.item) else onOpenSeries(current.item)
            },
            modifier = modifier,
        )
        if (highlights.size > 1) Row(
            Modifier.fillMaxWidth().focusGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FocusVisibleButton(stringResource(R.string.home_showcase_previous),
                { selected = (selected - 1 + highlights.size) % highlights.size },
                prominent = false, modifier = Modifier.weight(1f).testTag("home-showcase-previous"))
            Text(stringResource(R.string.home_showcase_position, selected + 1, highlights.size),
                style = MaterialTheme.typography.labelLarge)
            FocusVisibleButton(stringResource(R.string.home_showcase_next),
                { selected = (selected + 1) % highlights.size },
                prominent = false, modifier = Modifier.weight(1f).testTag("home-showcase-next"))
        }
        Text(stringResource(R.string.home_showcase_cached), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun HomeContinueWatching(
    movies: List<ContinueWatchingItem>,
    series: List<SeriesContinueWatchingItem>,
    onMovie: (CatalogItem) -> Unit,
    onSeries: (SeriesContinueWatchingItem) -> Unit,
    tileWidth: Dp,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.testTag("home-continue-watching")) {
        ProductSectionHeading(stringResource(R.string.home_continue_title))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.focusGroup()) {
            rowItems(movies, key = { "movie:${it.catalogItem.providerId}" }) { record ->
                HomeResumeCard(record.catalogItem.name, record.catalogItem.artworkUrl, record.progressPercent,
                    tileWidth, "home-resume-movie-${record.catalogItem.providerId}") { onMovie(record.catalogItem) }
            }
            rowItems(series, key = { "episode:${it.episode.providerSeriesId}:${it.episode.providerEpisodeId}" }) { record ->
                HomeResumeCard(listOfNotNull(record.seriesTitle, record.episode.title).filter(String::isNotBlank).joinToString(" • "),
                    record.seriesArtworkUrl, record.progressPercent, tileWidth,
                    "home-resume-series-${record.episode.providerEpisodeId}") { onSeries(record) }
            }
        }
    }
}

@Composable
private fun HomeResumeCard(title: String, artwork: String?, progress: Int?, width: Dp, tag: String, onClick: () -> Unit) {
    Column(Modifier.width(width), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CatalogTile(label = title, selected = false, onClick = onClick,
            supporting = progress?.let { stringResource(R.string.continue_watching_progress, it) }
                ?: stringResource(R.string.continue_watching_resume),
            modifier = Modifier.fillMaxWidth().testTag(tag), showArtwork = true,
            artworkUrl = artwork, artworkAspectRatio = 16f / 9f)
        progress?.let { LinearProgressIndicator(progress = { it.coerceIn(0, 100) / 100f }, modifier = Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun HomeHero(featured: HomeFeatured, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val request = remember(context, featured.artworkUrl) {
        featured.artworkUrl?.let { ImageRequest.Builder(context).data(it).crossfade(150).build() }
    }
    val metadata = remember(featured) { HomePresentation.metadata(when (featured) {
        is HomeFeatured.NewMovie -> featured.item
        is HomeFeatured.NewSeries -> featured.item
    }) }
    val locale = Locale.forLanguageTag(LocalLocale.current.toLanguageTag())
    val numbers = remember(locale) { NumberFormat.getNumberInstance(locale).apply {
        isGroupingUsed = false
        maximumFractionDigits = 1
    } }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("home-featured")) {
        val spacious = maxWidth >= 600.dp
        Box(
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = if (spacious) 360.dp else 280.dp)
                .clip(MaterialTheme.shapes.large)
                .background(Brush.linearGradient(listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.background,
                ))),
        ) {
            if (request != null) AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            val backdrop = MaterialTheme.colorScheme.background
            val shades = listOf(backdrop.copy(alpha = 0.98f), backdrop.copy(alpha = 0.72f),
                backdrop.copy(alpha = 0.5f))
            Box(Modifier.matchParentSize().background(
                if (spacious) Brush.horizontalGradient(if (rtl) shades.reversed() else shades)
                else Brush.verticalGradient(shades.reversed()),
            ))
            Column(
                modifier = Modifier.align(Alignment.BottomStart).widthIn(max = 560.dp).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(color = Color.Black.copy(alpha = 0.45f), contentColor = Color.White,
                    shape = RoundedCornerShape(50), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(painterResource(if (featured is HomeFeatured.NewSeries) R.drawable.ic_nav_series
                            else R.drawable.ic_nav_movies), contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Text(stringResource(if (featured is HomeFeatured.NewSeries) R.string.destination_series
                            else R.string.destination_movies), style = MaterialTheme.typography.labelLarge)
                    }
                }
                Text(featured.title, style = if (spacious) MaterialTheme.typography.headlineMedium
                    else MaterialTheme.typography.headlineSmall, color = Color.White,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (metadata.rating != null || metadata.releaseYear != null) FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().testTag("home-featured-metadata"),
                ) {
                    metadata.rating?.let { rating -> HomeMetadataBadge(
                        stringResource(R.string.home_showcase_rating, numbers.format(rating), numbers.format(10)),
                        "home-featured-rating") }
                    metadata.releaseYear?.let { year -> HomeMetadataBadge(
                        stringResource(R.string.home_showcase_year, numbers.format(year)), "home-featured-year") }
                }
                FocusVisibleButton(
                    label = stringResource(R.string.home_featured_details),
                    onClick = onClick,
                    modifier = modifier.testTag("home-featured-action"),
                )
            }
        }
    }
}

@Composable
private fun HomeMetadataBadge(label: String, tag: String) {
    Surface(color = Color.Black.copy(alpha = 0.55f), contentColor = Color.White,
        shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))) {
        Text(label, style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).testTag(tag))
    }
}

@Composable
private fun HomeRecentStrip(
    title: String,
    records: List<CatalogItem>,
    onClick: (CatalogItem) -> Unit,
    tag: String,
    tileWidth: Dp,
    artworkAspectRatio: Float = 2f / 3f,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag(tag)) {
        ProductSectionHeading(title)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.focusGroup()) {
            rowItems(records, key = { it.providerId }) { item ->
                CatalogTile(
                    label = item.name,
                    selected = false,
                    onClick = { onClick(item) },
                    modifier = Modifier.width(tileWidth),
                    showArtwork = true,
                    artworkUrl = item.artworkUrl,
                    artworkAspectRatio = artworkAspectRatio,
                )
            }
        }
    }
}

/** Pure adaptive shortcuts; only explicit touch or directional activation emits a destination. */
@Composable
internal fun HomeBrowseSections(onOpenCatalog: (CatalogSection) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.testTag("home-browse")) {
        ProductSectionHeading(stringResource(R.string.product_browse_title))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val readableWidth = 96.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
            if (maxWidth >= readableWidth * 3f + 24.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().focusGroup()) {
                    CatalogSection.entries.forEach { section ->
                        HomeBrowseCard(section, { onOpenCatalog(section) }, Modifier.weight(1f), stacked = true)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.focusGroup()) {
                    CatalogSection.entries.forEach { section ->
                        HomeBrowseCard(section, { onOpenCatalog(section) }, Modifier.fillMaxWidth(), stacked = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeBrowseCard(section: CatalogSection, onClick: () -> Unit, modifier: Modifier, stacked: Boolean) {
    var focused by remember { mutableStateOf(false) }
    val label = when (section) {
        CatalogSection.Live -> R.string.destination_live
        CatalogSection.Movies -> R.string.destination_movies
        CatalogSection.Series -> R.string.destination_series
    }
    val icon = when (section) {
        CatalogSection.Live -> R.drawable.ic_nav_live
        CatalogSection.Movies -> R.drawable.ic_nav_movies
        CatalogSection.Series -> R.drawable.ic_nav_series
    }
    val accent = when (section) {
        CatalogSection.Live -> MaterialTheme.colorScheme.primary
        CatalogSection.Movies -> MaterialTheme.colorScheme.tertiary
        CatalogSection.Series -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = if (stacked) 132.dp else 72.dp)
            .onFocusChanged { focused = it.isFocused }
            .testTag("home-browse-${section.name.lowercase()}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(if (focused) 3.dp else 1.dp,
            if (focused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant),
    ) {
        if (stacked) Column(
            Modifier.background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.08f),
                MaterialTheme.colorScheme.surfaceContainer))).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(44.dp).clip(MaterialTheme.shapes.small).background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
            }
            Text(stringResource(label), style = MaterialTheme.typography.titleSmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, minLines = 2, maxLines = 3)
        } else Row(Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(painterResource(icon), contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
            Text(stringResource(label), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
    }
}
