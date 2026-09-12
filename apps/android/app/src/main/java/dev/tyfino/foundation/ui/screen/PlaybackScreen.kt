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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import dev.tyfino.foundation.R
import dev.tyfino.foundation.playback.BoundedRedirectDataSource
import dev.tyfino.foundation.playback.EpisodePlaybackSelection
import dev.tyfino.foundation.playback.EpisodeResumeLoadResult
import dev.tyfino.foundation.playback.EpisodeResumeRepository
import dev.tyfino.foundation.playback.CatalogHistoryRepository
import dev.tyfino.foundation.playback.MovieResumeLoadResult
import dev.tyfino.foundation.playback.MovieResumePresentation
import dev.tyfino.foundation.playback.MovieResumeRepository
import dev.tyfino.foundation.playback.PreviousLiveChannelController
import dev.tyfino.foundation.playback.PlaybackOperationGate
import dev.tyfino.foundation.playback.PlaybackReferenceFailure
import dev.tyfino.foundation.playback.PlaybackReferenceResult
import dev.tyfino.foundation.playback.PlaybackSelection
import dev.tyfino.foundation.playback.PlaybackTrackLabel
import dev.tyfino.foundation.playback.SecretPlaybackReference
import dev.tyfino.foundation.playback.XtreamPlaybackReferenceBuilder
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.xtream.CatalogSection
import dev.tyfino.foundation.xtream.SeriesDetailsRepository
import dev.tyfino.foundation.xtream.XtreamAccountStore
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun PlaybackScreen(
    selection: PlaybackSelection,
    accountStore: XtreamAccountStore,
    resumeRepository: MovieResumeRepository,
    previousLiveChannelController: PreviousLiveChannelController,
    onPreviousLive: () -> Unit,
    onBack: () -> Unit,
    historyRepository: CatalogHistoryRepository? = null,
) {
    val previousLiveState by previousLiveChannelController.state.collectAsState()
    val previousLiveAvailable = remember(selection, previousLiveState) {
        previousLiveChannelController.beginPrevious(selection) != null
    }
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
                val resumePositionMillis = if (selection.section == CatalogSection.Movies) {
                    when (val resume = resumeRepository.load(selection)) {
                        is MovieResumeLoadResult.Ready -> resume.record?.positionMillis ?: 0L
                        is MovieResumeLoadResult.Failure -> 0L
                    }
                } else {
                    0L
                }
                when (val result = XtreamPlaybackReferenceBuilder.build(account, selection)) {
                    is PlaybackReferenceResult.Ready -> {
                        PlaybackPreparation.Ready(
                            reference = result.reference,
                            cleartextConsent = account.cleartextConsent,
                            resumePositionMillis = resumePositionMillis,
                        )
                    }
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

    when (val state = preparation) {
        PlaybackPreparation.Loading -> PlaybackLoading(onBack)
        is PlaybackPreparation.Failure -> PlaybackFailure(
            message = state.reason.messageResource(),
            onRetry = { preparationAttempt++ },
            onBack = onBack,
        )
        is PlaybackPreparation.Ready -> PlayerSurface(
            selection = selection,
            reference = state.reference,
            cleartextConsent = state.cleartextConsent,
            accountStore = accountStore,
            resumeRepository = resumeRepository,
            previousLiveChannelController = previousLiveChannelController,
            previousLiveAvailable = previousLiveAvailable,
            onPreviousLive = onPreviousLive,
            resumePositionMillis = state.resumePositionMillis,
            onBack = onBack,
            catalogHistoryRepository = historyRepository,
        )
    }
}

@Composable
internal fun EpisodePlaybackScreen(
    selection: EpisodePlaybackSelection,
    accountStore: XtreamAccountStore,
    seriesRepository: SeriesDetailsRepository,
    resumeRepository: MovieResumeRepository,
    episodeResumeRepository: EpisodeResumeRepository,
    previousLiveChannelController: PreviousLiveChannelController,
    onBack: () -> Unit,
) {
    val gate = remember(selection.operationId) {
        PlaybackOperationGate().also(PlaybackOperationGate::activateDestination)
    }
    val playerIdentity = remember(selection) {
        PlaybackSelection(
            accountId = selection.accountId,
            accountGeneration = selection.accountGeneration,
            section = CatalogSection.Series,
            providerItemId = selection.providerEpisodeId,
            containerExtension = selection.containerExtension,
        )
    }
    var preparationAttempt by remember { mutableIntStateOf(0) }
    var preparation by remember(selection) {
        mutableStateOf<PlaybackPreparation>(PlaybackPreparation.Loading)
    }

    DisposableEffect(gate) { onDispose { gate.deactivateDestination() } }
    LaunchedEffect(selection, preparationAttempt) {
        preparation = PlaybackPreparation.Loading
        val owner = gate.begin(playerIdentity)
        val scoped = selection.atDestination(owner.destinationEpoch)
        val resumePosition = when (val loaded = episodeResumeRepository.load(scoped)) {
            is EpisodeResumeLoadResult.Ready -> loaded.record?.positionMillis ?: 0L
            EpisodeResumeLoadResult.Failure -> 0L
        }
        val committed = seriesRepository.commitIfEpisodeCurrent(
            accountId = scoped.accountId,
            accountGeneration = scoped.accountGeneration,
            seriesId = scoped.providerSeriesId,
            seriesGeneration = scoped.seriesGeneration,
            episodeId = scoped.providerEpisodeId,
            extension = scoped.containerExtension,
        ) {
            val account = accountStore.load()
            val result = if (account == null) {
                PlaybackReferenceResult.Failure(PlaybackReferenceFailure.AccountChanged)
            } else {
                XtreamPlaybackReferenceBuilder.buildEpisode(account, scoped)
            }
            when (result) {
                is PlaybackReferenceResult.Ready -> {
                    if (!gate.commit(owner, accountStore.load()) {
                        preparation = PlaybackPreparation.Ready(
                            reference = result.reference,
                            cleartextConsent = account!!.cleartextConsent,
                            resumePositionMillis = resumePosition,
                            episodeSelection = scoped,
                        )
                    }) {
                        gate.commitFailure(owner) {
                            preparation = PlaybackPreparation.Failure(PlaybackReferenceFailure.AccountChanged)
                        }
                    }
                }
                is PlaybackReferenceResult.Failure -> gate.commitFailure(owner) {
                    preparation = PlaybackPreparation.Failure(result.reason)
                }
            }
        }
        if (!committed) gate.commitFailure(owner) {
            preparation = PlaybackPreparation.Failure(PlaybackReferenceFailure.InvalidMetadata)
        }
    }

    when (val state = preparation) {
        PlaybackPreparation.Loading -> PlaybackLoading(onBack)
        is PlaybackPreparation.Failure -> PlaybackFailure(
            message = state.reason.messageResource(),
            onRetry = { preparationAttempt++ },
            onBack = onBack,
        )
        is PlaybackPreparation.Ready -> PlayerSurface(
            selection = playerIdentity,
            reference = state.reference,
            cleartextConsent = state.cleartextConsent,
            accountStore = accountStore,
            resumeRepository = resumeRepository,
            previousLiveChannelController = previousLiveChannelController,
            previousLiveAvailable = false,
            onPreviousLive = {},
            resumePositionMillis = state.resumePositionMillis,
            onBack = onBack,
            episodeSelection = state.episodeSelection,
            episodeResumeRepository = episodeResumeRepository,
        )
    }
}

@Composable
private fun PlayerSurface(
    selection: PlaybackSelection,
    reference: SecretPlaybackReference,
    cleartextConsent: Boolean,
    accountStore: XtreamAccountStore,
    resumeRepository: MovieResumeRepository,
    previousLiveChannelController: PreviousLiveChannelController,
    previousLiveAvailable: Boolean,
    onPreviousLive: () -> Unit,
    resumePositionMillis: Long,
    onBack: () -> Unit,
    episodeSelection: EpisodePlaybackSelection? = null,
    episodeResumeRepository: EpisodeResumeRepository? = null,
    catalogHistoryRepository: CatalogHistoryRepository? = null,
) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val displayLocale = LocalConfiguration.current.locales[0]
    val unknownAudio = stringResource(R.string.playback_unknown_audio)
    val unknownSubtitle = stringResource(R.string.playback_unknown_subtitle)
    val scope = rememberCoroutineScope()
    var retryAttempt by remember { mutableIntStateOf(0) }
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var playbackFailed by remember { mutableStateOf(false) }
    var audioTracks by remember { mutableStateOf(emptyList<EmbeddedTrackOption>()) }
    var subtitleTracks by remember { mutableStateOf(emptyList<EmbeddedTrackOption>()) }
    var audioAutomatic by remember { mutableStateOf(true) }
    var subtitleAutomatic by remember { mutableStateOf(true) }
    var subtitlesDisabled by remember { mutableStateOf(false) }
    var activeMenu by remember { mutableStateOf<TrackMenu?>(null) }
    var startPositionMillis by remember(reference) { mutableStateOf(resumePositionMillis) }
    var returnedFromBackground by remember(reference) { mutableStateOf(false) }
    var exitRequested by remember(reference) { mutableStateOf(false) }

    fun snapshot(current: ExoPlayer?): PlaybackProgressSnapshot? {
        if ((selection.section != CatalogSection.Movies && episodeSelection == null) || current == null) {
            return null
        }
        val duration = current.duration.takeIf { it != C.TIME_UNSET && it > 0L }
        val position = if (duration == null) {
            current.currentPosition.coerceAtLeast(0L)
        } else {
            current.currentPosition.coerceIn(0L, duration)
        }
        return PlaybackProgressSnapshot(position, duration)
    }

    fun persistImmediately(progress: PlaybackProgressSnapshot?) {
        progress ?: return
        scope.launch {
            if (episodeSelection != null && episodeResumeRepository != null) {
                episodeResumeRepository.saveImmediately(episodeSelection, progress.positionMillis, progress.durationMillis)
            } else {
                resumeRepository.saveImmediately(selection, progress.positionMillis, progress.durationMillis)
            }
        }
    }

    fun requestExit() {
        if (exitRequested) return
        exitRequested = true
        val current = player
        val progress = snapshot(current)
        current?.playWhenReady = false
        scope.launch {
            if (progress != null) {
                if (episodeSelection != null && episodeResumeRepository != null) {
                    episodeResumeRepository.saveImmediately(episodeSelection, progress.positionMillis, progress.durationMillis)
                } else {
                    resumeRepository.saveImmediately(selection, progress.positionMillis, progress.durationMillis)
                }
            }
            onBack()
        }
    }

    fun refreshTracks(current: ExoPlayer, tracks: Tracks = current.currentTracks) {
        val parameters = current.trackSelectionParameters
        audioTracks = tracks.supportedOptions(
            trackType = C.TRACK_TYPE_AUDIO,
            fallback = unknownAudio,
            displayLocale = displayLocale,
            parameters = parameters,
        )
        subtitleTracks = tracks.supportedOptions(
            trackType = C.TRACK_TYPE_TEXT,
            fallback = unknownSubtitle,
            displayLocale = displayLocale,
            parameters = parameters,
        )
        audioAutomatic = parameters.isAutomatic(C.TRACK_TYPE_AUDIO)
        subtitlesDisabled = C.TRACK_TYPE_TEXT in parameters.disabledTrackTypes
        subtitleAutomatic = !subtitlesDisabled && parameters.isAutomatic(C.TRACK_TYPE_TEXT)
    }

    fun select(trackType: Int, option: EmbeddedTrackOption?) {
        val current = player ?: return
        val builder = current.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(trackType, false)
            .clearOverridesOfType(trackType)
        option?.let {
            builder.addOverride(
                TrackSelectionOverride(it.group.mediaTrackGroup, listOf(it.trackIndex)),
            )
        }
        current.trackSelectionParameters = builder.build()
        refreshTracks(current)
        activeMenu = null
    }

    fun disableSubtitles() {
        val current = player ?: return
        current.trackSelectionParameters = current.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        refreshTracks(current)
        activeMenu = null
    }

    LaunchedEffect(player, selection, resumeRepository, episodeSelection, episodeResumeRepository) {
        while (true) {
            delay(10_000L)
            val current = player
            if (current?.isPlaying == true) {
                snapshot(current)?.let { progress ->
                    if (episodeSelection != null && episodeResumeRepository != null) {
                        episodeResumeRepository.checkpoint(episodeSelection, progress.positionMillis, progress.durationMillis)
                    } else {
                        resumeRepository.checkpoint(selection, progress.positionMillis, progress.durationMillis)
                    }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, reference, cleartextConsent, retryAttempt, catalogHistoryRepository) {
        var releasing = false
        var recordedStart = false
        fun releasePlayer() {
            val current = player
            val progress = snapshot(current)
            progress?.let { startPositionMillis = it.positionMillis }
            releasing = true
            player = null
            activeMenu = null
            audioTracks = emptyList()
            subtitleTracks = emptyList()
            current?.clearMediaItems()
            current?.release()
            persistImmediately(progress)
        }
        fun startPlayer() {
            if (player != null) return
            releasing = false
            playbackFailed = false
            player = createPlayer(
                context = context,
                reference = reference,
                cleartextConsent = cleartextConsent,
                initialPositionMillis = startPositionMillis,
                autoPlay = !returnedFromBackground,
                onReady = { current, position, autoPlay ->
                    val account = accountStore.load()
                    if (
                        account?.accountId == selection.accountId &&
                        account.generation == selection.accountGeneration
                    ) {
                        val duration = current.duration.takeIf { it != C.TIME_UNSET && it > 0L }
                        current.seekTo(MovieResumePresentation.resumePosition(position, duration))
                        previousLiveChannelController.recordSuccessfullyStarted(selection)
                        current.playWhenReady = autoPlay
                    } else {
                        playbackFailed = true
                    }
                },
                onPause = { current ->
                    if (!releasing && !exitRequested) persistImmediately(snapshot(current))
                },
                onPlaying = {
                    if (!recordedStart && catalogHistoryRepository != null && selection.section != CatalogSection.Series) {
                        recordedStart = true
                        scope.launch { catalogHistoryRepository.recordStarted(selection) }
                    }
                },
                onFailure = { playbackFailed = true },
                onTracksChanged = { current, tracks -> refreshTracks(current, tracks) },
            )
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> startPlayer()
                Lifecycle.Event.ON_STOP -> {
                    returnedFromBackground = true
                    releasePlayer()
                }
                Lifecycle.Event.ON_DESTROY -> releasePlayer()
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

    BackHandler {
        if (activeMenu != null) {
            activeMenu = null
        } else {
            requestExit()
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
            onClick = ::requestExit,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(WindowInsets.safeDrawing.asPaddingValues())
                .padding(12.dp),
        )
        if (player != null && !playbackFailed) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(WindowInsets.safeDrawing.asPaddingValues())
                    .padding(12.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (previousLiveAvailable) {
                    FocusVisibleButton(
                        label = stringResource(R.string.playback_previous_live),
                        onClick = onPreviousLive,
                        modifier = Modifier.testTag("playback-previous-live"),
                    )
                }
                FocusVisibleButton(
                    label = stringResource(R.string.playback_audio),
                    onClick = { activeMenu = TrackMenu.Audio },
                    modifier = Modifier.testTag("playback-audio"),
                )
                FocusVisibleButton(
                    label = stringResource(R.string.playback_subtitles),
                    onClick = { activeMenu = TrackMenu.Subtitles },
                    modifier = Modifier.testTag("playback-subtitles"),
                )
            }
        }
        if (player == null && !playbackFailed) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        if (playbackFailed) {
            PlaybackFailure(
                message = R.string.playback_error_failed,
                onRetry = { retryAttempt++ },
                onBack = ::requestExit,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }

    when (activeMenu) {
        TrackMenu.Audio -> TrackPickerDialog(
            title = stringResource(R.string.playback_audio),
            options = audioTracks,
            automaticSelected = audioAutomatic,
            includeOff = false,
            offSelected = false,
            noTracksMessage = stringResource(R.string.playback_no_audio),
            onAutomatic = { select(C.TRACK_TYPE_AUDIO, null) },
            onOff = {},
            onTrack = { select(C.TRACK_TYPE_AUDIO, it) },
            onDismiss = { activeMenu = null },
        )
        TrackMenu.Subtitles -> TrackPickerDialog(
            title = stringResource(R.string.playback_subtitles),
            options = subtitleTracks,
            automaticSelected = subtitleAutomatic,
            includeOff = true,
            offSelected = subtitlesDisabled,
            noTracksMessage = stringResource(R.string.playback_no_subtitles),
            onAutomatic = { select(C.TRACK_TYPE_TEXT, null) },
            onOff = ::disableSubtitles,
            onTrack = { select(C.TRACK_TYPE_TEXT, it) },
            onDismiss = { activeMenu = null },
        )
        null -> Unit
    }
}

@Composable
private fun TrackPickerDialog(
    title: String,
    options: List<EmbeddedTrackOption>,
    automaticSelected: Boolean,
    includeOff: Boolean,
    offSelected: Boolean,
    noTracksMessage: String,
    onAutomatic: () -> Unit,
    onOff: () -> Unit,
    onTrack: (EmbeddedTrackOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    val selectedSuffix = stringResource(R.string.playback_selected)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FocusVisibleButton(
                            label = selectionLabel(
                                stringResource(R.string.playback_automatic),
                                automaticSelected,
                                selectedSuffix,
                            ),
                            onClick = onAutomatic,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(firstFocus),
                        )
                    }
                    if (includeOff) {
                        item {
                            FocusVisibleButton(
                                label = selectionLabel(
                                    stringResource(R.string.playback_off),
                                    offSelected,
                                    selectedSuffix,
                                ),
                                onClick = onOff,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    itemsIndexed(options) { _, option ->
                        FocusVisibleButton(
                            label = selectionLabel(option.label, option.selected, selectedSuffix),
                            onClick = { onTrack(option) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (options.isEmpty()) {
                        item {
                            Text(
                                text = noTracksMessage,
                                modifier = Modifier.padding(vertical = 12.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
                FocusVisibleButton(
                    label = stringResource(R.string.playback_close),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    LaunchedEffect(Unit) { firstFocus.requestFocus() }
}

private fun createPlayer(
    context: android.content.Context,
    reference: SecretPlaybackReference,
    cleartextConsent: Boolean,
    initialPositionMillis: Long,
    autoPlay: Boolean,
    onReady: (ExoPlayer, Long, Boolean) -> Unit,
    onPause: (ExoPlayer) -> Unit,
    onPlaying: () -> Unit,
    onFailure: () -> Unit,
    onTracksChanged: (ExoPlayer, Tracks) -> Unit,
): ExoPlayer {
    val mediaSourceFactory = DefaultMediaSourceFactory(context)
        .setDataSourceFactory(BoundedRedirectDataSource.Factory(cleartextConsent))
    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(mediaSourceFactory)
        .build()
        .apply {
            addListener(
                object : Player.Listener {
                    private var readyDispatched = false

                    override fun onPlayerError(error: PlaybackException) {
                        onFailure()
                    }

                    override fun onTracksChanged(tracks: Tracks) {
                        onTracksChanged(this@apply, tracks)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY && !readyDispatched) {
                            readyDispatched = true
                            onReady(this@apply, initialPositionMillis, autoPlay)
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying && readyDispatched) onPlaying()
                    }

                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        if (!playWhenReady && readyDispatched) onPause(this@apply)
                    }
                },
            )
            setMediaItem(MediaItem.fromUri(reference.uri.toASCIIString()))
            playWhenReady = false
            prepare()
        }
}

private fun Tracks.supportedOptions(
    trackType: Int,
    fallback: String,
    displayLocale: Locale,
    parameters: TrackSelectionParameters,
): List<EmbeddedTrackOption> = groups
    .asSequence()
    .filter { it.type == trackType }
    .flatMap { group ->
        (0 until group.length).asSequence()
            .filter(group::isTrackSupported)
            .map { index ->
                val format = group.getTrackFormat(index)
                val explicitlySelected = parameters.overrides[group.mediaTrackGroup]
                    ?.trackIndices
                    ?.contains(index) == true
                EmbeddedTrackOption(
                    group = group,
                    trackIndex = index,
                    label = PlaybackTrackLabel.resolve(
                        language = format.language,
                        mediaLabel = format.label,
                        fallback = fallback,
                        displayLocale = displayLocale,
                    ),
                    selected = explicitlySelected,
                )
            }
    }
    .toList()

private fun TrackSelectionParameters.isAutomatic(trackType: Int): Boolean =
    trackType !in disabledTrackTypes && overrides.values.none { it.type == trackType }

private fun selectionLabel(label: String, selected: Boolean, selectedSuffix: String): String =
    if (selected) "$label — $selectedSuffix" else label

private data class EmbeddedTrackOption(
    val group: Tracks.Group,
    val trackIndex: Int,
    val label: String,
    val selected: Boolean,
)

private data class PlaybackProgressSnapshot(
    val positionMillis: Long,
    val durationMillis: Long?,
)

private enum class TrackMenu {
    Audio,
    Subtitles,
}

@Composable
private fun PlaybackLoading(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
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
    BackHandler(onBack = onBack)
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
        val resumePositionMillis: Long,
        val episodeSelection: EpisodePlaybackSelection? = null,
    ) : PlaybackPreparation

    data class Failure(val reason: PlaybackReferenceFailure) : PlaybackPreparation
}
