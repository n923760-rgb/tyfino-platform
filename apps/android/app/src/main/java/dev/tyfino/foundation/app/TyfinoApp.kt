package dev.tyfino.foundation.app

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.tyfino.foundation.BuildConfig
import dev.tyfino.foundation.licensing.AndroidLicenseClock
import dev.tyfino.foundation.licensing.HttpLicensingApi
import dev.tyfino.foundation.licensing.LicensingController
import dev.tyfino.foundation.licensing.LicensingRepository
import dev.tyfino.foundation.licensing.LicensingUiState
import dev.tyfino.foundation.licensing.SecureLicensingStore
import dev.tyfino.foundation.playback.EpisodePlaybackSelection
import dev.tyfino.foundation.playback.CatalogHistoryRepository
import dev.tyfino.foundation.playback.SQLiteCatalogHistoryStore
import dev.tyfino.foundation.playback.EpisodeResumeRepository
import dev.tyfino.foundation.playback.SeriesContinueWatchingItem
import dev.tyfino.foundation.playback.SQLiteEpisodeResumeStore
import dev.tyfino.foundation.playback.MovieResumeRepository
import dev.tyfino.foundation.playback.PreviousLiveChannelController
import dev.tyfino.foundation.playback.PlaybackSelection
import dev.tyfino.foundation.playback.SQLiteMovieResumeStore
import dev.tyfino.foundation.ui.screen.EpisodePlaybackScreen
import dev.tyfino.foundation.ui.screen.PlaybackScreen
import dev.tyfino.foundation.ui.screen.CatalogScreen
import dev.tyfino.foundation.ui.screen.FoundationScreen
import dev.tyfino.foundation.ui.screen.LicensingScreen
import dev.tyfino.foundation.ui.screen.SettingsScreen
import dev.tyfino.foundation.ui.screen.SeriesDetailsScreen
import dev.tyfino.foundation.ui.screen.SeriesSelection
import dev.tyfino.foundation.ui.screen.XtreamLoginScreen
import dev.tyfino.foundation.xtream.CatalogItem
import dev.tyfino.foundation.xtream.CatalogFavoritesRepository
import dev.tyfino.foundation.xtream.SQLiteFavoriteStore
import dev.tyfino.foundation.xtream.CatalogRepository
import dev.tyfino.foundation.xtream.CatalogSection
import dev.tyfino.foundation.xtream.HttpLiveEpgApi
import dev.tyfino.foundation.xtream.LiveEpgRepository
import dev.tyfino.foundation.xtream.SQLiteLiveEpgStore
import dev.tyfino.foundation.xtream.HttpXtreamApi
import dev.tyfino.foundation.xtream.HttpXtreamCatalogApi
import dev.tyfino.foundation.xtream.HttpXtreamSeriesApi
import dev.tyfino.foundation.xtream.SQLiteCatalogStore
import dev.tyfino.foundation.xtream.SQLiteSeriesStore
import dev.tyfino.foundation.xtream.SeriesDetailsRepository
import dev.tyfino.foundation.xtream.SeriesEpisode
import dev.tyfino.foundation.xtream.SecureXtreamAccountStore
import dev.tyfino.foundation.xtream.XtreamAccountStore
import dev.tyfino.foundation.xtream.XtreamController
import dev.tyfino.foundation.xtream.XtreamRepository
import dev.tyfino.foundation.xtream.XtreamUiState
import kotlinx.coroutines.launch

@Composable
internal fun TyfinoApp() {
    val context = LocalContext.current.applicationContext
    val controller = remember {
        LicensingController(
            LicensingRepository(
                store = SecureLicensingStore(context),
                api = HttpLicensingApi(BuildConfig.LICENSING_API_BASE_URL),
                clock = AndroidLicenseClock(context),
                appVersion = BuildConfig.VERSION_NAME,
            ),
        )
    }
    val xtreamStore = remember { SecureXtreamAccountStore(context) }
    val xtreamController = remember {
        XtreamController(
            XtreamRepository(
                store = xtreamStore,
                api = HttpXtreamApi(context),
            ),
        )
    }
    val catalogRepository = remember {
        CatalogRepository(
            accountStore = xtreamStore,
            api = HttpXtreamCatalogApi(context),
            store = SQLiteCatalogStore(context),
        )
    }
    val epgRepository = remember { LiveEpgRepository(xtreamStore, HttpLiveEpgApi(context), SQLiteLiveEpgStore(context)) }
    val favoritesRepository = remember {
        CatalogFavoritesRepository(xtreamStore, catalogRepository, SQLiteFavoriteStore(context))
    }
    val historyRepository = remember {
        CatalogHistoryRepository(xtreamStore, catalogRepository, SQLiteCatalogHistoryStore(context))
    }
    val seriesStore = remember { SQLiteSeriesStore(context) }
    val seriesDetailsRepository = remember {
        SeriesDetailsRepository(
            accountStore = xtreamStore,
            api = HttpXtreamSeriesApi(context),
            store = seriesStore,
        )
    }
    val movieResumeRepository = remember {
        MovieResumeRepository(
            accountStore = xtreamStore,
            store = SQLiteMovieResumeStore(context),
        )
    }
    val episodeResumeRepository = remember {
        EpisodeResumeRepository(xtreamStore, SQLiteEpisodeResumeStore(context), seriesStore, seriesDetailsRepository)
    }
    val previousLiveChannelController = remember { PreviousLiveChannelController() }
    var licensingState by remember { mutableStateOf(controller.state) }
    val scope = rememberCoroutineScope()
    val publish: (LicensingUiState) -> Unit = { licensingState = it }

    LaunchedEffect(controller) { controller.initialize(publish) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) scope.launch { controller.onForeground(publish) }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (licensingState is LicensingUiState.Active) {
        XtreamGate(
            controller = xtreamController,
            catalogRepository = catalogRepository,
            favoritesRepository = favoritesRepository,
            historyRepository = historyRepository,
            epgRepository = epgRepository,
            seriesDetailsRepository = seriesDetailsRepository,
            movieResumeRepository = movieResumeRepository,
            episodeResumeRepository = episodeResumeRepository,
            previousLiveChannelController = previousLiveChannelController,
            accountStore = xtreamStore,
        )
    } else {
        LicensingScreen(
            state = licensingState,
            onStartTrial = { scope.launch { controller.startTrial(publish) } },
            onShowActivation = { controller.showActivation(publish) },
            onBack = { controller.showChoice(publish) },
            onActivate = { code -> scope.launch { controller.activate(code, publish) } },
            onRetry = { scope.launch { controller.retry(publish) } },
        )
    }
}

@Composable
private fun XtreamGate(
    controller: XtreamController,
    catalogRepository: CatalogRepository,
    favoritesRepository: CatalogFavoritesRepository,
    historyRepository: CatalogHistoryRepository,
    epgRepository: LiveEpgRepository,
    seriesDetailsRepository: SeriesDetailsRepository,
    movieResumeRepository: MovieResumeRepository,
    episodeResumeRepository: EpisodeResumeRepository,
    previousLiveChannelController: PreviousLiveChannelController,
    accountStore: XtreamAccountStore,
) {
    var state by remember { mutableStateOf(controller.state) }
    val scope = rememberCoroutineScope()
    val publish: (XtreamUiState) -> Unit = { state = it }

    LaunchedEffect(controller) { controller.initialize(publish) }
    DisposableEffect(controller) {
        onDispose { controller.deactivate() }
    }

    if (state is XtreamUiState.SignedIn) {
        LicensedAppShell(
            catalogRepository = catalogRepository,
            favoritesRepository = favoritesRepository,
            historyRepository = historyRepository,
            epgRepository = epgRepository,
            seriesDetailsRepository = seriesDetailsRepository,
            movieResumeRepository = movieResumeRepository,
            episodeResumeRepository = episodeResumeRepository,
            previousLiveChannelController = previousLiveChannelController,
            accountStore = accountStore,
            onRemoveXtreamAccount = {
                previousLiveChannelController.clear()
                scope.launch {
                    try {
                        epgRepository.clearActiveAccount()
                    } finally {
                    try {
                        historyRepository.clearActiveAccount()
                    } finally {
                    try {
                        favoritesRepository.clearActiveAccount()
                    } finally {
                    try {
                        episodeResumeRepository.clearActiveAccount()
                    } finally {
                    try {
                        seriesDetailsRepository.clearActiveAccount()
                    } finally {
                        try {
                            movieResumeRepository.clearActiveAccount()
                        } finally {
                            try {
                                catalogRepository.clearActiveAccount()
                            } finally {
                                controller.logout(publish)
                            }
                        }
                    }
                    }
                    }
                    }
                    }
                }
            },
        )
    } else {
        XtreamLoginScreen(
            state = state,
            onSignIn = { input -> scope.launch { controller.signIn(input, publish) } },
            onConfirmCleartext = {
                scope.launch { controller.confirmCleartext(publish) }
            },
            onCancelCleartext = { controller.cancelCleartext(publish) },
        )
    }
}

@Composable
private fun LicensedAppShell(
    catalogRepository: CatalogRepository,
    favoritesRepository: CatalogFavoritesRepository,
    historyRepository: CatalogHistoryRepository,
    epgRepository: LiveEpgRepository,
    seriesDetailsRepository: SeriesDetailsRepository,
    movieResumeRepository: MovieResumeRepository,
    episodeResumeRepository: EpisodeResumeRepository,
    previousLiveChannelController: PreviousLiveChannelController,
    accountStore: XtreamAccountStore,
    onRemoveXtreamAccount: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val navigationType = navigationTypeFor(windowSizeClass)
    val scope = rememberCoroutineScope()
    var playbackSelection by remember { mutableStateOf<PlaybackSelection?>(null) }
    var seriesSelection by remember { mutableStateOf<SeriesSelection?>(null) }
    var episodePlaybackSelection by remember { mutableStateOf<EpisodePlaybackSelection?>(null) }

    val navigateTo: (AppDestination) -> Unit = { destination ->
        if (currentDestination?.route != destination.route) {
            navController.navigate(destination.route) {
                launchSingleTop = true
                restoreState = true
                popUpTo(AppDestination.Foundation.route) {
                    saveState = true
                }
            }
        }
    }
    val play: (CatalogSection, CatalogItem) -> Unit = { section, item ->
        accountStore.load()?.let { account ->
            playbackSelection = PlaybackSelection.from(account, section, item)
            navController.navigate(PLAYBACK_ROUTE) { launchSingleTop = true }
        }
    }
    val openSeries: (CatalogItem) -> Unit = { item ->
        accountStore.load()?.let { account ->
            seriesSelection = SeriesSelection(account.accountId, account.generation, item)
            navController.navigate(SERIES_DETAILS_ROUTE) { launchSingleTop = true }
        }
    }
    val playEpisode: (SeriesEpisode, Long) -> Unit = { episode, generation ->
        val series = seriesSelection
        val account = accountStore.load()
        if (series != null && account?.accountId == series.accountId &&
            account.generation == series.accountGeneration &&
            episode.providerSeriesId == series.item.providerId
        ) {
            val chosen = EpisodePlaybackSelection.from(
                account.accountId, account.generation, series.item.providerId, generation, episode,
            )
            if (chosen != null) {
                episodePlaybackSelection = chosen
                navController.navigate(PLAYBACK_ROUTE) { launchSingleTop = true }
            }
        }
    }
    val resumeEpisode: (SeriesContinueWatchingItem) -> Unit = { item ->
        val account = accountStore.load()
        if (account?.accountId == item.episode.accountId && account.generation == item.accountGeneration) {
            val chosen = EpisodePlaybackSelection.from(
                account.accountId, account.generation, item.episode.providerSeriesId,
                item.seriesGeneration, item.episode,
            )
            if (chosen != null) {
                episodePlaybackSelection = chosen
                navController.navigate(PLAYBACK_ROUTE) { launchSingleTop = true }
            }
        }
    }
    val playPreviousLive: (PlaybackSelection) -> Unit = { source ->
        val request = previousLiveChannelController.beginPrevious(source)
        if (request != null) {
            scope.launch {
                val target = catalogRepository.cachedItems(
                    section = CatalogSection.Live,
                    providerItemIds = setOf(request.targetProviderItemId),
                ).singleOrNull { it.providerId == request.targetProviderItemId }
                val account = accountStore.load()
                if (
                    target != null &&
                    playbackSelection == source &&
                    account?.accountId == request.accountId &&
                    account.generation == request.accountGeneration &&
                    previousLiveChannelController.isCurrent(request)
                ) {
                    playbackSelection = PlaybackSelection.from(account, CatalogSection.Live, target)
                }
            }
        }
    }
    val navHost: @Composable (Modifier) -> Unit = { modifier ->
        AppNavHost(
            navController = navController,
            modifier = modifier,
            catalogRepository = catalogRepository,
            favoritesRepository = favoritesRepository,
            historyRepository = historyRepository,
            epgRepository = epgRepository,
            seriesDetailsRepository = seriesDetailsRepository,
            movieResumeRepository = movieResumeRepository,
            episodeResumeRepository = episodeResumeRepository,
            previousLiveChannelController = previousLiveChannelController,
            accountStore = accountStore,
            seriesSelection = seriesSelection,
            episodePlaybackSelection = episodePlaybackSelection,
            playbackSelection = playbackSelection,
            onPlay = play,
            onOpenSeries = openSeries,
            onPlayEpisode = playEpisode,
            onResumeEpisode = resumeEpisode,
            onPreviousLive = playPreviousLive,
            onOpenSettings = { navigateTo(AppDestination.Settings) },
            onPlaybackClosed = {
                playbackSelection = null
                episodePlaybackSelection = null
            },
            onRemoveXtreamAccount = onRemoveXtreamAccount,
        )
    }

    if (currentDestination?.route == PLAYBACK_ROUTE) {
        navHost(Modifier.fillMaxSize())
    } else if (navigationType == AppNavigationType.BottomBar) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("app-shell"),
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                AppBottomBar(
                    selectedRoute = currentDestination?.route,
                    onDestinationSelected = navigateTo,
                )
            },
        ) { contentPadding ->
            navHost(Modifier.padding(contentPadding))
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .testTag("app-shell"),
        ) {
            AppNavigationRail(
                selectedRoute = currentDestination?.route,
                onDestinationSelected = navigateTo,
            )
            navHost(
                Modifier
                    .weight(1f)
                    .padding(WindowInsets.safeDrawing.asPaddingValues()),
            )
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier,
    catalogRepository: CatalogRepository,
    favoritesRepository: CatalogFavoritesRepository,
    historyRepository: CatalogHistoryRepository,
    epgRepository: LiveEpgRepository,
    seriesDetailsRepository: SeriesDetailsRepository,
    movieResumeRepository: MovieResumeRepository,
    episodeResumeRepository: EpisodeResumeRepository,
    previousLiveChannelController: PreviousLiveChannelController,
    accountStore: XtreamAccountStore,
    seriesSelection: SeriesSelection?,
    episodePlaybackSelection: EpisodePlaybackSelection?,
    playbackSelection: PlaybackSelection?,
    onPlay: (CatalogSection, CatalogItem) -> Unit,
    onOpenSeries: (CatalogItem) -> Unit,
    onPlayEpisode: (SeriesEpisode, Long) -> Unit,
    onResumeEpisode: (SeriesContinueWatchingItem) -> Unit,
    onPreviousLive: (PlaybackSelection) -> Unit,
    onOpenSettings: () -> Unit,
    onPlaybackClosed: () -> Unit,
    onRemoveXtreamAccount: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Foundation.route,
        modifier = modifier,
    ) {
        composable(AppDestination.Foundation.route) {
            FoundationScreen(onOpenSettings = onOpenSettings)
        }
        composable(AppDestination.Live.route) {
            CatalogScreen(
                section = CatalogSection.Live,
                repository = catalogRepository,
                resumeRepository = movieResumeRepository,
                favoritesRepository = favoritesRepository,
                historyRepository = historyRepository,
                onPlay = { item -> onPlay(CatalogSection.Live, item) },
            )
        }
        composable(AppDestination.Movies.route) {
            CatalogScreen(
                section = CatalogSection.Movies,
                repository = catalogRepository,
                resumeRepository = movieResumeRepository,
                favoritesRepository = favoritesRepository,
                historyRepository = historyRepository,
                onPlay = { item -> onPlay(CatalogSection.Movies, item) },
            )
        }
        composable(AppDestination.Series.route) {
            CatalogScreen(
                section = CatalogSection.Series,
                repository = catalogRepository,
                resumeRepository = movieResumeRepository,
                favoritesRepository = favoritesRepository,
                onPlay = onOpenSeries,
                episodeResumeRepository = episodeResumeRepository,
                onResumeEpisode = onResumeEpisode,
            )
        }
        composable(SERIES_DETAILS_ROUTE) {
            val selection = seriesSelection
            if (selection == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                SeriesDetailsScreen(
                    selection = selection,
                    repository = seriesDetailsRepository,
                    onBack = { navController.popBackStack() },
                    onEpisode = onPlayEpisode,
                )
            }
        }
        composable(AppDestination.Settings.route) {
            SettingsScreen(onRemoveXtreamAccount = onRemoveXtreamAccount)
        }
        composable(PLAYBACK_ROUTE) {
            val episode = episodePlaybackSelection
            val selection = playbackSelection
            if (episode == null && selection == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                DisposableEffect(Unit) { onDispose(onPlaybackClosed) }
                if (episode != null) {
                    EpisodePlaybackScreen(
                        selection = episode,
                        accountStore = accountStore,
                        seriesRepository = seriesDetailsRepository,
                        resumeRepository = movieResumeRepository,
                        episodeResumeRepository = episodeResumeRepository,
                        previousLiveChannelController = previousLiveChannelController,
                        onBack = { navController.popBackStack() },
                    )
                } else if (selection != null) {
                    PlaybackScreen(
                        selection = selection,
                        accountStore = accountStore,
                        resumeRepository = movieResumeRepository,
                        historyRepository = historyRepository,
                        liveEpgRepository = epgRepository,
                        previousLiveChannelController = previousLiveChannelController,
                        onPreviousLive = { onPreviousLive(selection) },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

private const val PLAYBACK_ROUTE = "playback"
private const val SERIES_DETAILS_ROUTE = "series-details"

@Composable
private fun AppBottomBar(
    selectedRoute: String?,
    onDestinationSelected: (AppDestination) -> Unit,
) {
    NavigationBar {
        AppDestination.entries.forEach { destination ->
            val label = stringResource(destination.labelRes)
            NavigationBarItem(
                modifier = Modifier.testTag("destination-${destination.route}"),
                selected = selectedRoute == destination.route,
                onClick = { onDestinationSelected(destination) },
                icon = { Text(label.take(1)) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun AppNavigationRail(
    selectedRoute: String?,
    onDestinationSelected: (AppDestination) -> Unit,
) {
    NavigationRail(windowInsets = WindowInsets.safeDrawing) {
        AppDestination.entries.forEach { destination ->
            val label = stringResource(destination.labelRes)
            NavigationRailItem(
                modifier = Modifier.testTag("destination-${destination.route}"),
                selected = selectedRoute == destination.route,
                onClick = { onDestinationSelected(destination) },
                icon = { Text(label.take(1)) },
                label = { Text(label) },
            )
        }
    }
}
