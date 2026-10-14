package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.ui.components.FocusIconButton
import dev.tyfino.foundation.ui.components.rememberInitialFocusRequester
import dev.tyfino.foundation.xtream.CatalogCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun CatalogCategoryPickerDialog(
    categories: List<CatalogCategory>,
    selectedCategoryId: String?,
    onSelect: (CatalogCategory) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        CatalogCategoryPickerContent(categories, selectedCategoryId,
            rememberInitialFocusRequester(), onSelect, onDismiss)
    }
}

/** Local presentation of the already-published categories; no provider requests or selection effects. */
@Composable
internal fun CatalogCategoryPickerContent(
    categories: List<CatalogCategory>,
    selectedCategoryId: String?,
    closeFocus: FocusRequester,
    onSelect: (CatalogCategory) -> Unit,
    onDismiss: () -> Unit,
) {
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    // Each input/snapshot owns a different state cell. Late work can never publish into its replacement.
    var matches by remember(categories, query) { mutableStateOf<List<CatalogCategory>?>(null) }
    LaunchedEffect(categories, query) {
        if (query.isNotBlank()) {
            matches = withContext(Dispatchers.Default) { filterPickerCategories(categories, query) }
        }
    }
    val visible = if (query.isBlank()) categories else matches
    val keyboard = LocalSoftwareKeyboardController.current
    Surface(Modifier.widthIn(max = 620.dp).fillMaxWidth().heightIn(max = 680.dp)
        .testTag("catalog-category-picker"), shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (searching) {
                OutlinedTextField(query, { query = boundCategoryPickerQuery(it) },
                    Modifier.fillMaxWidth().focusRequester(rememberInitialFocusRequester())
                        .testTag("catalog-category-picker-query"),
                    label = { Text(stringResource(R.string.catalog_category_search)) },
                    singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }))
            } else Text(stringResource(R.string.catalog_category_picker),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() })
            if (visible == null) Text(stringResource(R.string.catalog_loading))
            else if (visible.isEmpty()) Text(stringResource(R.string.catalog_category_search_empty))
            else key(query) {
                val initialIndex = if (query.isBlank()) visible.indexOfFirst {
                    it.providerId == selectedCategoryId
                }.coerceAtLeast(0) else 0
                val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
                LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 400.dp)
                    .testTag("catalog-category-picker-list"), state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(visible, key = { _, category -> category.providerId }) { index, category ->
                        val isSelected = category.providerId == selectedCategoryId
                        FocusVisibleButton(category.name, { onSelect(category) },
                            Modifier.fillMaxWidth().semantics { selected = isSelected }
                                .testTag("catalog-category-picker-option-$index"), prominent = isSelected)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FocusVisibleButton(stringResource(R.string.playback_close), onDismiss,
                    Modifier.weight(1f).focusRequester(closeFocus).testTag("catalog-category-picker-close"),
                    prominent = false)
                FocusIconButton(if (searching) R.drawable.ic_clear_search else R.drawable.ic_search,
                    stringResource(if (searching) R.string.catalog_category_search_close else R.string.catalog_category_search),
                    { searching = !searching; query = ""; keyboard?.hide() },
                    Modifier.testTag("catalog-category-picker-search-toggle"), selected = searching)
            }
        }
    }
}
