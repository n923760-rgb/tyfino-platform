package dev.tyfino.foundation.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.tyfino.foundation.R
import dev.tyfino.foundation.app.AppDestination
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.ui.components.rememberInitialFocusRequester

private data class HomeShortcut(
    val destination: AppDestination,
    @StringRes val description: Int,
)

private val shortcuts = listOf(
    HomeShortcut(AppDestination.Live, R.string.home_live_description),
    HomeShortcut(AppDestination.Movies, R.string.home_movies_description),
    HomeShortcut(AppDestination.Series, R.string.home_series_description),
)

@Composable
internal fun HomeScreen(
    onOpenDestination: (AppDestination) -> Unit,
    onOpenAccountSwitcher: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val initialFocus = rememberInitialFocusRequester()
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 240.dp),
            modifier = Modifier
                .width(minOf(maxWidth, 900.dp))
                .fillMaxHeight()
                .focusGroup()
                .testTag("home-screen"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
            items(shortcuts, key = { it.destination.route }, contentType = { "shortcut" }) { shortcut ->
                HomeShortcutCard(
                    title = stringResource(shortcut.destination.labelRes),
                    description = stringResource(shortcut.description),
                    onClick = { onOpenDestination(shortcut.destination) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (shortcut.destination == AppDestination.Live) Modifier.focusRequester(initialFocus) else Modifier)
                        .testTag("home-${shortcut.destination.route}"),
                )
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
            .heightIn(min = 152.dp)
            .onFocusChanged { focused = it.isFocused },
        border = BorderStroke(
            if (focused) 3.dp else 1.dp,
            if (focused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
