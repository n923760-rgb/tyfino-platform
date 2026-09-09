package dev.tyfino.foundation.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.xtream.CatalogCategory
import dev.tyfino.foundation.xtream.CatalogFailure
import dev.tyfino.foundation.xtream.CatalogItem
import dev.tyfino.foundation.xtream.CatalogRepository
import dev.tyfino.foundation.xtream.CatalogSection
import dev.tyfino.foundation.xtream.CatalogState
import kotlinx.coroutines.launch

@Composable
internal fun CatalogScreen(
    section: CatalogSection,
    repository: CatalogRepository,
) {
    var categories by remember(section) {
        mutableStateOf<CatalogState<CatalogCategory>>(CatalogState.Empty)
    }
    var selectedCategoryId by remember(section) { mutableStateOf<String?>(null) }
    var selectedCategoryName by remember(section) { mutableStateOf<String?>(null) }
    var catalogItems by remember(section) {
        mutableStateOf<CatalogState<CatalogItem>>(CatalogState.Empty)
    }
    val scope = rememberCoroutineScope()

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .testTag("catalog-${section.name.lowercase()}"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
            FocusVisibleButton(
                label = stringResource(R.string.catalog_refresh),
                onClick = {
                    scope.launch {
                        repository.categories(section, forceRefresh = true) { categories = it }
                    }
                },
            )
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
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                FocusVisibleButton(
                    label = stringResource(R.string.catalog_refresh_items),
                    onClick = {
                        val categoryId = selectedCategoryId ?: return@FocusVisibleButton
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
            hasSelection = selectedCategoryId != null,
            onRetry = {
                val categoryId = selectedCategoryId ?: return@ItemContent
                scope.launch {
                    repository.items(section, categoryId, forceRefresh = true) { catalogItems = it }
                }
            },
            modifier = Modifier.weight(1f),
        )
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
        CatalogState.Empty, CatalogState.Loading -> LoadingState()
        is CatalogState.Error -> ErrorState(state.failure, onRetry)
        is CatalogState.EmptyContent -> EmptyState(R.string.catalog_no_categories)
        is CatalogState.Content -> {
            if (state.isRefreshing) RefreshingNotice()
            Categories(state.records, selectedCategoryId, onSelect)
        }
        is CatalogState.StaleContent -> {
            StaleNotice(state.failure)
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
        modifier = Modifier.fillMaxWidth().testTag("catalog-categories"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(categories, key = { it.providerId }) { category ->
            CatalogTile(
                label = category.name,
                selected = category.providerId == selectedCategoryId,
                onClick = { onSelect(category) },
                modifier = Modifier.widthIn(min = 140.dp, max = 260.dp),
            )
        }
    }
}

@Composable
private fun ItemContent(
    state: CatalogState<CatalogItem>,
    hasSelection: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        when (state) {
            CatalogState.Empty -> EmptyState(
                if (hasSelection) R.string.catalog_loading else R.string.catalog_choose_category,
            )
            CatalogState.Loading -> LoadingState()
            is CatalogState.Error -> ErrorState(state.failure, onRetry)
            is CatalogState.EmptyContent -> EmptyState(R.string.catalog_no_items)
            is CatalogState.Content -> {
                ItemGrid(state.records)
                if (state.isRefreshing) RefreshingNotice(Modifier.align(Alignment.TopCenter))
            }
            is CatalogState.StaleContent -> {
                ItemGrid(state.records)
                StaleNotice(state.failure, Modifier.align(Alignment.TopCenter))
            }
        }
    }
}

@Composable
private fun ItemGrid(records: List<CatalogItem>) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 168.dp),
        modifier = Modifier.fillMaxSize().testTag("catalog-items"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(records, key = { it.providerId }) { item ->
            CatalogTile(
                label = item.name,
                supporting = listOfNotNull(item.releaseYear, item.rating).joinToString(" • "),
                selected = false,
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CatalogTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    supporting: String = "",
) {
    var focused by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 88.dp)
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = label },
        border = BorderStroke(
            if (focused) 3.dp else 1.dp,
            when {
                focused -> MaterialTheme.colorScheme.onSurface
                selected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            },
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.titleMedium, maxLines = 2)
            if (supporting.isNotEmpty()) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Row(
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
private fun ErrorState(failure: CatalogFailure, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(failure.messageResource()),
            color = MaterialTheme.colorScheme.error,
        )
        FocusVisibleButton(label = stringResource(R.string.retry), onClick = onRetry)
    }
}

@Composable
private fun RefreshingNotice(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.catalog_refreshing),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun StaleNotice(failure: CatalogFailure, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.catalog_stale, stringResource(failure.messageResource())),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier,
    )
}

@StringRes
private fun CatalogSection.titleResource(): Int = when (this) {
    CatalogSection.Live -> R.string.catalog_live_title
    CatalogSection.Movies -> R.string.catalog_movies_title
    CatalogSection.Series -> R.string.catalog_series_title
}

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
