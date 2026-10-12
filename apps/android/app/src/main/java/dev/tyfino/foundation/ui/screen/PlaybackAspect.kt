@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package dev.tyfino.foundation.ui.screen

import android.graphics.Color
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import dev.tyfino.foundation.R
import dev.tyfino.foundation.playback.PlaybackSelection
import dev.tyfino.foundation.ui.components.FocusIconButton
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.ui.components.ProductHeader
import dev.tyfino.foundation.ui.components.rememberInitialFocusRequester

internal enum class PlaybackAspectMode(@StringRes val label: Int) {
    Fit(R.string.playback_picture_fit),
    Zoom(R.string.playback_picture_zoom),
    Stretch(R.string.playback_picture_stretch),
}

/** Presentation only; never saved or shared across a different playback owner. */
@Composable
internal fun rememberPlaybackAspectMode(owner: PlaybackSelection): MutableState<PlaybackAspectMode> =
    remember(owner) { mutableStateOf(PlaybackAspectMode.Fit) }

@Composable
internal fun PlaybackVideoView(player: Player?, mode: PlaybackAspectMode, modifier: Modifier = Modifier) {
    AndroidView(factory = { context ->
        PlayerView(context).apply {
            setBackgroundColor(Color.BLACK)
            useController = true
            controllerAutoShow = true
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
        }
    }, update = { view ->
        if (view.player !== player) view.player = player
        view.resizeMode = when (mode) {
            PlaybackAspectMode.Fit -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            PlaybackAspectMode.Zoom -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            PlaybackAspectMode.Stretch -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        }
    }, modifier = modifier)
}

@Composable
internal fun PlaybackAspectAction(mode: PlaybackAspectMode, onOpen: () -> Unit) {
    val currentMode = stringResource(mode.label)
    FocusIconButton(R.drawable.ic_player_aspect, stringResource(R.string.playback_picture_size), onOpen,
        Modifier.testTag("playback-aspect").semantics { stateDescription = currentMode },
        selected = mode != PlaybackAspectMode.Fit)
}

@Composable
internal fun PlaybackAspectDialog(mode: PlaybackAspectMode, onSelect: (PlaybackAspectMode) -> Unit,
    onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        PlaybackAspectContent(mode, rememberInitialFocusRequester(mode), onSelect, onDismiss)
    }
}

@Composable
internal fun PlaybackAspectContent(mode: PlaybackAspectMode, firstFocus: FocusRequester,
    onSelect: (PlaybackAspectMode) -> Unit, onDismiss: () -> Unit) {
    Surface(Modifier.widthIn(max = 520.dp).fillMaxWidth().heightIn(max = 680.dp)
        .testTag("playback-aspect-dialog"), shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.verticalScroll(rememberScrollState()).testTag("playback-aspect-scroll")
            .padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProductHeader(stringResource(R.string.playback_picture_size),
                eyebrow = stringResource(R.string.product_player_options))
            for (option in PlaybackAspectMode.entries) {
                FocusVisibleButton(stringResource(option.label), { onSelect(option) },
                    Modifier.fillMaxWidth().testTag("aspect-mode-$option")
                        .semantics { selected = option == mode }
                        .then(if (option == mode) Modifier.focusRequester(firstFocus) else Modifier),
                    prominent = option == mode)
            }
            FocusVisibleButton(stringResource(R.string.playback_close), onDismiss,
                Modifier.fillMaxWidth().testTag("aspect-close"), prominent = false)
        }
    }
}
