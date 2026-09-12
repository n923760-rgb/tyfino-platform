package dev.tyfino.foundation.ui.screen

import dev.tyfino.foundation.xtream.SeriesDetailsCandidate
import dev.tyfino.foundation.xtream.SeriesEpisode
import dev.tyfino.foundation.xtream.SeriesSeason

/** Stable identities preserve selection and focus across a same-Series generation refresh. */
internal object SeriesPresentation {
    fun selectedSeason(
        details: SeriesDetailsCandidate,
        preferredSeason: Int?,
    ): Int? = preferredSeason?.takeIf { preferred ->
        details.seasons.any { it.seasonNumber == preferred }
    } ?: details.seasons.firstOrNull()?.seasonNumber

    fun episodesForSeason(details: SeriesDetailsCandidate, seasonNumber: Int?): List<SeriesEpisode> =
        if (seasonNumber == null) emptyList() else details.episodes.filter { it.seasonNumber == seasonNumber }

    fun displayName(details: SeriesDetailsCandidate, catalogName: String): String =
        details.summary.name?.takeIf(String::isNotBlank) ?: catalogName

    fun acceptedSeasonLabel(season: SeriesSeason): String? = season.displayLabel.takeIf(String::isNotBlank)

    fun playableMetadataAvailable(episode: SeriesEpisode): Boolean {
        val extension = episode.containerExtension ?: return false
        return extension.length in 1..12 && extension.all { it in 'a'..'z' || it in '0'..'9' }
    }
}
