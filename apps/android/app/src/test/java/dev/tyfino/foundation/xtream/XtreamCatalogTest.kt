package dev.tyfino.foundation.xtream

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XtreamCatalogTest {
    @Test
    fun requestBuilderUsesTheSixApprovedActionsAndEncodesCategoryId() {
        assertEquals("action=get_live_categories", categoriesQuery(CatalogSection.Live))
        assertEquals("action=get_vod_categories", categoriesQuery(CatalogSection.Movies))
        assertEquals("action=get_series_categories", categoriesQuery(CatalogSection.Series))
        assertEquals(
            "action=get_live_streams&category_id=news%20%2B%20%D8%B9%D8%B1%D8%A8%D9%8A",
            itemsQuery(CatalogSection.Live, "news + عربي"),
        )
        assertEquals("action=get_vod_streams&category_id=42", itemsQuery(CatalogSection.Movies, "42"))
        assertEquals("action=get_series&category_id=7", itemsQuery(CatalogSection.Series, "7"))
    }

    @Test
    fun categoryParserAcceptsUnknownFieldsAndSkipsOnlyMalformedEntries() {
        val result = parser().parseCategories(
            StringReader(
                """[
                    {"category_id":"10","category_name":"News","unknown":{"nested":true}},
                    {"category_id":11,"category_name":"رياضة"},
                    {"category_name":"Missing id"},
                    false
                ]""".trimIndent(),
            ),
        ) as CatalogResult.Success

        assertEquals(2, result.records.size)
        assertEquals(2, result.skippedEntries)
        assertEquals(CatalogCategory("10", "News", 0), result.records[0])
        assertEquals(CatalogCategory("11", "رياضة", 1), result.records[1])
    }

    @Test
    fun itemParserMapsEachSectionAndToleratesMissingOptionalFields() {
        val account = account()
        val cases = listOf(
            Triple(CatalogSection.Live, "live", """[{"stream_id":1,"name":"Channel","stream_icon":"https://cdn.example/live.png"}]"""),
            Triple(CatalogSection.Movies, "movie", """[{"stream_id":"2","name":"Movie","stream_icon":"https://cdn.example/movie.png","rating":8.5,"year":2026,"container_extension":"mkv"}]"""),
            Triple(CatalogSection.Series, "series", """[{"series_id":3,"name":"Series","cover":"https://cdn.example/series.png","releaseDate":"2025-01-01"}]"""),
        )

        cases.forEach { (section, artworkName, body) ->
            val result = parser().parseItems(
                StringReader(body),
                section,
                "category",
                CatalogArtworkPolicy(account),
            ) as CatalogResult.Success
            assertEquals(1, result.records.size)
            assertEquals("category", result.records.single().categoryId)
            assertEquals("https://cdn.example/$artworkName.png", result.records.single().artworkUrl)
        }
    }

    @Test
    fun movieAddDateRequiresPlausibleProviderTimestamp() {
        val result = parser().parseItems(StringReader("""[
            {"stream_id":1,"name":"Dated","added":"1790000000"},
            {"stream_id":2,"name":"Invalid","added":"123"},
            {"stream_id":3,"name":"Missing"}
        ]"""), CatalogSection.Movies, "category", CatalogArtworkPolicy(account())) as CatalogResult.Success
        assertEquals(1790000000L, result.records[0].addedAtEpochSeconds)
        assertNull(result.records[1].addedAtEpochSeconds)
        assertNull(result.records[2].addedAtEpochSeconds)
    }

    @Test
    fun parserRejectsInvalidTopLevelAndHardEntryLimitsWithoutPartialSuccess() {
        assertFailure(CatalogFailure.MalformedResponse, parser().parseCategories(StringReader("{}")))
        assertFailure(CatalogFailure.MalformedResponse, parser().parseCategories(StringReader("[")))
        val limited = XtreamCatalogParser(categoryLimit = 1, itemLimit = 1)
        assertFailure(
            CatalogFailure.ResponseTooLarge,
            limited.parseCategories(
                StringReader("""[{"category_id":"1","category_name":"One"},{"category_id":"2","category_name":"Two"}]"""),
            ),
        )
    }

    @Test
    fun parserBoundsNamesAndRejectsOversizedProviderIdentity() {
        val longName = "😀".repeat(513)
        val oversizedId = "x".repeat(257)
        val result = parser().parseCategories(
            StringReader(
                """[
                    {"category_id":"ok","category_name":"$longName"},
                    {"category_id":"$oversizedId","category_name":"Ignored"}
                ]""".trimIndent(),
            ),
        ) as CatalogResult.Success

        assertEquals(1, result.records.size)
        assertEquals(512, result.records.single().name.codePointCount(0, result.records.single().name.length))
        assertEquals(1, result.skippedEntries)
    }

    @Test
    fun artworkPolicyRejectsSecretBearingAndUnapprovedReferences() {
        val https = CatalogArtworkPolicy(account(cleartext = false))
        assertEquals("https://cdn.example/image.png", https.accept("https://cdn.example/image.png"))
        assertNull(https.accept("http://cdn.example/image.png"))
        assertNull(https.accept("file:///tmp/image.png"))
        assertNull(https.accept("https://user:pass@cdn.example/image.png"))
        assertNull(https.accept("https://cdn.example/image.png?username=value"))
        assertNull(https.accept("https://cdn.example/user-name/image.png"))
        assertNull(https.accept("https://cdn.example/pass-word/image.png"))
        val http = CatalogArtworkPolicy(account(cleartext = true))
        assertEquals("http://cdn.example/image.png", http.accept("http://cdn.example/image.png"))
    }

    @Test
    fun transportClassifiesResponsesAndNeverRetriesAutomatically() = runBlocking {
        val factory = FakeConnectionFactory(200, "[]")
        val api = HttpXtreamCatalogApi(
            networkAvailable = { true },
            connectionFactory = factory::open,
        )

        val result = api.categories(account(), CatalogSection.Live)

        assertTrue(result is CatalogResult.Success)
        assertEquals(1, factory.calls)
        assertEquals(false, factory.lastConnection?.instanceFollowRedirects)
        assertEquals(8_000, factory.lastConnection?.connectTimeout)
        assertEquals(60_000, factory.lastConnection?.readTimeout)
    }

    @Test
    fun transportEnforcesDecompressedBodyLimitAndFailureTaxonomy() = runBlocking {
        val tooLarge = api(FakeConnectionFactory(200, "[] "), responseLimit = 2)
            .categories(account(), CatalogSection.Live)
        assertFailure(CatalogFailure.ResponseTooLarge, tooLarge)

        val rejected = api(FakeConnectionFactory(401, ""))
            .categories(account(), CatalogSection.Live)
        assertFailure(CatalogFailure.AuthenticationRejected, rejected)

        val redirect = api(FakeConnectionFactory(302, ""))
            .categories(account(), CatalogSection.Live)
        assertFailure(CatalogFailure.UnsupportedResponse, redirect)

        val offline = HttpXtreamCatalogApi(
            networkAvailable = { false },
            connectionFactory = { error("must not connect") },
        ).categories(account(), CatalogSection.Live)
        assertFailure(CatalogFailure.NetworkUnavailable, offline)
    }

    @Test
    fun timeoutRemainsDistinct() = runBlocking {
        val connection = FakeConnection(200, "[]").apply {
            failure = SocketTimeoutException("bounded timeout")
        }
        val result = HttpXtreamCatalogApi(
            networkAvailable = { true },
            connectionFactory = { connection },
        ).categories(account(), CatalogSection.Live)

        assertFailure(CatalogFailure.Timeout, result)
    }

    private fun categoriesQuery(section: CatalogSection) =
        XtreamCatalogRequestBuilder.publicQuery(CatalogRequest.Categories(section))

    private fun itemsQuery(section: CatalogSection, categoryId: String) =
        XtreamCatalogRequestBuilder.publicQuery(CatalogRequest.Items(section, categoryId))

    private fun parser() = XtreamCatalogParser()

    private fun account(cleartext: Boolean = false) = SavedXtreamAccount(
        accountId = "account-id",
        generation = 4,
        endpoint = ProviderEndpoint(
            baseUrl = if (cleartext) "http://provider.example" else "https://provider.example",
            isCleartext = cleartext,
        ),
        username = "user-name",
        password = "pass-word",
        cleartextConsent = cleartext,
    )

    private fun api(factory: FakeConnectionFactory, responseLimit: Long = 32L * 1024L * 1024L) =
        HttpXtreamCatalogApi(
            networkAvailable = { true },
            connectionFactory = factory::open,
            responseLimitBytes = responseLimit,
        )

    private fun assertFailure(expected: CatalogFailure, actual: CatalogResult<*>) {
        assertEquals(CatalogResult.Failure(expected), actual)
    }

    private class FakeConnectionFactory(
        private val status: Int,
        private val body: String,
    ) {
        var calls = 0
        var lastConnection: FakeConnection? = null

        fun open(url: URL): HttpURLConnection {
            calls++
            return FakeConnection(status, body, url).also { lastConnection = it }
        }
    }

    private class FakeConnection(
        private val status: Int,
        private val body: String,
        url: URL = URL("https://provider.example/player_api.php"),
    ) : HttpURLConnection(url) {
        var failure: IOException? = null

        override fun connect() = Unit

        override fun disconnect() = Unit

        override fun usingProxy(): Boolean = false

        override fun getResponseCode(): Int = failure?.let { throw it } ?: status

        override fun getInputStream(): InputStream =
            failure?.let { throw it } ?: ByteArrayInputStream(body.toByteArray())
    }
}
