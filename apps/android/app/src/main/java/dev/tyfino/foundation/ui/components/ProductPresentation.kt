package dev.tyfino.foundation.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.tyfino.foundation.R

/** Presentation only; authorization, navigation and durable state retain their existing owners. */
@Composable
internal fun ProductHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    eyebrow: String = stringResource(R.string.app_name),
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (eyebrow.isNotBlank()) Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(painterResource(R.drawable.ic_app_tyfino), contentDescription = null,
                tint = Color.Unspecified, modifier = Modifier.size(22.dp))
            Text(eyebrow, style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
        }
        Text(title, style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() })
        subtitle?.takeIf(String::isNotBlank)?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ProductPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(
            Modifier.background(Brush.linearGradient(listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f),
                MaterialTheme.colorScheme.surfaceContainer,
            ))).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
internal fun productTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
)

@Composable
internal fun ProductSectionHeading(title: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(width = 4.dp, height = 20.dp)
            .clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary))
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

/** Uses the existing bounded image loader; titles/facts remain useful without artwork. */
@Composable
internal fun MediaDetailsHero(
    title: String,
    facts: String,
    artwork: String?,
    modifier: Modifier = Modifier,
    landscape: Boolean = false,
    @DrawableRes icon: Int = R.drawable.ic_nav_movies,
) {
    ProductPanel(modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val posterWidth = if (maxWidth >= 600.dp) 176.dp else 88.dp
            val backdropHeight = minOf(maxWidth * (9f / 16f), 320.dp)
            if (landscape) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DetailsArtwork(artwork, icon,
                        Modifier.fillMaxWidth().height(backdropHeight))
                    DetailsHeading(title, facts)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DetailsArtwork(artwork, icon, Modifier.width(posterWidth).aspectRatio(2f / 3f))
                    DetailsHeading(title, facts, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DetailsHeading(title: String, facts: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() })
        if (facts.isNotBlank()) Text(facts, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DetailsArtwork(artwork: String?, @DrawableRes icon: Int, modifier: Modifier) {
    Box(modifier.clip(MaterialTheme.shapes.medium).background(Brush.linearGradient(listOf(
        MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer))),
        contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
        if (artwork != null) AsyncImage(model = artwork, contentDescription = null,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}
