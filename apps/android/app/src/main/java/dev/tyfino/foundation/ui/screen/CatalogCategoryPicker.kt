package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.ui.components.rememberInitialFocusRequester
import dev.tyfino.foundation.xtream.CatalogCategory

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
    val initialIndex = categories.indexOfFirst { it.providerId == selectedCategoryId }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    Surface(Modifier.widthIn(max = 620.dp).fillMaxWidth().heightIn(max = 680.dp)
        .testTag("catalog-category-picker"), shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.catalog_category_picker),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() })
            LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 400.dp)
                .testTag("catalog-category-picker-list"), state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(categories, key = { _, category -> category.providerId }) { index, category ->
                    val isSelected = category.providerId == selectedCategoryId
                    FocusVisibleButton(category.name, { onSelect(category) },
                        Modifier.fillMaxWidth().semantics { selected = isSelected }
                            .testTag("catalog-category-picker-option-$index"), prominent = isSelected)
                }
            }
            FocusVisibleButton(stringResource(R.string.playback_close), onDismiss,
                Modifier.fillMaxWidth().focusRequester(closeFocus).testTag("catalog-category-picker-close"),
                prominent = false)
        }
    }
}
