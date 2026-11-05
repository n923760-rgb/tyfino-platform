package dev.tyfino.foundation.ui.screen

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.components.FocusIconButton

/** Presentation of the current player-owned policy; never changes or stores a selection. */
@Composable
internal fun PlaybackTrackAction(
    @DrawableRes icon: Int,
    @StringRes description: Int,
    automaticSelected: Boolean,
    offSelected: Boolean,
    selectedLabel: String?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentState = when {
        offSelected -> stringResource(R.string.playback_off)
        automaticSelected -> stringResource(R.string.playback_automatic)
        else -> selectedLabel
    }
    FocusIconButton(icon, stringResource(description), onOpen,
        modifier.semantics { currentState?.let { stateDescription = it } })
}
