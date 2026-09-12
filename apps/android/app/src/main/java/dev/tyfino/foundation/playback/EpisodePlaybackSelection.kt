package dev.tyfino.foundation.playback

import dev.tyfino.foundation.xtream.SeriesEpisode
import java.util.UUID

/** Contains identities only. A credential-bearing playback URL is never placed in navigation state. */
internal data class EpisodePlaybackSelection(
    val accountId: String,
    val accountGeneration: Long,
    val providerSeriesId: String,
    val seriesGeneration: Long,
    val providerEpisodeId: String,
    val containerExtension: String,
    val operationId: String = UUID.randomUUID().toString(),
    val playbackDestinationEpoch: Long = 0L,
) {
    fun atDestination(epoch: Long): EpisodePlaybackSelection = copy(playbackDestinationEpoch = epoch)

    override fun toString(): String = "[episode playback selection]"

    companion object {
        fun from(
            accountId: String,
            accountGeneration: Long,
            seriesId: String,
            seriesGeneration: Long,
            episode: SeriesEpisode,
        ): EpisodePlaybackSelection? {
            val extension = episode.containerExtension ?: return null
            if (episode.accountId != accountId || episode.providerSeriesId != seriesId ||
                seriesGeneration <= 0L || !SAFE_EXTENSION.matches(extension)
            ) return null
            return EpisodePlaybackSelection(
                accountId, accountGeneration, seriesId, seriesGeneration,
                episode.providerEpisodeId, extension,
            )
        }

        private val SAFE_EXTENSION = Regex("[a-z0-9]{1,12}")
    }
}
