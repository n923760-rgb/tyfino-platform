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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.tyfino.foundation.ui.screen.FoundationScreen
import dev.tyfino.foundation.ui.screen.SettingsScreen

@Composable
internal fun TyfinoApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val navigationType = navigationTypeFor(windowSizeClass)

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

    if (navigationType == AppNavigationType.BottomBar) {
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
            AppNavHost(
                navController = navController,
                modifier = Modifier.padding(contentPadding),
                onOpenSettings = { navigateTo(AppDestination.Settings) },
            )
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
            AppNavHost(
                navController = navController,
                modifier = Modifier
                    .weight(1f)
                    .padding(WindowInsets.safeDrawing.asPaddingValues()),
                onOpenSettings = { navigateTo(AppDestination.Settings) },
            )
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier,
    onOpenSettings: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Foundation.route,
        modifier = modifier,
    ) {
        composable(AppDestination.Foundation.route) {
            FoundationScreen(onOpenSettings = onOpenSettings)
        }
        composable(AppDestination.Settings.route) {
            SettingsScreen()
        }
    }
}

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
