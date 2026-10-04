package dev.tyfino.foundation.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import dev.tyfino.foundation.ui.components.ProductHeader
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.tyfino.foundation.R

@Composable
internal fun AppBottomBar(
    selectedRoute: String?,
    onDestinationSelected: (AppDestination) -> Unit,
) {
    NavigationBar(
        modifier = Modifier.focusGroup().testTag("app-bottom-menu")
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 0.dp,
    ) {
        AppDestination.entries.forEach { destination ->
            var focused by remember { mutableStateOf(false) }
            NavigationBarItem(
                modifier = Modifier
                    .testTag("destination-${destination.route}")
                    .onFocusChanged { focused = it.isFocused }
                    .border(
                        BorderStroke(2.dp, if (focused) MaterialTheme.colorScheme.onSurface else Color.Transparent),
                        RoundedCornerShape(16.dp),
                    ),
                selected = selectedRoute == destination.route,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(painterResource(destination.iconRes), contentDescription = null, modifier = Modifier.size(24.dp))
                },
                label = {
                    Text(stringResource(destination.labelRes), maxLines = 2,
                        overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
internal fun AppNavigationRail(
    selectedRoute: String?,
    onDestinationSelected: (AppDestination) -> Unit,
    expanded: Boolean,
) {
    Surface(
        modifier = Modifier.width(if (expanded) 232.dp else 96.dp).fillMaxHeight()
            .testTag(if (expanded) "app-expanded-menu" else "app-compact-menu"),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start),
            ),
        ) {
            if (expanded) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(painterResource(R.drawable.ic_app_tyfino), contentDescription = null,
                        tint = Color.Unspecified, modifier = Modifier.size(40.dp))
                    ProductHeader(
                        title = stringResource(R.string.app_name),
                        eyebrow = "",
                        subtitle = stringResource(R.string.product_navigation_caption),
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_app_tyfino), contentDescription = null,
                        tint = Color.Unspecified, modifier = Modifier.size(40.dp))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            // Scrollable even in a short landscape/multi-window pane; settings stays reachable.
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().focusGroup().testTag("app-side-menu"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (expanded) item(key = "browse-heading") {
                    MenuHeading(R.string.navigation_browse)
                }
                items(AppDestination.entries.filter { it != AppDestination.Settings }, key = { it.route }) { destination ->
                    SideNavigationItem(destination, selectedRoute == destination.route,
                        { onDestinationSelected(destination) }, expanded)
                }
                item(key = "settings-heading") {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    if (expanded) MenuHeading(R.string.navigation_preferences)
                }
                item(key = AppDestination.Settings.route) {
                    SideNavigationItem(AppDestination.Settings, selectedRoute == AppDestination.Settings.route,
                        { onDestinationSelected(AppDestination.Settings) }, expanded)
                }
            }
        }
    }
}

@Composable
private fun MenuHeading(@androidx.annotation.StringRes label: Int) {
    Text(stringResource(label), style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp).semantics { heading() })
}

@Composable
private fun SideNavigationItem(
    destination: AppDestination,
    selected: Boolean,
    onClick: () -> Unit,
    expanded: Boolean,
) {
    var focused by remember { mutableStateOf(false) }
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = if (expanded) 56.dp else 76.dp)
            .testTag("destination-${destination.route}")
            .onFocusChanged { focused = it.isFocused }
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        contentColor = contentColor,
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            when {
                focused -> MaterialTheme.colorScheme.onSurface
                selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else -> Color.Transparent
            },
        ),
    ) {
        if (expanded) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(painterResource(destination.iconRes), contentDescription = null, modifier = Modifier.size(24.dp))
                Text(stringResource(destination.labelRes), style = MaterialTheme.typography.titleSmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(painterResource(destination.iconRes), contentDescription = null, modifier = Modifier.size(24.dp))
                Text(stringResource(destination.labelRes), style = MaterialTheme.typography.labelSmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
        }
    }
}
