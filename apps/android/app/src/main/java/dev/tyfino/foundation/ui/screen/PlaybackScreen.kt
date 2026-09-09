@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package dev.tyfino.foundation.ui.screen

import android.graphics.Color
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import dev.tyfino.foundation.R
import dev.tyfino.foundation.playback.BoundedRedirectDataSource
import dev.tyfino.foundation.playback.PlaybackOperationGate
import dev.tyfino.foundation.playback.PlaybackReferenceFailure
import dev.tyfino.foundation.playback.PlaybackReferenceResult
import dev.tyfino.foundation.playback.PlaybackSelection
import dev.tyfino.foundation.playback.SecretPlaybackReference
import dev.tyfino.foundation.playback.XtreamPlaybackReferenceBuilder
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.xtream.XtreamAccountStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun PlaybackScreen(
    selection: PlaybackSelection,
    accountStore: XtreamAccountStore,
    onBack: () -> Unit,
) {
    val gate = remember { PlaybackOperationGate().also(PlaybackOperationGate::activateDestination) }
    var preparationAttempt by remember { mutableIntStateOf(0) }
    var preparation by remember(selection) {
        mutableStateOf<PlaybackPreparation>(PlaybackPreparation.Loading)
    }

    DisposableEffect(gate) {
        onDispose { gate.deactivateDestination() }
    }
    LaunchedEffect(selection, preparationAttempt) {
        preparation = PlaybackPreparation.Loading
        val owner = gate.begin(selection)
        val outcome = withContext(Dispatchers.IO) {
            val account = accountStore.load()
            if (account == null) {
                PlaybackPreparation.Failure(PlaybackReferenceFailure.AccountChanged)
            } else {
                when (val result = XtreamPlaybackReferenceBuilder.build(account, selection)) {
                    is PlaybackReferenceResult.Ready -> PlaybackPreparation.Ready(
                        reference = result.reference,
                        cleartextConsent = account.cleartextConsent,
                    )
                    is PlaybackReferenceResult.Failure -> PlaybackPreparation.Failure(result.reason)
                }
            }
        }
        when (outcome) {
            is PlaybackPreparation.Ready -> {
                val activeAccount = accountStore.load()
                if (!gate.commit(owner, activeAccount) { preparation = outcome }) {
                    gate.commitFailure(owner) {
                        preparation = PlaybackPreparation.Failure(
                            PlaybackReferenceFailure.AccountChanged,
                        )
                    }
                }
            }
            is PlaybackPreparation.Failure -> {
                gate.commitFailure(owner) { preparation = outcome }
            }
            PlaybackPreparation.Loading -> Unit
        }
    }

    BackHandler(onBack = onBack)
    when (val state = preparation) {
        PlaybackPreparation.Loading -> PlaybackLoading(onBack)
        is PlaybackPreparation.Failure -> PlaybackFailure(
            message = state.reason.messageResource(),
            onRetry = { preparationAttempt++ },
            onBack = onBack,
        )
        is PlaybackPreparation.Ready -> PlayerSurface(
            reference = state.reference,
            cleartextConsent = state.cleartextConsent,
            onBack = onBack,
        )
    }
}

@Composable
private fun PlayerSurface(
    reference: SecretPlaybackReference,
    cleartextConsent: Boolean,
    onBack: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    var retryAttempt by remember { mutableIntStateOf(0) }
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var playbackFailed by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, reference, cleartextConsent, retryAttempt) {
        fun releasePlayer() {
            val current = player
            player = null
            current?.release()
        }
        fun startPlayer() {
            if (player != null) return
            playbackFailed = false
            player = createPlayer(
                context = context,
                reference = reference,
                cleartextConsent = cleartextConsent,
                onFailure = { playbackFailed = true },
            )
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> startPlayer()
                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> releasePlayer()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            startPlayer()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            releasePlayer()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor.Black)
            .testTag("playback-screen"),
    ) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    setBackgroundColor(Color.BLACK)
                    useController = true
                    controllerAutoShow = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { view -> view.player = player },
            modifier = Modifier.fillMaxSize(),
        )
        FocusVisibleButton(
            label = stringResource(R.string.playback_back),
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(WindowInsets.safeDrawing.asPaddingValues())
                .padding(12.dp),
        )
        if (player == null && !playbackFailed) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        if (playbackFailed) {
            PlaybackFailure(
                message = R.string.playback_error_failed,
                onRetry = { retryAttempt++ },
                onBack = onBack,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

private fun createPlayer(
    context: android.content.Context,
    reference: SecretPlaybackReference,
    cleartextConsent: Boolean,
    onFailure: () -> Unit,
): ExoPlayer {
    val mediaSourceFactory = DefaultMediaSourceFactory(context)
        .setDataSourceFactory(BoundedRedirectDataSource.Factory(cleartextConsent))
    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(mediaSourceFactory)
        .build()
        .apply {
            addListener(
                object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        onFailure()
                    }
                },
            )
            setMediaItem(MediaItem.fromUri(reference.uri.toASCIIString()))
            playWhenReady = true
            prepare()
        }
}

@Composable
private fun PlaybackLoading(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor.Black),
    ) {
        CircularProgressIndicator(Modifier.align(Alignment.Center))
        Text(
            text = stringResource(R.string.playback_loading),
            color = ComposeColor.White,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 88.dp),
        )
        FocusVisibleButton(
            label = stringResource(R.string.playback_back),
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(WindowInsets.safeDrawing.asPaddingValues())
                .padding(12.dp),
        )
    }
}

@Composable
private fun PlaybackFailure(
    @StringRes message: Int,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(message),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
        FocusVisibleButton(
            label = stringResource(R.string.playback_retry),
            onClick = onRetry,
        )
        FocusVisibleButton(
            label = stringResource(R.string.playback_back),
            onClick = onBack,
        )
    }
}

@StringRes
private fun PlaybackReferenceFailure.messageResource(): Int = when (this) {
    PlaybackReferenceFailure.AccountChanged -> R.string.playback_error_account
    PlaybackReferenceFailure.InvalidMetadata -> R.string.playback_error_metadata
    PlaybackReferenceFailure.CleartextNotApproved -> R.string.playback_error_cleartext
}

private sealed interface PlaybackPreparation {
    data object Loading : PlaybackPreparation

    class Ready(
        val reference: SecretPlaybackReference,
        val cleartextConsent: Boolean,
    ) : PlaybackPreparation

    data class Failure(val reason: PlaybackReferenceFailure) : PlaybackPreparation
}
