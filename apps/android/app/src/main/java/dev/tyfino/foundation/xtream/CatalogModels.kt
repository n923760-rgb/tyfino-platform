package dev.tyfino.foundation.xtream

internal enum class CatalogSection(
    val categoriesAction: String,
    val itemsAction: String,
    val itemIdField: String,
    val artworkField: String,
) {
    Live(
        categoriesAction = "get_live_categories",
        itemsAction = "get_live_streams",
        itemIdField = "stream_id",
        artworkField = "stream_icon",
    ),
    Movies(
        categoriesAction = "get_vod_categories",
        itemsAction = "get_vod_streams",
        itemIdField = "stream_id",
        artworkField = "stream_icon",
    ),
    Series(
        categoriesAction = "get_series_categories",
        itemsAction = "get_series",
        itemIdField = "series_id",
        artworkField = "cover",
    ),
}

internal sealed interface CatalogRequest {
    val section: CatalogSection

    data class Categories(override val section: CatalogSection) : CatalogRequest

    data class Items(
        override val section: CatalogSection,
        val categoryId: String,
    ) : CatalogRequest
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
    val addedAtEpochSeconds: Long? = null,
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
    data class Success<T>(
        val records: List<T>,
        val skippedEntries: Int,
    ) : CatalogResult<T>

    data class Failure(val reason: CatalogFailure) : CatalogResult<Nothing>
}
