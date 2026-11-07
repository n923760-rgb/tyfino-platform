@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package dev.tyfino.foundation.ui.screen

import android.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoFrameProcessor
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.ui.components.ProductHeader
import dev.tyfino.foundation.ui.components.rememberInitialFocusRequester

@Composable
internal fun VisionLabEntry() {
    var visible by remember { mutableStateOf(false) }
    FocusVisibleButton(stringResource(R.string.vision_lab_open), { visible = true },
        Modifier.fillMaxWidth().testTag("vision-lab-open"), prominent = false)
    if (visible) VisionLabDialog { visible = false }
}

/** Bundled synthetic SDR only. No provider URI, account, settings persistence or production factory. */
@Composable
internal fun VisionLabDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val dismiss by rememberUpdatedState(onDismiss)
    var profile by remember { mutableStateOf(VisionLabProfile()) }
    var ready by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val player = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            setVideoEffects(emptyList()) // Deliberately opt in only for this laboratory player.
            setMediaItem(MediaItem.fromUri("asset:///vision-sdr.mp4"))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
        }
    }
    DisposableEffect(player, lifecycleOwner) {
        var prepared = false
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() { ready = true }
            override fun onPlayerError(error: PlaybackException) {
                failed = true
                player.stop()
            }
        }
        fun start() {
            if (!prepared) {
                prepared = true
                player.prepare()
                player.playWhenReady = true
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> start()
                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                    player.pause()
                    dismiss()
                }
                else -> Unit
            }
        }
        player.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) start()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.removeListener(listener)
            player.release()
        }
    }
    LaunchedEffect(player, profile.effectiveStrength, failed) {
        if (!failed) {
            player.setVideoEffects(if (profile.effectiveStrength == 0f) emptyList()
                else listOf(VisionBalancedEffect(profile.effectiveStrength)))
            if (!player.isPlaying) player.setVideoEffects(VideoFrameProcessor.REDRAW)
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        val initialFocus = rememberInitialFocusRequester(failed)
        Surface(Modifier.widthIn(max = 720.dp).fillMaxWidth().heightIn(max = 720.dp)
            .testTag("vision-lab-dialog"), shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProductHeader(stringResource(R.string.vision_lab_title))
                Text(stringResource(R.string.vision_lab_description))
                AndroidView(factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        setBackgroundColor(Color.BLACK)
                        useController = false
                        this.player = player
                    }
                }, onRelease = { it.player = null },
                    modifier = Modifier.fillMaxWidth().height(180.dp).testTag("vision-lab-video"))
                Text(stringResource(if (failed) R.string.vision_lab_failed
                    else if (ready) R.string.vision_lab_ready else R.string.vision_lab_loading),
                    Modifier.testTag(if (failed) "vision-lab-failed" else if (ready) "vision-lab-ready" else "vision-lab-loading"))
                VisionLabControls(profile, failed, { profile = it }, onDismiss,
                    Modifier.focusRequester(initialFocus))
            }
        }
    }
}

@Composable
internal fun VisionLabControls(profile: VisionLabProfile, failed: Boolean,
    onChange: (VisionLabProfile) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FocusVisibleButton(stringResource(R.string.vision_lab_original),
            { onChange(profile.copy(balanced = false, compareOriginal = false)) },
            (if (failed) Modifier else modifier).weight(1f).semantics { selected = !profile.balanced }.testTag("vision-original"),
            enabled = !failed, prominent = !profile.balanced)
        FocusVisibleButton(stringResource(R.string.vision_lab_balanced),
            { onChange(profile.copy(balanced = true, compareOriginal = false)) },
            Modifier.weight(1f).semantics { selected = profile.balanced }.testTag("vision-balanced"),
            enabled = !failed, prominent = profile.balanced)
    }
    Text(stringResource(R.string.vision_lab_strength, profile.strength), Modifier.testTag("vision-strength"))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FocusVisibleButton(stringResource(R.string.vision_lab_less),
            { onChange(profile.copy(strength = (profile.strength - 25).coerceAtLeast(0))) },
            Modifier.weight(1f).testTag("vision-less"), enabled = !failed && profile.strength > 0)
        FocusVisibleButton(stringResource(R.string.vision_lab_more),
            { onChange(profile.copy(strength = (profile.strength + 25).coerceAtMost(100))) },
            Modifier.weight(1f).testTag("vision-more"), enabled = !failed && profile.strength < 100)
    }
    FocusVisibleButton(stringResource(if (profile.compareOriginal) R.string.vision_lab_return else R.string.vision_lab_compare),
        { onChange(profile.copy(compareOriginal = !profile.compareOriginal)) },
        Modifier.fillMaxWidth().semantics { selected = profile.compareOriginal }.testTag("vision-compare"),
        enabled = !failed && profile.balanced && profile.strength > 0, prominent = false)
    FocusVisibleButton(stringResource(R.string.playback_close), onClose,
        (if (failed) modifier else Modifier).fillMaxWidth().testTag("vision-close"), prominent = false)
}
