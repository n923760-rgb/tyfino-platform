package dev.tyfino.foundation.xtream

internal enum class CatalogSection(
    val categoriesAction: String,
    val itemsAction: String,
    val itemIdField: String,
    val artworkField: String,
) {
    Live("get_live_categories", "get_live_streams", "stream_id", "stream_icon"),
    Movies("get_vod_categories", "get_vod_streams", "stream_id", "stream_icon"),
    Series("get_series_categories", "get_series", "series_id", "cover"),
}

internal sealed interface CatalogRequest {
    val section: CatalogSection
    data class Categories(override val section: CatalogSection) : CatalogRequest
    data class Items(override val section: CatalogSection, val categoryId: String) : CatalogRequest
}

internal data class CatalogCategory(
    val providerId: String,
    val name: String,
    val providerOrder: Int,
)

internal data class CatalogItem(
    val providerId: String,
    val categoryId: String,
    val name: String,
    val providerOrder: Int,
    val artworkUrl: String?,
    val rating: String?,
    val releaseYear: String?,
    val containerExtension: String?,
)

internal enum class CatalogFailure {
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

internal sealed interface CatalogResult<out T> {
    data class Success<T>(val records: List<T>, val skippedEntries: Int) : CatalogResult<T>
    data class Failure(val reason: CatalogFailure) : CatalogResult<Nothing>
}
