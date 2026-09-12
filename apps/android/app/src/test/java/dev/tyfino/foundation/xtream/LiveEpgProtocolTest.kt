package dev.tyfino.foundation.xtream

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.Base64
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveEpgProtocolTest {
    private fun parser(limit: Int = 100) =
        LiveEpgParser(entryLimit = limit, decodeBase64 = { Base64.getDecoder().decode(it) })

    @Test fun requestEncodesOnlyOneBoundedChannelAndLimit() {
        assertEquals(
            "action=get_short_epg&stream_id=stream%20%2B%20%D8%B9%D8%B1%D8%A8%D9%8A&limit=100",
            LiveEpgRequestBuilder.publicQuery("stream + عربي"),
        )
        assertNull(LiveEpgRequestBuilder.publicQuery(" "))
        assertNull(LiveEpgRequestBuilder.publicQuery("x".repeat(257)))
        assertNull(LiveEpgRequestBuilder.publicQuery("one", 101))
        assertNull(LiveEpgRequestBuilder.publicQuery("one", 0))
        assertFalse(LiveEpgRequestBuilder.publicQuery("one")!!.contains("password"))
    }

    @Test fun fixtureDecodesUnicodeAndOrdersNumericUtcTimestamps() {
        val arabic = Base64.getEncoder().encodeToString("البرنامج".toByteArray())
        val input = """
            {"epg_listings":[
              {"title":"$arabic","description":"TmV3cw==","start":"2099-01-01 00:00:00",
               "start_timestamp":"1704112200","stop_timestamp":1704114000},
              {"title":"Sm91cm5hbA==","start_timestamp":1704110400,"stop_timestamp":"1704112200"},
              {"title":"Sm91cm5hbA==","start_timestamp":1704110400,"stop_timestamp":"1704112200"}
            ]}
        """.trimIndent()
        val result = parser().parse(StringReader(input), "account", "channel") as LiveEpgResult.Success
        assertEquals(2, result.programs.size)
        assertEquals(1, result.skippedEntries)
        assertEquals("Journal", result.programs.first().title)
        assertEquals("البرنامج", result.programs.last().title)
        assertEquals("News", result.programs.last().description)
        assertEquals(1_704_110_400_000L, result.programs.first().startEpochMillis)
        assertEquals("account", result.programs.first().accountId)
        assertEquals("channel", result.programs.first().channelId)
    }

    @Test fun rejectsAmbiguousOrInvalidTimesWithoutGuessingTimezone() {
        val body = """{"epg_listings":[
            {"title":"News","start":"2024-01-01 12:00:00","end":"2024-01-01 13:00:00"},
            {"start_timestamp":"1704110400.0","stop_timestamp":"1704112200"},
            {"start_timestamp":"1704112200","stop_timestamp":"1704110400"},
            {"start_timestamp":"9999999999","stop_timestamp":"10000000000"},
            {"start_timestamp":"1704110400","stop_timestamp":"1704112200"}
        ]}"""
        val result = parser().parse(StringReader(body), "a", "c") as LiveEpgResult.Success
        assertEquals(1, result.programs.size)
        assertEquals(4, result.skippedEntries)
    }

    @Test fun malformedContainersAndEntryHardLimitNeverYieldPartialSuccess() {
        assertEquals(LiveEpgResult.Failure(LiveEpgFailure.MalformedResponse),
            parser().parse(StringReader("{}"), "a", "c"))
        assertEquals(LiveEpgResult.Failure(LiveEpgFailure.MalformedResponse),
            parser().parse(StringReader("""{"epg_listings":[]} {}"""), "a", "c"))
        assertEquals(LiveEpgResult.Failure(LiveEpgFailure.MalformedResponse),
            parser().parse(StringReader("""{"epg_listings":{} }"""), "a", "c"))
        assertEquals(LiveEpgResult.Failure(LiveEpgFailure.ResponseTooLarge),
            parser(1).parse(StringReader("""{"epg_listings":[{},{}]}"""), "a", "c"))
    }

    @Test fun rejectsOversizedTextButRetainsSafeProgramAndBoundsIdentity() {
        val raw = "😀".repeat(513)
        val result = parser().parse(
            StringReader("""{"epg_listings":[{"title":"$raw","start_timestamp":"1704110400",
                "stop_timestamp":"1704112200"}]}"""),
            "a", "c",
        ) as LiveEpgResult.Success
        assertNull(result.programs.single().title)
        assertEquals(LiveEpgResult.Failure(LiveEpgFailure.UnsupportedResponse),
            parser().parse(StringReader("""{"epg_listings":[]}"""), "a", "x".repeat(257)))
    }

    @Test fun transportRejectsRedirectAndCleartextAndNeverRetries() = runBlocking {
        val factory = FakeFactory(200, """{"epg_listings":[]}""".toByteArray())
        val api = HttpLiveEpgApi({ true }, factory::open)
        assertTrue(api.shortGuide(account(), "live") is LiveEpgResult.Success)
        assertEquals(1, factory.calls)
        assertTrue(factory.lastUrl.toString().contains("action=get_short_epg&stream_id=live&limit=100"))
        assertEquals(false, factory.lastConnection?.instanceFollowRedirects)
        assertEquals(8_000, factory.lastConnection?.connectTimeout)
        assertEquals(60_000, factory.lastConnection?.readTimeout)
        assertEquals(LiveEpgResult.Failure(LiveEpgFailure.UnsupportedResponse),
            HttpLiveEpgApi({ true }, FakeFactory(302, byteArrayOf())::open).shortGuide(account(), "live"))
        assertEquals(LiveEpgResult.Failure(LiveEpgFailure.UnsupportedResponse),
            api.shortGuide(account(cleartext = true, consent = false), "live"))
        assertEquals(LiveEpgResult.Failure(LiveEpgFailure.NetworkUnavailable),
            HttpLiveEpgApi({ false }, { error("no request") }).shortGuide(account(), "live"))
    }

    @Test fun gzipDecompressedLimitAndTimeoutAreCategorized() = runBlocking {
        val compressed = ByteArrayOutputStream().also { output ->
            GZIPOutputStream(output).use { it.write("""{"epg_listings":[]}""".toByteArray()) }
        }.toByteArray()
        val factory = FakeFactory(200, compressed, encoding = "gzip")
        assertTrue(HttpLiveEpgApi({ true }, factory::open).shortGuide(account(), "live")
            is LiveEpgResult.Success)
        assertEquals(LiveEpgResult.Failure(LiveEpgFailure.ResponseTooLarge),
            HttpLiveEpgApi({ true }, factory::open, responseLimitBytes = 10)
                .shortGuide(account(), "live"))
        val timeout = FakeFactory(200, byteArrayOf()).apply { failure = SocketTimeoutException() }
        assertEquals(LiveEpgResult.Failure(LiveEpgFailure.Timeout),
            HttpLiveEpgApi({ true }, timeout::open).shortGuide(account(), "live"))
    }

    private fun account(cleartext: Boolean = false, consent: Boolean = cleartext) =
        SavedXtreamAccount("account", 1,
            ProviderEndpoint(if (cleartext) "http://provider.example" else "https://provider.example", cleartext),
            "user", "pass", consent)

    private class FakeFactory(
        private val status: Int,
        private val body: ByteArray,
        private val encoding: String? = null,
    ) {
        var calls = 0
        lateinit var lastUrl: URL
        var lastConnection: FakeConnection? = null
        var failure: IOException? = null
        fun open(url: URL): HttpURLConnection {
            calls++
            lastUrl = url
            return FakeConnection(url, status, body, encoding, failure).also { lastConnection = it }
        }
    }

    private class FakeConnection(
        url: URL,
        private val status: Int,
        private val body: ByteArray,
        private val encoding: String?,
        private val failure: IOException?,
    ) : HttpURLConnection(url) {
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy() = false
        override fun getResponseCode(): Int = failure?.let { throw it } ?: status
        override fun getInputStream(): InputStream = ByteArrayInputStream(body)
        override fun getContentEncoding(): String? = encoding
    }
}
