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

class XtreamSeriesTest {
    @Test
    fun requestBuilderUsesApprovedActionAndEncodesOneBoundedSeriesId() {
        assertEquals(
            "action=get_series_info&series_id=show%20%2B%20%D8%B9%D8%B1%D8%A8%D9%8A",
            XtreamSeriesRequestBuilder.publicQuery("show + عربي"),
        )
        assertNull(XtreamSeriesRequestBuilder.publicQuery(" "))
        assertNull(XtreamSeriesRequestBuilder.publicQuery("x".repeat(257)))
    }

    @Test
    fun parserUsesEpisodesAsAuthorityWhenOptionalSeasonsAreMissing() {
        val result = parse(
            """{
                "info": {
                    "name": "A Series",
                    "rating": 8.5,
                    "cover": "https://cdn.example/cover.jpg",
                    "backdrop_path": ["https://cdn.example/backdrop.jpg"]
                },
                "episodes": {
                    "1": [
                        {
                            "id": 101,
                            "episode_num": "1",
                            "title": "Pilot",
                            "container_extension": "MKV",
                            "info": {"plot": "Opening", "duration": "00:42:00"}
                        }
                    ]
                }
            }""".trimIndent(),
        )

        val details = success(result)
        assertEquals("A Series", details.summary.name)
        assertEquals("8.5", details.summary.rating)
        assertEquals("https://cdn.example/backdrop.jpg", details.summary.backdropUrl)
        assertEquals(1, details.seasons.size)
        assertEquals("1", details.seasons.single().displayLabel)
        assertEquals(1, details.seasons.single().episodeCount)
        assertEquals("101", details.episodes.single().providerEpisodeId)
        assertEquals("mkv", details.episodes.single().containerExtension)
        assertEquals("Opening", details.episodes.single().plot)
    }

    @Test
    fun parserMergesAdvisorySeasonMetadataAndOrdersEpisodesDeterministically() {
        val result = parse(
            """{
                "seasons": [
                    {"season_number": "2", "name": "Second", "overview": "Overview"},
                    {"season_number": 1, "name": "First"},
                    {"name": "Malformed"}
                ],
                "episodes": {
                    "2": [
                        {"id": "later", "episode_num": 2, "container_extension": "mp4"},
                        {"id": "earlier", "episode_num": 1, "container_extension": "mp4"}
                    ],
                    "1": [{"id": "first", "episode_num": 3, "container_extension": "ts"}]
                }
            }""".trimIndent(),
        )

        val details = success(result)
        assertEquals(listOf(1, 2), details.seasons.map(SeriesSeason::seasonNumber))
        assertEquals("Second", details.seasons.last().displayLabel)
        assertEquals("Overview", details.seasons.last().overview)
        assertEquals(listOf("first", "earlier", "later"), details.episodes.map(SeriesEpisode::providerEpisodeId))
        assertEquals(1, details.skippedEntries)
    }

    @Test
    fun parserKeepsSafeEpisodeWhenExtensionIsUnavailableAndCountsConflictsAndDuplicates() {
        val result = parse(
            """{
                "episodes": {
                    "1": [
                        {"id": "same", "season": 2, "episode_num": 1, "title": "Visible", "container_extension": "../mkv"},
                        {"id": "same", "episode_num": 2, "container_extension": "mp4"},
                        {"id": true, "episode_num": 3, "container_extension": "mp4"},
                        {"id": "fraction", "episode_num": 1.5, "container_extension": "mp4"}
                    ]
                }
            }""".trimIndent(),
        )

        val details = success(result)
        assertEquals(1, details.episodes.size)
        assertEquals("Visible", details.episodes.single().title)
        assertNull(details.episodes.single().containerExtension)
        assertEquals(3, details.skippedEntries)
        assertEquals(1, details.seasonMismatchCount)
    }

    @Test
    fun parserRejectsMissingOrInvalidEpisodesObjectAndTrailingJson() {
        assertFailure(SeriesFailure.MalformedResponse, parse("{}"))
        assertFailure(SeriesFailure.MalformedResponse, parse("""{"episodes": []}"""))
        assertFailure(SeriesFailure.MalformedResponse, parse("""{"episodes": {}} {}"""))
    }

    @Test
    fun parserEnforcesSeasonAndEpisodeEntryLimitsWithoutPartialSuccess() {
        val seasonLimited = XtreamSeriesParser(seasonGroupLimit = 1, episodeLimit = 10)
        assertFailure(
            SeriesFailure.ResponseTooLarge,
            parse("""{"episodes":{"1":[],"2":[]}}""", seasonLimited),
        )
        assertFailure(
            SeriesFailure.ResponseTooLarge,
            parse(
                """{"episodes":{"1":[{"id":"1"},{"id":"2"}]}}""",
                XtreamSeriesParser(seasonGroupLimit = 10, episodeLimit = 1),
            ),
        )
        assertFailure(
            SeriesFailure.ResponseTooLarge,
            parse(
                """{"seasons":[{"season_number":1},{"season_number":2}],"episodes":{}}""",
                seasonLimited,
            ),
        )
    }

    @Test
    fun parserBoundsDisplayTextAndRejectsUnsafeNumericIdentityCoercion() {
        val longName = "😀".repeat(513)
        val result = parse(
            """{
                "info":{"name":"$longName"},
                "episodes":{"0":[
                    {"id":1.0,"episode_num":"0","container_extension":"TS"},
                    {"id":1.5,"episode_num":1,"container_extension":"ts"},
                    {"id":1e100,"episode_num":2,"container_extension":"ts"}
                ]}
            }""".trimIndent(),
        )

        val details = success(result)
        assertEquals(512, details.summary.name!!.codePointCount(0, details.summary.name.length))
        assertEquals(listOf("1"), details.episodes.map(SeriesEpisode::providerEpisodeId))
        assertEquals(2, details.skippedEntries)
    }

    @Test
    fun transportIsBoundedClassifiesFailuresAndNeverRetries() = runBlocking {
        val factory = FakeConnectionFactory(200, """{"episodes":{}}""")
        val api = HttpXtreamSeriesApi(
            networkAvailable = { true },
            connectionFactory = factory::open,
        )

        assertTrue(api.details(account(), "series") is SeriesDetailsResult.Success)
        assertEquals(1, factory.calls)
        assertEquals(false, factory.lastConnection?.instanceFollowRedirects)
        assertEquals(8_000, factory.lastConnection?.connectTimeout)
        assertEquals(60_000, factory.lastConnection?.readTimeout)
        assertTrue(factory.lastUrl.toString().contains("action=get_series_info&series_id=series"))

        val tooLarge = HttpXtreamSeriesApi(
            networkAvailable = { true },
            connectionFactory = FakeConnectionFactory(200, """{"episodes":{}}""")::open,
            responseLimitBytes = 2,
        ).details(account(), "series")
        assertFailure(SeriesFailure.ResponseTooLarge, tooLarge)

        assertFailure(
            SeriesFailure.AuthenticationRejected,
            api(FakeConnectionFactory(401, "")).details(account(), "series"),
        )
        assertFailure(
            SeriesFailure.UnsupportedResponse,
            api(FakeConnectionFactory(302, "")).details(account(), "series"),
        )
        assertFailure(
            SeriesFailure.ProviderUnavailable,
            api(FakeConnectionFactory(503, "")).details(account(), "series"),
        )
        assertFailure(
            SeriesFailure.NetworkUnavailable,
            HttpXtreamSeriesApi(
                networkAvailable = { false },
                connectionFactory = { error("must not connect") },
            ).details(account(), "series"),
        )
    }

    @Test
    fun transportKeepsTimeoutDistinctAndRejectsUnapprovedCleartext() = runBlocking {
        val connection = FakeConnection(200, "{}").apply {
            failure = SocketTimeoutException("bounded timeout")
        }
        assertFailure(
            SeriesFailure.Timeout,
            HttpXtreamSeriesApi(
                networkAvailable = { true },
                connectionFactory = { connection },
            ).details(account(), "series"),
        )
        assertFailure(
            SeriesFailure.UnsupportedResponse,
            api(FakeConnectionFactory(200, "{}"))
                .details(account(cleartext = true, cleartextConsent = false), "series"),
        )
    }

    private fun parse(
        body: String,
        parser: XtreamSeriesParser = XtreamSeriesParser(),
    ): SeriesDetailsResult = parser.parse(
        reader = StringReader(body),
        accountId = "account-id",
        seriesId = "series-id",
        artworkPolicy = CatalogArtworkPolicy(account()),
    )

    private fun success(result: SeriesDetailsResult): SeriesDetailsCandidate =
        (result as SeriesDetailsResult.Success).details

    private fun assertFailure(expected: SeriesFailure, actual: SeriesDetailsResult) {
        assertEquals(SeriesDetailsResult.Failure(expected), actual)
    }

    private fun account(
        cleartext: Boolean = false,
        cleartextConsent: Boolean = cleartext,
    ) = SavedXtreamAccount(
        accountId = "account-id",
        generation = 4,
        endpoint = ProviderEndpoint(
            baseUrl = if (cleartext) "http://provider.example" else "https://provider.example",
            isCleartext = cleartext,
        ),
        username = "user-name",
        password = "pass-word",
        cleartextConsent = cleartextConsent,
    )

    private fun api(factory: FakeConnectionFactory) = HttpXtreamSeriesApi(
        networkAvailable = { true },
        connectionFactory = factory::open,
    )

    private class FakeConnectionFactory(
        private val status: Int,
        private val body: String,
    ) {
        var calls = 0
        var lastConnection: FakeConnection? = null
        lateinit var lastUrl: URL

        fun open(url: URL): HttpURLConnection {
            calls++
            lastUrl = url
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
