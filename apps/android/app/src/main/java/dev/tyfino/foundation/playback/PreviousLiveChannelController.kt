package dev.tyfino.foundation.playback

import dev.tyfino.foundation.xtream.CatalogSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class PreviousLiveChannelState(
    val accountId: String? = null,
    val accountGeneration: Long? = null,
    val currentProviderItemId: String? = null,
    val previousProviderItemId: String? = null,
    val version: Long = 0L,
)

internal data class PreviousLiveChannelRequest(
    val accountId: String,
    val accountGeneration: Long,
    val sourceProviderItemId: String,
    val targetProviderItemId: String,
    val version: Long,
)

internal class PreviousLiveChannelController {
    private val mutableState = MutableStateFlow(PreviousLiveChannelState())
    val state: StateFlow<PreviousLiveChannelState> = mutableState.asStateFlow()

    @Synchronized
    fun recordSuccessfullyStarted(selection: PlaybackSelection): Boolean {
        if (!selection.isValidLiveSelection()) return false
        val current = mutableState.value
        val sameOwner = current.accountId == selection.accountId &&
            current.accountGeneration == selection.accountGeneration
        val next = when {
            !sameOwner -> PreviousLiveChannelState(
                accountId = selection.accountId,
                accountGeneration = selection.accountGeneration,
                currentProviderItemId = selection.providerItemId,
                version = current.version + 1L,
            )
            current.currentProviderItemId == selection.providerItemId -> current
            else -> current.copy(
                currentProviderItemId = selection.providerItemId,
                previousProviderItemId = current.currentProviderItemId,
                version = current.version + 1L,
            )
        }
        mutableState.value = next
        return true
    }

    @Synchronized
    fun beginPrevious(selection: PlaybackSelection): PreviousLiveChannelRequest? {
        if (!selection.isValidLiveSelection()) return null
        val current = mutableState.value
        val target = current.previousProviderItemId ?: return null
        if (
            current.accountId != selection.accountId ||
            current.accountGeneration != selection.accountGeneration ||
            current.currentProviderItemId != selection.providerItemId
        ) return null
        return PreviousLiveChannelRequest(
            accountId = selection.accountId,
            accountGeneration = selection.accountGeneration,
            sourceProviderItemId = selection.providerItemId,
            targetProviderItemId = target,
            version = current.version,
        )
    }

    @Synchronized
    fun isCurrent(request: PreviousLiveChannelRequest): Boolean {
        val current = mutableState.value
        return current.accountId == request.accountId &&
            current.accountGeneration == request.accountGeneration &&
            current.currentProviderItemId == request.sourceProviderItemId &&
            current.previousProviderItemId == request.targetProviderItemId &&
            current.version == request.version
    }

    @Synchronized
    fun clear() {
        mutableState.value = PreviousLiveChannelState(version = mutableState.value.version + 1L)
    }

    private fun PlaybackSelection.isValidLiveSelection(): Boolean =
        section == CatalogSection.Live &&
            accountId.isNotBlank() &&
            accountGeneration >= 0L &&
            providerItemId.isNotBlank() &&
            providerItemId.codePointCount(0, providerItemId.length) <= MAX_PROVIDER_ID_CODE_POINTS

    private companion object {
        const val MAX_PROVIDER_ID_CODE_POINTS = 256
    }
}
