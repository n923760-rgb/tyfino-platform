package dev.tyfino.foundation.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import dev.tyfino.foundation.R
import dev.tyfino.foundation.playback.ContinueWatchingItem
import dev.tyfino.foundation.playback.CatalogHistoryListResult
import dev.tyfino.foundation.playback.CatalogHistoryRepository
import dev.tyfino.foundation.playback.EpisodeResumeListResult
import dev.tyfino.foundation.playback.EpisodeResumeRepository
import dev.tyfino.foundation.playback.EpisodeHistoryListResult
import dev.tyfino.foundation.playback.EpisodeHistoryRepository
import dev.tyfino.foundation.playback.SeriesHistoryItem
import dev.tyfino.foundation.playback.SeriesContinueWatchingItem
import dev.tyfino.foundation.playback.MovieResumeListResult
import dev.tyfino.foundation.playback.MovieResumePresentation
import dev.tyfino.foundation.playback.MovieResumeRepository
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.ui.components.FocusIconButton
import dev.tyfino.foundation.ui.components.rememberInitialFocusRequester
import dev.tyfino.foundation.xtream.CatalogCategory
import dev.tyfino.foundation.xtream.CatalogFailure
import dev.tyfino.foundation.xtream.CatalogFavoritesRepository
import dev.tyfino.foundation.xtream.FavoritesListResult
import dev.tyfino.foundation.xtream.FavoriteToggleResult
import dev.tyfino.foundation.xtream.CatalogItem
import dev.tyfino.foundation.xtream.CatalogRepository
import dev.tyfino.foundation.xtream.CatalogSection
import dev.tyfino.foundation.xtream.CatalogSearchResult
import dev.tyfino.foundation.xtream.CatalogState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun CatalogScreen(
    section: CatalogSection,
    repository: CatalogRepository,
    resumeRepository: MovieResumeRepository,
    onPlay: (CatalogItem) -> Unit,
    episodeResumeRepository: EpisodeResumeRepository? = null,
    episodeHistoryRepository: EpisodeHistoryRepository? = null,
    onResumeEpisode: (SeriesContinueWatchingItem) -> Unit = {},
    onPlayHistoryEpisode: (SeriesHistoryItem) -> Unit = {},
    favoritesRepository: CatalogFavoritesRepository? = null,
    historyRepository: CatalogHistoryRepository? = null,
) {
    var categories by remember(section) {
        mutableStateOf<CatalogState<CatalogCategory>>(CatalogState.Empty)
    }
    var selectedCategoryId by remember(section) { mutableStateOf<String?>(null) }
    var selectedCategoryName by remember(section) { mutableStateOf<String?>(null) }
    var catalogItems by remember(section) {
        mutableStateOf<CatalogState<CatalogItem>>(CatalogState.Empty)
    }
    var continueWatching by remember(section) { mutableStateOf(emptyList<ContinueWatchingItem>()) }
    var seriesContinueWatching by remember(section) { mutableStateOf(emptyList<SeriesContinueWatchingItem>()) }
    var seriesHistory by remember(section) { mutableStateOf(emptyList<SeriesHistoryItem>()) }
    var searchText by remember(section) { mutableStateOf("") }
    var searchResult by remember(section) { mutableStateOf<CatalogSearchResult?>(null) }
    val searchActive = searchText.trim().codePointCount(0, searchText.trim().length) >= 2
    var favoritesState by remember(section) { mutableStateOf<FavoritesListResult?>(null) }
    var favoritesOnly by remember(section) { mutableStateOf(false) }
    var historyOnly by remember(section) { mutableStateOf(false) }
    var favoriteActionError by remember(section) { mutableStateOf(false) }
    var historyState by remember(section) { mutableStateOf<CatalogHistoryListResult?>(null) }
    val recentItems = (historyState as? CatalogHistoryListResult.Ready)?.items.orEmpty()
    val recentIds = if (section == CatalogSection.Series) {
        seriesHistory.mapTo(hashSetOf()) { it.episode.providerSeriesId }
    } else {
        recentItems.mapTo(hashSetOf()) { it.providerId }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val favoriteItems = (favoritesState as? FavoritesListResult.Ready)?.items.orEmpty()
    val favoriteIds = favoriteItems.mapTo(hashSetOf()) { it.providerId }
    val scope = rememberCoroutineScope()
    val initialFocus = rememberInitialFocusRequester(section)
    val toggleFavorite: (CatalogItem) -> Unit = { item ->
        val owner = (favoritesState as? FavoritesListResult.Ready)?.owner
        val selectedRepository = favoritesRepository
        if (owner != null && selectedRepository != null) {
            scope.launch {
                when (selectedRepository.toggle(owner, section, item.providerId)) {
                    is FavoriteToggleResult.Changed -> {
                        favoriteActionError = false
                        favoritesState = selectedRepository.list(section)
                    }
                    FavoriteToggleResult.Failure -> favoriteActionError = true
                }
            }
        }
    }

    LaunchedEffect(repository, section) {
        repository.categories(section, publish = { categories = it })
    }
    LaunchedEffect(repository, section, selectedCategoryId) {
        val categoryId = selectedCategoryId
        if (categoryId == null) {
            catalogItems = CatalogState.Empty
        } else {
            repository.items(section, categoryId, publish = { catalogItems = it })
        }
    }
    LaunchedEffect(repository, resumeRepository, section) {
        continueWatching = if (section == CatalogSection.Movies) {
            when (val result = resumeRepository.continueWatching()) {
                is MovieResumeListResult.Ready -> {
                    val ids = result.records.mapTo(linkedSetOf()) { it.providerItemId }
                    MovieResumePresentation.assemble(
                        records = result.records,
                        currentCatalog = repository.cachedItems(section, ids),
                    )
                }
                is MovieResumeListResult.Failure -> emptyList()
            }
        } else {
            emptyList()
        }
    }

    LaunchedEffect(repository, episodeResumeRepository, section) {
        seriesContinueWatching = if (section == CatalogSection.Series && episodeResumeRepository != null) {
            when (val result = episodeResumeRepository.continueWatching()) {
                is EpisodeResumeListResult.Ready -> {
                    val ids = result.items.mapTo(linkedSetOf()) { it.episode.providerSeriesId }
                    val current = repository.cachedItems(CatalogSection.Series, ids).associateBy { it.providerId }
                    result.items.mapNotNull { item ->
                        current[item.episode.providerSeriesId]?.let {
                            item.copy(seriesTitle = it.name, seriesArtworkUrl = it.artworkUrl)
                        }
                    }
                }
                EpisodeResumeListResult.Failure -> emptyList()
            }
        } else emptyList()
    }

    DisposableEffect(lifecycleOwner, historyRepository, episodeHistoryRepository, section) {
        fun refreshHistory() {
            scope.launch {
                if (historyRepository != null && section != CatalogSection.Series) {
                    historyState = historyRepository.recent(section)
                } else if (episodeHistoryRepository != null && section == CatalogSection.Series) {
                    seriesHistory = when (val result = episodeHistoryRepository.recent()) {
                        is EpisodeHistoryListResult.Ready -> {
                            val ids = result.items.mapTo(linkedSetOf()) { it.episode.providerSeriesId }
                            val current = repository.cachedItems(CatalogSection.Series, ids).associateBy { it.providerId }
                            result.items.map { item ->
                                current[item.episode.providerSeriesId]?.let {
                                    item.copy(seriesTitle = it.name, seriesArtworkUrl = it.artworkUrl)
                                } ?: item
                            }
                        }
                        EpisodeHistoryListResult.Failure -> emptyList()
                    }
                }
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshHistory()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) refreshHistory()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(favoritesRepository, section) {
        favoritesState = favoritesRepository?.list(section)
    }

    LaunchedEffect(repository, section, searchText) {
        searchResult = null
        if (searchActive) {
            delay(250L)
            searchResult = repository.searchCached(section, searchText)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .testTag("catalog-${section.name.lowercase()}"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(section.titleResource()),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            FocusIconButton(
                icon = R.drawable.ic_refresh,
                description = stringResource(R.string.catalog_refresh),
                onClick = {
                    scope.launch {
                        repository.categories(section, forceRefresh = true) { categories = it }
                    }
                },
                modifier = Modifier
                    .focusRequester(initialFocus)
                    .testTag("catalog-initial-focus"),
            )
        }

        CatalogFilterTabs(
            favoritesOnly = favoritesOnly,
            historyOnly = historyOnly,
            onAll = { favoritesOnly = false; historyOnly = false },
            onFavorites = { favoritesOnly = true; historyOnly = false },
            onHistory = { historyOnly = true; favoritesOnly = false },
        )
        OutlinedTextField(
            value = searchText,
            onValueChange = { next ->
                if (next.codePointCount(0, next.length) <= 80) {
                    searchText = next
                    searchResult = null
                }
            },
            label = { Text(stringResource(R.string.catalog_search_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("catalog-search"),
        )
        if (favoritesState == FavoritesListResult.Failure || favoriteActionError) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.catalog_favorite_error), color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                FocusVisibleButton(
                    label = stringResource(R.string.retry),
                    onClick = {
                        scope.launch {
                            favoritesState = favoritesRepository?.list(section)
                            favoriteActionError = false
                        }
                    },
                )
            }
        }
        if (searchText.isNotBlank()) {
            Text(
                stringResource(R.string.catalog_search_scope),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (searchActive) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (val result = searchResult) {
                    null -> CatalogLoadingState()
                    is CatalogSearchResult.Ready -> {
                        val visible = when {
                            favoritesOnly -> result.records.filter { it.providerId in favoriteIds }
                            historyOnly -> result.records.filter { it.providerId in recentIds }
                            else -> result.records
                        }
                        if (visible.isEmpty()) {
                            EmptyState(R.string.catalog_search_empty)
                        } else {
                            ItemGrid(visible, section, onPlay, favoriteIds,
                                if (favoritesRepository != null && favoritesState is FavoritesListResult.Ready) toggleFavorite else null)
                            if (result.limited) {
                                Text(
                                    stringResource(R.string.catalog_search_limit),
                                    modifier = Modifier.align(Alignment.TopCenter),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    CatalogSearchResult.InvalidQuery -> EmptyState(R.string.catalog_search_minimum)
                    CatalogSearchResult.StaleOwner -> EmptyState(R.string.catalog_error_authentication)
                    CatalogSearchResult.LocalStorage -> EmptyState(R.string.catalog_error_storage)
                }
            }
        } else if (historyOnly) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (section == CatalogSection.Series) {
                    if (seriesHistory.isEmpty()) EmptyState(R.string.catalog_history_empty)
                    else SeriesHistoryGrid(seriesHistory, onPlayHistoryEpisode)
                } else if (recentItems.isEmpty()) EmptyState(R.string.catalog_history_empty)
                else ItemGrid(recentItems, section, onPlay, favoriteIds,
                    if (favoritesRepository != null && favoritesState is FavoritesListResult.Ready) toggleFavorite else null)
            }
        } else if (favoritesOnly) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (favoritesState == FavoritesListResult.Failure) EmptyState(R.string.catalog_favorite_error)
                else if (favoriteItems.isEmpty()) EmptyState(R.string.catalog_favorites_empty)
                else ItemGrid(favoriteItems, section, onPlay, favoriteIds, toggleFavorite)
            }
        } else {
        if (continueWatching.isNotEmpty()) {
            ContinueWatchingStrip(continueWatching, onPlay)
        }
        if (seriesContinueWatching.isNotEmpty()) {
            SeriesContinueWatchingStrip(seriesContinueWatching, onResumeEpisode)
        }
        if (seriesHistory.isNotEmpty()) {
            SeriesHistoryStrip(seriesHistory, onPlayHistoryEpisode)
        }

        CategoryStrip(
            state = categories,
            selectedCategoryId = selectedCategoryId,
            onSelect = { category ->
                selectedCategoryId = category.providerId
                selectedCategoryName = category.name
            },
            onRetry = {
                scope.launch { repository.categories(section, forceRefresh = true) { categories = it } }
            },
        )

        selectedCategoryName?.let { name ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                FocusIconButton(
                    icon = R.drawable.ic_refresh,
                    description = stringResource(R.string.catalog_refresh_items),
                    onClick = {
                        val categoryId = selectedCategoryId ?: return@FocusIconButton
                        scope.launch {
                            repository.items(section, categoryId, forceRefresh = true) {
                                catalogItems = it
                            }
                        }
                    },
                )
            }
        }

        ItemContent(
            state = catalogItems,
            section = section,
            hasSelection = selectedCategoryId != null,
            onPlay = onPlay,
            onRetry = {
                val categoryId = selectedCategoryId ?: return@ItemContent
                scope.launch {
                    repository.items(section, categoryId, forceRefresh = true) { catalogItems = it }
                }
            },
            modifier = Modifier.weight(1f),
            favoriteIds = favoriteIds,
            onToggleFavorite = if (favoritesRepository != null && favoritesState is FavoritesListResult.Ready) toggleFavorite else null,
        )
        }
    }
}

@Composable
internal fun CatalogFilterTabs(
    favoritesOnly: Boolean,
    historyOnly: Boolean,
    onAll: () -> Unit,
    onFavorites: () -> Unit,
    onHistory: () -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().focusGroup().testTag("catalog-filters"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            CatalogFilterButton(
                label = stringResource(R.string.catalog_show_all),
                selected = !favoritesOnly && !historyOnly,
                onClick = onAll,
                modifier = Modifier.testTag("catalog-all-filter"),
            )
        }
        item {
            CatalogFilterButton(
                label = stringResource(R.string.catalog_favorites_only),
                selected = favoritesOnly,
                onClick = onFavorites,
                modifier = Modifier.testTag("catalog-favorites-filter"),
            )
        }
        item {
            CatalogFilterButton(
                label = stringResource(R.string.catalog_recent_title),
                selected = historyOnly,
                onClick = onHistory,
                modifier = Modifier.testTag("catalog-history-filter"),
            )
        }
    }
}

@Composable
private fun ContinueWatchingStrip(
    records: List<ContinueWatchingItem>,
    onPlay: (CatalogItem) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("continue-watching"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.continue_watching_title),
            style = MaterialTheme.typography.titleLarge,
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth().focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(records, key = { it.catalogItem.providerId }) { record ->
                CatalogTile(
                    label = record.catalogItem.name,
                    supporting = record.progressPercent?.let {
                        stringResource(R.string.continue_watching_progress, it)
                    } ?: stringResource(R.string.continue_watching_resume),
                    selected = false,
                    onClick = { onPlay(record.catalogItem) },
                    modifier = Modifier.widthIn(min = 136.dp, max = 180.dp),
                    showArtwork = true,
                    artworkUrl = record.catalogItem.artworkUrl,
                    artworkAspectRatio = POSTER_ASPECT_RATIO,
                )
            }
        }
    }
}

@Composable
private fun SeriesContinueWatchingStrip(
    records: List<SeriesContinueWatchingItem>,
    onPlay: (SeriesContinueWatchingItem) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("series-continue-watching"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.continue_watching_title), style = MaterialTheme.typography.titleLarge)
        LazyRow(
            modifier = Modifier.fillMaxWidth().focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(records, key = { "${it.episode.providerSeriesId}:${it.episode.providerEpisodeId}" }) { item ->
                CatalogTile(
                    label = listOfNotNull(item.seriesTitle, item.episode.title).filter { it.isNotBlank() }.joinToString(" • "),
                    supporting = item.progressPercent?.let { stringResource(R.string.continue_watching_progress, it) }
                        ?: stringResource(R.string.continue_watching_resume),
                    selected = false,
                    onClick = { onPlay(item) },
                    modifier = Modifier.widthIn(min = 136.dp, max = 180.dp),
                    showArtwork = true,
                    artworkUrl = item.seriesArtworkUrl,
                    artworkAspectRatio = POSTER_ASPECT_RATIO,
                )
            }
        }
    }
}

@Composable
internal fun SeriesHistoryStrip(
    records: List<SeriesHistoryItem>,
    onPlay: (SeriesHistoryItem) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("series-history"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.series_history_title), style = MaterialTheme.typography.titleLarge)
        LazyRow(
            modifier = Modifier.fillMaxWidth().focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(records, key = { "${it.episode.providerSeriesId}:${it.episode.providerEpisodeId}" }) { item ->
                val episodeLabel = item.episode.title?.takeIf(String::isNotBlank)
                    ?: item.episode.episodeNumber?.let { stringResource(R.string.series_episode_number, it) }
                    ?: stringResource(R.string.series_episode_order, item.episode.providerOrder + 1)
                CatalogTile(
                    label = listOf(item.seriesTitle, episodeLabel).filter(String::isNotBlank).joinToString(" • "),
                    supporting = stringResource(R.string.series_history_play),
                    selected = false,
                    onClick = { onPlay(item) },
                    modifier = Modifier.widthIn(min = 136.dp, max = 180.dp),
                    showArtwork = true,
                    artworkUrl = item.seriesArtworkUrl,
                    artworkAspectRatio = POSTER_ASPECT_RATIO,
                )
            }
        }
    }
}

@Composable
private fun CategoryStrip(
    state: CatalogState<CatalogCategory>,
    selectedCategoryId: String?,
    onSelect: (CatalogCategory) -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        CatalogState.Empty, CatalogState.Loading -> CatalogLoadingState()
        is CatalogState.Error -> CatalogErrorState(state.failure, onRetry)
        is CatalogState.EmptyContent -> EmptyState(R.string.catalog_no_categories)
        is CatalogState.Content -> {
            if (state.isRefreshing) CatalogRefreshingNotice()
            Categories(state.records, selectedCategoryId, onSelect)
        }
        is CatalogState.StaleContent -> {
            CatalogStaleNotice(state.failure)
            Categories(state.records, selectedCategoryId, onSelect)
        }
    }
}

@Composable
private fun Categories(
    categories: List<CatalogCategory>,
    selectedCategoryId: String?,
    onSelect: (CatalogCategory) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().focusGroup().testTag("catalog-categories"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(categories, key = { it.providerId }) { category ->
            CatalogTile(
                label = category.name,
                selected = category.providerId == selectedCategoryId,
                onClick = { onSelect(category) },
                modifier = Modifier.widthIn(min = 120.dp, max = 172.dp),
                exposeSelectionState = true,
                compact = true,
            )
        }
    }
}

@Composable
private fun ItemContent(
    state: CatalogState<CatalogItem>,
    section: CatalogSection,
    hasSelection: Boolean,
    onPlay: (CatalogItem) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier,
    favoriteIds: Set<String> = emptySet(),
    onToggleFavorite: ((CatalogItem) -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        when (state) {
            CatalogState.Empty -> EmptyState(
                if (hasSelection) R.string.catalog_loading else R.string.catalog_choose_category,
            )
            CatalogState.Loading -> CatalogLoadingState()
            is CatalogState.Error -> CatalogErrorState(state.failure, onRetry)
            is CatalogState.EmptyContent -> EmptyState(R.string.catalog_no_items)
            is CatalogState.Content -> {
                ItemGrid(state.records, section, onPlay, favoriteIds, onToggleFavorite)
                if (state.isRefreshing) CatalogRefreshingNotice(Modifier.align(Alignment.TopCenter))
            }
            is CatalogState.StaleContent -> {
                ItemGrid(state.records, section, onPlay, favoriteIds, onToggleFavorite)
                CatalogStaleNotice(state.failure, Modifier.align(Alignment.TopCenter))
            }
        }
    }
}

@Composable
private fun ItemGrid(
    records: List<CatalogItem>,
    section: CatalogSection,
    onPlay: (CatalogItem) -> Unit,
    favoriteIds: Set<String> = emptySet(),
    onToggleFavorite: ((CatalogItem) -> Unit)? = null,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(catalogGridColumns(maxWidth)),
            modifier = Modifier.fillMaxSize().focusGroup().testTag("catalog-items"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(records, key = { it.providerId }) { item ->
                Box {
                    CatalogTile(
                        label = item.name,
                        supporting = listOfNotNull(item.releaseYear, item.rating).joinToString(" • "),
                        selected = false,
                        onClick = { onPlay(item) },
                        modifier = Modifier.fillMaxWidth(),
                        showArtwork = true,
                        artworkUrl = item.artworkUrl,
                        artworkAspectRatio = section.artworkAspectRatio(),
                    )
                    if (onToggleFavorite != null) {
                        val isFavorite = item.providerId in favoriteIds
                        CatalogFavoriteButton(
                            label = stringResource(
                                if (isFavorite) R.string.catalog_favorite_remove else R.string.catalog_favorite_add,
                                item.name,
                            ),
                            selected = isFavorite,
                            onClick = { onToggleFavorite(item) },
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).testTag("catalog-favorite-toggle"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesHistoryGrid(records: List<SeriesHistoryItem>, onPlay: (SeriesHistoryItem) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(catalogGridColumns(maxWidth)),
            modifier = Modifier.fillMaxSize().focusGroup().testTag("catalog-items"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(records, key = { "${it.episode.providerSeriesId}:${it.episode.providerEpisodeId}" }) { item ->
                val episodeLabel = item.episode.title?.takeIf(String::isNotBlank)
                    ?: item.episode.episodeNumber?.let { stringResource(R.string.series_episode_number, it) }
                    ?: stringResource(R.string.series_episode_order, item.episode.providerOrder + 1)
                CatalogTile(
                    label = listOf(item.seriesTitle, episodeLabel).filter(String::isNotBlank).joinToString(" • "),
                    supporting = stringResource(R.string.series_history_play),
                    selected = false,
                    onClick = { onPlay(item) },
                    modifier = Modifier.fillMaxWidth(),
                    showArtwork = true,
                    artworkUrl = item.seriesArtworkUrl,
                    artworkAspectRatio = POSTER_ASPECT_RATIO,
                )
            }
        }
    }
}

internal fun catalogGridColumns(width: Dp): Int {
    return when {
        width >= 900.dp -> 6
        width >= 600.dp -> 5
        width >= 480.dp -> 4
        else -> 3
    }
}

@Composable
internal fun CatalogTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    supporting: String = "",
    enabled: Boolean = true,
    exposeSelectionState: Boolean = false,
    showArtwork: Boolean = false,
    artworkUrl: String? = null,
    artworkAspectRatio: Float = LANDSCAPE_ARTWORK_ASPECT_RATIO,
    compact: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val monogram = remember(label) {
        val words = label.trim().split(Regex("\\s+")).filter(String::isNotEmpty)
        if (words.size >= 2) words.take(2).joinToString("") { it.take(1) } else label.trim().take(2).ifEmpty { "•" }
    }
    val artworkRequest = remember(context, artworkUrl) {
        artworkUrl?.let { ImageRequest.Builder(context).data(it).crossfade(true).build() }
    }
    val tintIndex = (label.hashCode().ushr(1) % 3)
    val tint = when (tintIndex) {
        0 -> MaterialTheme.colorScheme.primaryContainer
        1 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val onTint = when (tintIndex) {
        0 -> MaterialTheme.colorScheme.onPrimaryContainer
        1 -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = if (compact) 52.dp else 88.dp)
            .onFocusChanged { focused = it.isFocused }
            .semantics {
                contentDescription = listOf(label, supporting)
                    .filter(String::isNotBlank)
                    .joinToString(", ")
                if (exposeSelectionState) this.selected = selected
            },
        border = BorderStroke(
            if (focused) 3.dp else 1.dp,
            when {
                focused -> MaterialTheme.colorScheme.onSurface
                selected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            },
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(if (showArtwork) 6.dp else if (compact) 10.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (showArtwork) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(artworkAspectRatio)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.linearGradient(listOf(tint, MaterialTheme.colorScheme.surfaceVariant)))
                        .testTag("catalog-artwork-placeholder"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = monogram,
                        style = MaterialTheme.typography.titleLarge,
                        color = onTint,
                    )
                    if (artworkRequest != null) {
                        AsyncImage(
                            model = artworkRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("catalog-artwork"),
                        )
                    }
                }
            }
            Text(
                text = label,
                style = if (compact || showArtwork) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                modifier = if (showArtwork) Modifier.heightIn(min = 38.dp) else Modifier,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (supporting.isNotEmpty() || showArtwork) {
                Text(
                    text = supporting.ifEmpty { " " },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun CatalogFilterButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.onFocusChanged { focused = it.isFocused }.semantics { this.selected = selected },
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused || selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) { Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1) }
}

@Composable
internal fun CatalogFavoriteButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusIconButton(
        icon = if (selected) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_outline,
        description = label,
        onClick = onClick,
        modifier = modifier.semantics { this.selected = selected },
        selected = selected,
    )
}

@Composable
internal fun CatalogLoadingState() {
    Row(
        modifier = Modifier
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag("catalog-loading"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.catalog_loading))
    }
}

@Composable
private fun EmptyState(@StringRes message: Int) {
    Text(
        text = stringResource(message),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun CatalogErrorState(failure: CatalogFailure, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(failure.messageResource()),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .semantics { liveRegion = LiveRegionMode.Assertive }
                .testTag("catalog-error"),
        )
        FocusVisibleButton(label = stringResource(R.string.retry), onClick = onRetry)
    }
}

@Composable
internal fun CatalogRefreshingNotice(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.catalog_refreshing),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag("catalog-refreshing"),
    )
}

@Composable
internal fun CatalogStaleNotice(failure: CatalogFailure, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.catalog_stale, stringResource(failure.messageResource())),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier
            .semantics { liveRegion = LiveRegionMode.Assertive }
            .testTag("catalog-stale"),
    )
}

@StringRes
private fun CatalogSection.titleResource(): Int = when (this) {
    CatalogSection.Live -> R.string.catalog_live_title
    CatalogSection.Movies -> R.string.catalog_movies_title
    CatalogSection.Series -> R.string.catalog_series_title
}

private fun CatalogSection.artworkAspectRatio(): Float = when (this) {
    CatalogSection.Live -> LANDSCAPE_ARTWORK_ASPECT_RATIO
    CatalogSection.Movies, CatalogSection.Series -> POSTER_ASPECT_RATIO
}

private const val LANDSCAPE_ARTWORK_ASPECT_RATIO = 16f / 9f
private const val POSTER_ASPECT_RATIO = 2f / 3f

@StringRes
private fun CatalogFailure.messageResource(): Int = when (this) {
    CatalogFailure.NetworkUnavailable -> R.string.catalog_error_network
    CatalogFailure.Timeout -> R.string.catalog_error_timeout
    CatalogFailure.ProviderUnavailable -> R.string.catalog_error_provider
    CatalogFailure.AuthenticationRejected -> R.string.catalog_error_authentication
    CatalogFailure.MalformedResponse -> R.string.catalog_error_malformed
    CatalogFailure.UnsupportedResponse -> R.string.catalog_error_unsupported
    CatalogFailure.ResponseTooLarge -> R.string.catalog_error_too_large
    CatalogFailure.LocalStorage -> R.string.catalog_error_storage
    CatalogFailure.Unknown -> R.string.catalog_error_unknown
}
