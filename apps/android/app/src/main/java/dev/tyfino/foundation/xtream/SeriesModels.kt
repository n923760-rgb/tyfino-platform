package dev.tyfino.foundation.xtream

internal data class SeriesDetailsCandidate(
    val accountId: String,
    val providerSeriesId: String,
    val summary: SeriesSummary,
    val seasons: List<SeriesSeason>,
    val episodes: List<SeriesEpisode>,
    val skippedEntries: Int,
    val seasonMismatchCount: Int,
)

internal data class SeriesSummary(
    val name: String?,
    val plot: String?,
    val genre: String?,
    val releaseDate: String?,
    val rating: String?,
    val cast: String?,
    val director: String?,
    val coverUrl: String?,
    val backdropUrl: String?,
) {
    companion object {
        val EMPTY = SeriesSummary(null, null, null, null, null, null, null, null, null)
    }
}

internal data class SeriesSeason(
    val accountId: String,
    val providerSeriesId: String,
    val seasonNumber: Int,
    val displayLabel: String,
    val overview: String?,
    val airDate: String?,
    val coverUrl: String?,
    val providerOrder: Int,
    val episodeCount: Int,
)

internal data class SeriesEpisode(
    val accountId: String,
    val providerSeriesId: String,
    val providerEpisodeId: String,
    val seasonNumber: Int,
    val episodeNumber: Int?,
    val title: String?,
    val containerExtension: String?,
    val providerOrder: Int,
    val plot: String?,
    val duration: String?,
    val releaseDate: String?,
    val rating: String?,
)

internal enum class SeriesFailure {
    NetworkUnavailable,
    Timeout,
    ProviderUnavailable,
    AuthenticationRejected,
    MalformedResponse,
    UnsupportedResponse,
    ResponseTooLarge,
    Unknown,
}

internal sealed interface SeriesDetailsResult {
    data class Success(val details: SeriesDetailsCandidate) : SeriesDetailsResult

    data class Failure(val reason: SeriesFailure) : SeriesDetailsResult
}
