package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.ui.components.ProductPanel

/** Guidance for a successfully loaded empty list, never a substitute for a failed read. */
@Composable
internal fun CatalogFavoritesEmptyState(onBrowse: () -> Unit) {
    CatalogStatusPane {
        ProductPanel(Modifier.fillMaxWidth().testTag("catalog-favorites-guidance")) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(painterResource(R.drawable.ic_favorite_outline), contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Text(stringResource(R.string.catalog_favorites_only),
                    style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().semantics { heading() })
                Text(stringResource(R.string.catalog_favorites_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().testTag("catalog-empty"))
                Text(stringResource(R.string.catalog_favorites_help),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
                FocusVisibleButton(stringResource(R.string.catalog_favorites_browse), onBrowse,
                    Modifier.fillMaxWidth().testTag("catalog-favorites-browse"))
            }
        }
    }
}
