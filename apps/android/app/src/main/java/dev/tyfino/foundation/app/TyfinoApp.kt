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
import dev.tyfino.foundation.playback.MovieResumeRepository
import dev.tyfino.foundation.playback.PlaybackSelection
import dev.tyfino.foundation.playback.SQLiteMovieResumeStore
import dev.tyfino.foundation.ui.screen.PlaybackScreen
import dev.tyfino.foundation.ui.screen.CatalogScreen
import dev.tyfino.foundation.ui.screen.FoundationScreen
import dev.tyfino.foundation.ui.screen.LicensingScreen
import dev.tyfino.foundation.ui.screen.SettingsScreen
import dev.tyfino.foundation.ui.screen.XtreamLoginScreen
import dev.tyfino.foundation.xtream.CatalogItem
import dev.tyfino.foundation.xtream.CatalogRepository
import dev.tyfino.foundation.xtream.CatalogSection
import dev.tyfino.foundation.xtream.HttpXtreamApi
import dev.tyfino.foundation.xtream.HttpXtreamCatalogApi
import dev.tyfino.foundation.xtream.SQLiteCatalogStore
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
    val movieResumeRepository = remember {
        MovieResumeRepository(
            accountStore = xtreamStore,
            store = SQLiteMovieResumeStore(context),
        )
    }
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
        XtreamGate(xtreamController, catalogRepository, movieResumeRepository, xtreamStore)
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
    movieResumeRepository: MovieResumeRepository,
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
            accountStore = accountStore,
            onRemoveXtreamAccount = {
                scope.launch {
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
    accountStore: XtreamAccountStore,
    onRemoveXtreamAccount: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val navigationType = navigationTypeFor(windowSizeClass)
    var playbackSelection by remember { mutableStateOf<PlaybackSelection?>(null) }

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
    val navHost: @Composable (Modifier) -> Unit = { modifier ->
        AppNavHost(
            navController = navController,
            modifier = modifier,
            catalogRepository = catalogRepository,
            accountStore = accountStore,
            playbackSelection = playbackSelection,
            onPlay = play,
            onOpenSettings = { navigateTo(AppDestination.Settings) },
            onPlaybackClosed = { playbackSelection = null },
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
    accountStore: XtreamAccountStore,
    playbackSelection: PlaybackSelection?,
    onPlay: (CatalogSection, CatalogItem) -> Unit,
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
                onPlay = { item -> onPlay(CatalogSection.Live, item) },
            )
        }
        composable(AppDestination.Movies.route) {
            CatalogScreen(
                section = CatalogSection.Movies,
                repository = catalogRepository,
                onPlay = { item -> onPlay(CatalogSection.Movies, item) },
            )
        }
        composable(AppDestination.Series.route) {
            CatalogScreen(
                section = CatalogSection.Series,
                repository = catalogRepository,
                onPlay = {},
            )
        }
        composable(AppDestination.Settings.route) {
            SettingsScreen(onRemoveXtreamAccount = onRemoveXtreamAccount)
        }
        composable(PLAYBACK_ROUTE) {
            val selection = playbackSelection
            if (selection == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                DisposableEffect(Unit) {
                    onDispose(onPlaybackClosed)
                }
                PlaybackScreen(
                    selection = selection,
                    accountStore = accountStore,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private const val PLAYBACK_ROUTE = "playback"

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
