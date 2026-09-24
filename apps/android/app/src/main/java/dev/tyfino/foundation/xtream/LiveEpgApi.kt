package dev.tyfino.foundation.xtream

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Provider access only; callers must revalidate account/channel ownership before publication. */
internal interface LiveEpgApi {
    suspend fun shortGuide(account: SavedXtreamAccount, channelId: String): LiveEpgResult
}

internal class HttpLiveEpgApi(
    private val networkAvailable: () -> Boolean,
    private val connectionFactory: (URL) -> HttpURLConnection,
    private val parser: LiveEpgParser = LiveEpgParser(),
    private val responseLimitBytes: Long = MAX_RESPONSE_BYTES,
    private val userAgent: () -> String = { ProviderUserAgentPreset.Tyfino.header() },
) : LiveEpgApi {
    constructor(context: Context) : this(
        networkAvailable = {
            val manager = context.getSystemService(ConnectivityManager::class.java)
            val network = manager.activeNetwork
            val capabilities = network?.let(manager::getNetworkCapabilities)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        },
        connectionFactory = { url -> url.openConnection() as HttpURLConnection },
        userAgent = { ProviderUserAgent(context).header() },
    )

    override suspend fun shortGuide(account: SavedXtreamAccount, channelId: String): LiveEpgResult =
        withContext(Dispatchers.IO) {
            if (!networkAvailable()) return@withContext failure(LiveEpgFailure.NetworkUnavailable)
            if (account.endpoint.isCleartext && !account.cleartextConsent) {
                return@withContext failure(LiveEpgFailure.UnsupportedResponse)
            }
            if (responseLimitBytes !in 1..MAX_RESPONSE_BYTES) {
                return@withContext failure(LiveEpgFailure.UnsupportedResponse)
            }
            val secretUrl = LiveEpgRequestBuilder.build(account, channelId)
                ?: return@withContext failure(LiveEpgFailure.UnsupportedResponse)
            val connection = try {
                connectionFactory(URI(secretUrl).toURL())
            } catch (_: IllegalArgumentException) {
                return@withContext failure(LiveEpgFailure.UnsupportedResponse)
            } catch (_: IOException) {
                return@withContext failure(LiveEpgFailure.ProviderUnavailable)
            }
            try {
                configure(connection)
                when (connection.responseCode) {
                    HttpURLConnection.HTTP_UNAUTHORIZED,
                    HttpURLConnection.HTTP_FORBIDDEN -> failure(LiveEpgFailure.AuthenticationRejected)
                    in 200..299 -> {
                        val decoded = decode(connection.inputStream, connection.contentEncoding)
                        val limited = CatalogLimitInputStream(decoded, responseLimitBytes)
                        InputStreamReader(limited, StandardCharsets.UTF_8).use { reader ->
                            parser.parse(reader, account.accountId, channelId)
                        }
                    }
                    in 500..599 -> failure(LiveEpgFailure.ProviderUnavailable)
                    else -> failure(LiveEpgFailure.UnsupportedResponse)
                }
            } catch (_: SocketTimeoutException) {
                failure(LiveEpgFailure.Timeout)
            } catch (_: CatalogResponseTooLargeException) {
                failure(LiveEpgFailure.ResponseTooLarge)
            } catch (_: IOException) {
                failure(if (networkAvailable()) LiveEpgFailure.ProviderUnavailable else LiveEpgFailure.NetworkUnavailable)
            } finally {
                connection.disconnect()
            }
        }

    private fun configure(connection: HttpURLConnection) {
        connection.requestMethod = "GET"
        connection.connectTimeout = 8_000
        connection.readTimeout = 60_000
        connection.instanceFollowRedirects = false
        connection.useCaches = false
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", userAgent())
        connection.setRequestProperty("Accept-Encoding", "gzip, identity")
    }

    private fun decode(input: InputStream, encoding: String?): InputStream =
        if (encoding.equals("gzip", ignoreCase = true)) GZIPInputStream(input) else input

    private fun failure(reason: LiveEpgFailure) = LiveEpgResult.Failure(reason)

    private companion object { const val MAX_RESPONSE_BYTES = 1L * 1024L * 1024L }
}

internal object LiveEpgRequestBuilder {
    fun build(account: SavedXtreamAccount, channelId: String): String? {
        val query = publicQuery(channelId) ?: return null
        return buildString {
            append(account.endpoint.baseUrl)
            append("/player_api.php?username=")
            append(encode(account.username))
            append("&password=")
            append(encode(account.password))
            append('&')
            append(query)
        }
    }

    fun publicQuery(channelId: String, limit: Int = 100): String? {
        if (channelId.isBlank() || channelId.codePointCount(0, channelId.length) > 256 ||
            limit !in 1..100
        ) return null
        return "action=get_short_epg&stream_id=${encode(channelId)}&limit=$limit"
    }

    private fun encode(value: String) =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20")
}
