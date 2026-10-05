package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Controls keep their natural height until they need scrolling; results retain half the viewport. */
@Composable
internal fun CatalogViewport(
    modifier: Modifier = Modifier,
    controlsScrollState: ScrollState = rememberScrollState(),
    controls: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val gap = 12.dp
        val controlsHeight = ((maxHeight - gap) / 2f).coerceAtLeast(0.dp)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = controlsHeight)
                    .verticalScroll(controlsScrollState).testTag("catalog-controls"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = controls,
            )
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("catalog-results"),
                content = content,
            )
        }
    }
}
