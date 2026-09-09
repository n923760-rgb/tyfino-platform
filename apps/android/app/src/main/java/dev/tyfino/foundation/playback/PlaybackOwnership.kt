package dev.tyfino.foundation.playback

import dev.tyfino.foundation.xtream.SavedXtreamAccount

internal data class PlaybackOperationOwner(
    val accountId: String,
    val accountGeneration: Long,
    val operationId: Long,
    val destinationEpoch: Long,
)

internal class PlaybackOperationGate {
    private var operationId = 0L
    private var destinationEpoch = 0L
    private var acceptingResults = false
    private var expectedAccountId: String? = null
    private var expectedAccountGeneration = -1L

    @Synchronized
    fun activateDestination() {
        acceptingResults = true
        destinationEpoch++
        operationId++
        expectedAccountId = null
        expectedAccountGeneration = -1L
    }

    @Synchronized
    fun begin(selection: PlaybackSelection): PlaybackOperationOwner {
        check(acceptingResults)
        operationId++
        expectedAccountId = selection.accountId
        expectedAccountGeneration = selection.accountGeneration
        return PlaybackOperationOwner(
            accountId = selection.accountId,
            accountGeneration = selection.accountGeneration,
            operationId = operationId,
            destinationEpoch = destinationEpoch,
        )
    }

    @Synchronized
    fun isCurrent(owner: PlaybackOperationOwner, activeAccount: SavedXtreamAccount?): Boolean =
        ownsOperation(owner) &&
            activeAccount?.accountId == owner.accountId &&
            activeAccount.generation == owner.accountGeneration

    private fun ownsOperation(owner: PlaybackOperationOwner): Boolean =
        acceptingResults &&
            expectedAccountId == owner.accountId &&
            expectedAccountGeneration == owner.accountGeneration &&
            operationId == owner.operationId &&
            destinationEpoch == owner.destinationEpoch

    @Synchronized
    fun commit(
        owner: PlaybackOperationOwner,
        activeAccount: SavedXtreamAccount?,
        publish: () -> Unit,
    ): Boolean {
        if (!isCurrent(owner, activeAccount)) return false
        publish()
        return true
    }

    @Synchronized
    fun commitFailure(
        owner: PlaybackOperationOwner,
        publish: () -> Unit,
    ): Boolean {
        if (!ownsOperation(owner)) return false
        publish()
        return true
    }

    @Synchronized
    fun invalidateSelection() {
        operationId++
        expectedAccountId = null
        expectedAccountGeneration = -1L
    }

    @Synchronized
    fun deactivateDestination() {
        acceptingResults = false
        destinationEpoch++
        operationId++
        expectedAccountId = null
        expectedAccountGeneration = -1L
    }
}
