package dev.tyfino.foundation.xtream

import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XtreamMovieDetailsTest {
    @Test
    fun requestBuilderUsesVodInfoAndEncodesOneBoundedId() {
        assertEquals("action=get_vod_info&vod_id=movie%20%2B%20%D8%B9%D8%B1%D8%A8%D9%8A", XtreamMovieDetailsRequestBuilder.publicQuery("movie + عربي"))
        assertNull(XtreamMovieDetailsRequestBuilder.publicQuery(" "))
        assertNull(XtreamMovieDetailsRequestBuilder.publicQuery("x".repeat(257)))
    }

    @Test
    fun parserAcceptsBoundedMetadataAndFiltersUnsafeArtwork() {
        val result = parse("""{
            "info": {
                "name": "Movie", "plot": "Story", "rating": 8.4, "duration": "01:42:00",
                "releasedate": "2025-01-01", "genre": "Drama", "cast": "A, B", "director": "C",
                "movie_image": "https://cdn.example/poster.jpg",
                "backdrop_path": "https://user:secret@cdn.example/backdrop.jpg"
            },
            "movie_data": {"stream_id": 42}
        }""")
        val details = (result as MovieDetailsResult.Success).details
        assertEquals("Movie", details.name)
        assertEquals("8.4", details.rating)
        assertEquals("https://cdn.example/poster.jpg", details.posterUrl)
        assertNull(details.backdropUrl)
    }

    @Test
    fun parserRejectsMismatchedIdentityMalformedShapeAndTrailingJson() {
        assertFailure(MovieDetailsFailure.UnsupportedResponse, parse("""{"movie_data":{"stream_id":"99"}}"""))
        assertFailure(MovieDetailsFailure.MalformedResponse, parse("[]"))
        assertFailure(MovieDetailsFailure.MalformedResponse, parse("{} {}"))
    }

    @Test
    fun parserDoesNotPersistCredentialBearingArtwork() {
        val result = parse("""{"info":{"movie_image":"https://cdn.example/credential-user/picture.jpg"}}""")
        assertNull((result as MovieDetailsResult.Success).details.posterUrl)
    }

    private fun parse(json: String): MovieDetailsResult {
        val account = SavedXtreamAccount(
            accountId = "account-a", generation = 1,
            endpoint = ProviderEndpoint("https://provider.example", false),
            username = "credential-user", password = "credential-password", cleartextConsent = false,
        )
        return XtreamMovieDetailsParser().parse(StringReader(json), account.accountId, "42", CatalogArtworkPolicy(account))
    }

    private fun assertFailure(expected: MovieDetailsFailure, result: MovieDetailsResult) {
        assertTrue(result is MovieDetailsResult.Failure)
        assertEquals(expected, (result as MovieDetailsResult.Failure).reason)
    }
}
