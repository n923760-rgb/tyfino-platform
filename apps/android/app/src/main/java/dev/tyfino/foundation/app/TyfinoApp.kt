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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import dev.tyfino.foundation.ui.screen.MovieDetailsScreen
import dev.tyfino.foundation.ui.screen.MovieSelection
import dev.tyfino.foundation.ui.screen.SettingsScreen
import dev.tyfino.foundation.ui.screen.SeriesDetailsScreen
import dev.tyfino.foundation.ui.screen.SeriesSelection
import dev.tyfino.foundation.ui.screen.XtreamAccountSwitcher
import dev.tyfino.foundation.ui.screen.XtreamAccountManager
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
import dev.tyfino.foundation.xtream.HttpXtreamMovieDetailsApi
import dev.tyfino.foundation.xtream.HttpXtreamSeriesApi
import dev.tyfino.foundation.xtream.SQLiteCatalogStore
import dev.tyfino.foundation.xtream.SQLiteMovieDetailsStore
import dev.tyfino.foundation.xtream.SQLiteSeriesStore
import dev.tyfino.foundation.xtream.MovieDetailsRepository
import dev.tyfino.foundation.xtream.SeriesDetailsRepository
import dev.tyfino.foundation.xtream.SeriesEpisode
import dev.tyfino.foundation.xtream.SecureXtreamAccountStore
import dev.tyfino.foundation.xtream.XtreamAccountSummary
import dev.tyfino.foundation.xtream.XtreamAccountDataCleaner
import dev.tyfino.foundation.xtream.XtreamAccountPartitionCleaner
import dev.tyfino.foundation.xtream.XtreamAccountStore
import dev.tyfino.foundation.xtream.XtreamAccountsSnapshot
import dev.tyfino.foundation.xtream.XtreamController
import dev.tyfino.foundation.xtream.XtreamRepository
import dev.tyfino.foundation.xtream.XtreamRemoveResult
import dev.tyfino.foundation.xtream.XtreamSwitchResult
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
    val xtreamRepository = remember {
        XtreamRepository(
            store = xtreamStore,
            api = HttpXtreamApi(context),
        )
    }
    val xtreamController = remember {
        XtreamController(xtreamRepository)
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
    val movieDetailsRepository = remember {
        MovieDetailsRepository(xtreamStore, HttpXtreamMovieDetailsApi(context), SQLiteMovieDetailsStore(context))
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
    val accountDataCleaner = remember {
        XtreamAccountDataCleaner(
            listOf(
                XtreamAccountPartitionCleaner(epgRepository::clearAccount),
                XtreamAccountPartitionCleaner(historyRepository::clearAccount),
                XtreamAccountPartitionCleaner(favoritesRepository::clearAccount),
                XtreamAccountPartitionCleaner(episodeResumeRepository::clearAccount),
                XtreamAccountPartitionCleaner(seriesDetailsRepository::clearAccount),
                XtreamAccountPartitionCleaner(movieDetailsRepository::clearAccount),
                XtreamAccountPartitionCleaner(movieResumeRepository::clearAccount),
                XtreamAccountPartitionCleaner(catalogRepository::clearAccount),
            ),
        )
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
            repository = xtreamRepository,
            catalogRepository = catalogRepository,
            favoritesRepository = favoritesRepository,
            historyRepository = historyRepository,
            epgRepository = epgRepository,
            seriesDetailsRepository = seriesDetailsRepository,
            movieDetailsRepository = movieDetailsRepository,
            movieResumeRepository = movieResumeRepository,
            episodeResumeRepository = episodeResumeRepository,
            previousLiveChannelController = previousLiveChannelController,
            accountStore = xtreamStore,
            accountDataCleaner = accountDataCleaner,
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
    repository: XtreamRepository,
    catalogRepository: CatalogRepository,
    favoritesRepository: CatalogFavoritesRepository,
    historyRepository: CatalogHistoryRepository,
    epgRepository: LiveEpgRepository,
    seriesDetailsRepository: SeriesDetailsRepository,
    movieDetailsRepository: MovieDetailsRepository,
    movieResumeRepository: MovieResumeRepository,
    episodeResumeRepository: EpisodeResumeRepository,
    previousLiveChannelController: PreviousLiveChannelController,
    accountStore: XtreamAccountStore,
    accountDataCleaner: XtreamAccountDataCleaner,
) {
    var state by remember { mutableStateOf(controller.state) }
    var addingAccount by remember { mutableStateOf(false) }
    var chooserSnapshot by remember { mutableStateOf<XtreamAccountsSnapshot?>(null) }
    var chooserSwitchingAccountId by remember { mutableStateOf<String?>(null) }
    var chooserManaging by remember { mutableStateOf(false) }
    var chooserRemovingAccountId by remember { mutableStateOf<String?>(null) }
    var chooserStorageError by remember { mutableStateOf(false) }
    var shellEpoch by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val publish: (XtreamUiState) -> Unit = { state = it }
    val publishAccountMutation: (XtreamUiState) -> Unit = { next ->
        state = next
        if (next is XtreamUiState.SignedIn) {
            shellEpoch++
            addingAccount = false
            chooserSnapshot = null
            chooserSwitchingAccountId = null
            chooserManaging = false
            chooserStorageError = false
        }
    }

    LaunchedEffect(controller) {
        controller.initialize(publish)
        if (state is XtreamUiState.SignedOut) {
            chooserSnapshot = runCatching { repository.accountSnapshot() }.getOrNull()
                ?.takeIf { it.accounts.isNotEmpty() }
        }
    }
    DisposableEffect(controller) {
        onDispose { controller.deactivate() }
    }

    val signedIn = state as? XtreamUiState.SignedIn
    if (signedIn != null && !addingAccount) {
        key(shellEpoch, signedIn.account.accountId) {
            LicensedAppShell(
                catalogRepository = catalogRepository,
                favoritesRepository = favoritesRepository,
                historyRepository = historyRepository,
                epgRepository = epgRepository,
                seriesDetailsRepository = seriesDetailsRepository,
                movieDetailsRepository = movieDetailsRepository,
                movieResumeRepository = movieResumeRepository,
                episodeResumeRepository = episodeResumeRepository,
                previousLiveChannelController = previousLiveChannelController,
                accountStore = accountStore,
                accountRepository = repository,
                accountDataCleaner = accountDataCleaner,
                onAddXtreamAccount = { addingAccount = true },
                onAccountSwitched = { account ->
                    previousLiveChannelController.clear()
                    publishAccountMutation(XtreamUiState.SignedIn(account))
                },
                onActiveAccountRemoved = { remaining ->
                    previousLiveChannelController.clear()
                    shellEpoch++
                    state = XtreamUiState.SignedOut()
                    chooserSnapshot = remaining.takeIf { it.accounts.isNotEmpty() }
                    chooserManaging = false
                    chooserStorageError = false
                },
            )
        }
    } else if (chooserSnapshot != null && !addingAccount) {
        val snapshot = requireNotNull(chooserSnapshot)
        if (chooserManaging) {
            XtreamAccountManager(
                snapshot = snapshot,
                removingAccountId = chooserRemovingAccountId,
                storageError = chooserStorageError,
                onRemoveAccount = { accountId ->
                    chooserRemovingAccountId = accountId
                    chooserStorageError = false
                    scope.launch {
                        when (
                            val result = repository.removeAccount(
                                accountId,
                                accountDataCleaner::clearAccount,
                            )
                        ) {
                            is XtreamRemoveResult.Removed -> {
                                chooserRemovingAccountId = null
                                chooserSnapshot = result.remaining.takeIf { it.accounts.isNotEmpty() }
                                if (chooserSnapshot == null) chooserManaging = false
                            }
                            XtreamRemoveResult.NotFound,
                            XtreamRemoveResult.LocalStorage,
                            -> {
                                chooserRemovingAccountId = null
                                chooserStorageError = true
                            }
                        }
                    }
                },
                onBack = {
                    chooserManaging = false
                    chooserStorageError = false
                },
            )
        } else {
            XtreamAccountSwitcher(
                snapshot = snapshot,
                switchingAccountId = chooserSwitchingAccountId,
                storageError = chooserStorageError,
                onSelectAccount = { accountId ->
                    chooserSwitchingAccountId = accountId
                    chooserStorageError = false
                    scope.launch {
                        when (val result = repository.switchAccount(accountId)) {
                            is XtreamSwitchResult.Switched -> {
                                publishAccountMutation(XtreamUiState.SignedIn(result.account))
                            }
                            is XtreamSwitchResult.AlreadyActive -> {
                                publishAccountMutation(XtreamUiState.SignedIn(result.account))
                            }
                            XtreamSwitchResult.NotFound,
                            XtreamSwitchResult.LocalStorage,
                            -> {
                                chooserSwitchingAccountId = null
                                chooserStorageError = true
                            }
                        }
                    }
                },
                onAddAccount = {
                    chooserStorageError = false
                    addingAccount = true
                },
                onManageAccounts = {
                    chooserStorageError = false
                    chooserManaging = true
                },
                onDismiss = {},
                dismissible = false,
            )
        }
    } else {
        XtreamLoginScreen(
            state = state,
            onSignIn = { input ->
                scope.launch {
                    controller.signIn(
                        input,
                        if (addingAccount) publishAccountMutation else publish,
                    )
                }
            },
            onConfirmCleartext = {
                scope.launch {
                    controller.confirmCleartext(
                        if (addingAccount) publishAccountMutation else publish,
                    )
                }
            },
            onCancelCleartext = { controller.cancelCleartext(publish) },
            onBack = if (addingAccount) {
                {
                    controller.deactivate()
                    scope.launch {
                        controller.initialize { restored ->
                            state = restored
                            if (restored is XtreamUiState.SignedIn || chooserSnapshot != null) {
                                addingAccount = false
                            }
                        }
                    }
                }
            } else {
                null
            },
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
    movieDetailsRepository: MovieDetailsRepository,
    movieResumeRepository: MovieResumeRepository,
    episodeResumeRepository: EpisodeResumeRepository,
    previousLiveChannelController: PreviousLiveChannelController,
    accountStore: XtreamAccountStore,
    accountRepository: XtreamRepository,
    accountDataCleaner: XtreamAccountDataCleaner,
    onAddXtreamAccount: () -> Unit,
    onAccountSwitched: (XtreamAccountSummary) -> Unit,
    onActiveAccountRemoved: (XtreamAccountsSnapshot) -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val navigationType = navigationTypeFor(windowSizeClass)
    val scope = rememberCoroutineScope()
    var playbackSelection by remember { mutableStateOf<PlaybackSelection?>(null) }
    var seriesSelection by remember { mutableStateOf<SeriesSelection?>(null) }
    var movieSelection by remember { mutableStateOf<MovieSelection?>(null) }
    var episodePlaybackSelection by remember { mutableStateOf<EpisodePlaybackSelection?>(null) }
    var showAccountSwitcher by remember { mutableStateOf(false) }
    var accountSnapshot by remember { mutableStateOf<XtreamAccountsSnapshot?>(null) }
    var accountSurfaceEpoch by remember { mutableIntStateOf(0) }
    var switchingAccountId by remember { mutableStateOf<String?>(null) }
    var switchStorageError by remember { mutableStateOf(false) }
    var showAccountManager by remember { mutableStateOf(false) }
    var managerReturnsToSwitcher by remember { mutableStateOf(false) }
    var removingAccountId by remember { mutableStateOf<String?>(null) }
    var removalStorageError by remember { mutableStateOf(false) }

    LaunchedEffect(showAccountSwitcher, showAccountManager, accountSurfaceEpoch) {
        if (showAccountSwitcher || showAccountManager) {
            val ownerEpoch = accountSurfaceEpoch
            val loadingSwitcher = showAccountSwitcher
            val loaded = runCatching { accountRepository.accountSnapshot() }.getOrNull()
            if (
                accountSurfaceEpoch == ownerEpoch &&
                (loadingSwitcher && showAccountSwitcher || !loadingSwitcher && showAccountManager)
            ) {
                accountSnapshot = loaded
                if (loadingSwitcher) switchStorageError = loaded == null
                else removalStorageError = loaded == null
            }
        }
    }

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
    val openMovie: (CatalogItem) -> Unit = { item ->
        accountStore.load()?.let { account ->
            movieSelection = MovieSelection(account.accountId, account.generation, item)
            navController.navigate(MOVIE_DETAILS_ROUTE) { launchSingleTop = true }
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
            movieDetailsRepository = movieDetailsRepository,
            movieResumeRepository = movieResumeRepository,
            episodeResumeRepository = episodeResumeRepository,
            previousLiveChannelController = previousLiveChannelController,
            accountStore = accountStore,
            seriesSelection = seriesSelection,
            movieSelection = movieSelection,
            episodePlaybackSelection = episodePlaybackSelection,
            playbackSelection = playbackSelection,
            onPlay = play,
            onOpenSeries = openSeries,
            onOpenMovie = openMovie,
            onPlayEpisode = playEpisode,
            onResumeEpisode = resumeEpisode,
            onPreviousLive = playPreviousLive,
            onOpenSettings = { navigateTo(AppDestination.Settings) },
            onOpenAccountSwitcher = {
                accountSurfaceEpoch++
                accountSnapshot = null
                switchStorageError = false
                showAccountSwitcher = true
            },
            onPlaybackClosed = {
                playbackSelection = null
                episodePlaybackSelection = null
            },
            onManageAccounts = {
                accountSurfaceEpoch++
                accountSnapshot = null
                removalStorageError = false
                managerReturnsToSwitcher = false
                showAccountManager = true
            },
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

    if (showAccountSwitcher) {
        XtreamAccountSwitcher(
            snapshot = accountSnapshot,
            switchingAccountId = switchingAccountId,
            storageError = switchStorageError,
            onSelectAccount = { accountId ->
                switchingAccountId = accountId
                switchStorageError = false
                scope.launch {
                    when (val result = accountRepository.switchAccount(accountId)) {
                        is XtreamSwitchResult.Switched -> {
                            showAccountSwitcher = false
                            switchingAccountId = null
                            onAccountSwitched(result.account)
                        }
                        is XtreamSwitchResult.AlreadyActive -> {
                            showAccountSwitcher = false
                            switchingAccountId = null
                        }
                        XtreamSwitchResult.NotFound,
                        XtreamSwitchResult.LocalStorage,
                        -> {
                            switchingAccountId = null
                            switchStorageError = true
                        }
                    }
                }
            },
            onAddAccount = {
                showAccountSwitcher = false
                onAddXtreamAccount()
            },
            onManageAccounts = {
                accountSurfaceEpoch++
                showAccountSwitcher = false
                managerReturnsToSwitcher = true
                showAccountManager = true
                removalStorageError = false
            },
            onDismiss = { showAccountSwitcher = false },
        )
    }
    if (showAccountManager) {
        XtreamAccountManager(
            snapshot = accountSnapshot,
            removingAccountId = removingAccountId,
            storageError = removalStorageError,
            onRemoveAccount = { accountId ->
                removingAccountId = accountId
                removalStorageError = false
                scope.launch {
                    when (
                        val result = accountRepository.removeAccount(
                            accountId,
                            accountDataCleaner::clearAccount,
                        )
                    ) {
                        is XtreamRemoveResult.Removed -> {
                            removingAccountId = null
                            accountSnapshot = result.remaining
                            if (result.wasActive) {
                                showAccountManager = false
                                onActiveAccountRemoved(result.remaining)
                            }
                        }
                        XtreamRemoveResult.NotFound,
                        XtreamRemoveResult.LocalStorage,
                        -> {
                            removingAccountId = null
                            removalStorageError = true
                        }
                    }
                }
            },
            onBack = {
                showAccountManager = false
                showAccountSwitcher = managerReturnsToSwitcher
            },
        )
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
    movieDetailsRepository: MovieDetailsRepository,
    movieResumeRepository: MovieResumeRepository,
    episodeResumeRepository: EpisodeResumeRepository,
    previousLiveChannelController: PreviousLiveChannelController,
    accountStore: XtreamAccountStore,
    seriesSelection: SeriesSelection?,
    movieSelection: MovieSelection?,
    episodePlaybackSelection: EpisodePlaybackSelection?,
    playbackSelection: PlaybackSelection?,
    onPlay: (CatalogSection, CatalogItem) -> Unit,
    onOpenSeries: (CatalogItem) -> Unit,
    onOpenMovie: (CatalogItem) -> Unit,
    onPlayEpisode: (SeriesEpisode, Long) -> Unit,
    onResumeEpisode: (SeriesContinueWatchingItem) -> Unit,
    onPreviousLive: (PlaybackSelection) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAccountSwitcher: () -> Unit,
    onPlaybackClosed: () -> Unit,
    onManageAccounts: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Foundation.route,
        modifier = modifier,
    ) {
        composable(AppDestination.Foundation.route) {
            FoundationScreen(
                onOpenAccountSwitcher = onOpenAccountSwitcher,
                onOpenSettings = onOpenSettings,
            )
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
                onPlay = onOpenMovie,
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
        composable(MOVIE_DETAILS_ROUTE) {
            val selection = movieSelection
            if (selection == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                MovieDetailsScreen(
                    selection = selection,
                    repository = movieDetailsRepository,
                    resumeRepository = movieResumeRepository,
                    onBack = { navController.popBackStack() },
                    onPlay = { item -> onPlay(CatalogSection.Movies, item) },
                )
            }
        }
        composable(AppDestination.Settings.route) {
            SettingsScreen(
                onOpenAccountSwitcher = onOpenAccountSwitcher,
                onManageAccounts = onManageAccounts,
            )
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
private const val MOVIE_DETAILS_ROUTE = "movie-details"

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
