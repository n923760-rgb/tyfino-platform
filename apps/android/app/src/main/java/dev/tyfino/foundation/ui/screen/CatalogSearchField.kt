package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.components.FocusIconButton
import dev.tyfino.foundation.ui.components.productTextFieldColors

/** Input and clearing dispatch only the existing query callback; Search only dismisses the IME. */
@Composable
internal fun CatalogSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val query = value.trim()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.catalog_search_label)) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon = if (value.isNotEmpty()) {
            {
                FocusIconButton(
                    icon = R.drawable.ic_clear_search,
                    description = stringResource(R.string.catalog_clear_search),
                    onClick = { onValueChange("") },
                    modifier = Modifier.testTag("catalog-clear-search").onPreviewKeyEvent { event ->
                        // Prevent a vertical key from bubbling into the editable field around this button.
                        if (event.type != KeyEventType.KeyDown) false else when (event.key) {
                            Key.DirectionDown -> focus.moveFocus(FocusDirection.Next)
                            Key.DirectionUp -> focus.moveFocus(FocusDirection.Previous)
                            else -> false
                        }
                    },
                )
            }
        } else null,
        supportingText = if (query.isNotEmpty()) {
            {
                Text(stringResource(if (query.codePointCount(0, query.length) < 2) {
                    R.string.catalog_search_minimum
                } else R.string.catalog_search_scope),
                    modifier = Modifier.testTag("catalog-search-guidance"))
            }
        } else null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        shape = MaterialTheme.shapes.medium,
        colors = productTextFieldColors(),
        singleLine = true,
        modifier = modifier.fillMaxWidth().testTag("catalog-search"),
    )
}
