package dev.tyfino.foundation.xtream

internal data class MovieDetails(
    val accountId: String,
    val providerMovieId: String,
    val name: String?,
    val plot: String?,
    val genre: String?,
    val releaseDate: String?,
    val rating: String?,
    val duration: String?,
    val cast: String?,
    val director: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
)

internal enum class MovieDetailsFailure {
    NetworkUnavailable,
    Timeout,
    ProviderUnavailable,
    AuthenticationRejected,
    MalformedResponse,
    UnsupportedResponse,
    ResponseTooLarge,
    LocalStorage,
    Unknown,
}

internal sealed interface MovieDetailsResult {
    data class Success(val details: MovieDetails) : MovieDetailsResult
    data class Failure(val reason: MovieDetailsFailure) : MovieDetailsResult
}
