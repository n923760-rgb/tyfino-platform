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

internal interface XtreamMovieDetailsApi {
    suspend fun details(account: SavedXtreamAccount, movieId: String): MovieDetailsResult
}

internal class HttpXtreamMovieDetailsApi(
    private val networkAvailable: () -> Boolean,
    private val connectionFactory: (URL) -> HttpURLConnection,
    private val parser: XtreamMovieDetailsParser = XtreamMovieDetailsParser(),
    private val responseLimitBytes: Long = MAX_RESPONSE_BYTES,
    private val userAgent: () -> String = { ProviderUserAgentPreset.Tyfino.header() },
) : XtreamMovieDetailsApi {
    constructor(context: Context) : this(
        networkAvailable = {
            val manager = context.getSystemService(ConnectivityManager::class.java)
            val network = manager.activeNetwork
            network?.let(manager::getNetworkCapabilities)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        },
        connectionFactory = { it.openConnection() as HttpURLConnection },
        userAgent = { ProviderUserAgent(context).header() },
    )

    override suspend fun details(account: SavedXtreamAccount, movieId: String): MovieDetailsResult =
        withContext(Dispatchers.IO) {
            if (!networkAvailable()) return@withContext failure(MovieDetailsFailure.NetworkUnavailable)
            if (account.endpoint.isCleartext && !account.cleartextConsent) {
                return@withContext failure(MovieDetailsFailure.UnsupportedResponse)
            }
            val secretUrl = XtreamMovieDetailsRequestBuilder.build(account, movieId)
                ?: return@withContext failure(MovieDetailsFailure.UnsupportedResponse)
            val connection = try {
                connectionFactory(URI(secretUrl).toURL())
            } catch (_: IllegalArgumentException) {
                return@withContext failure(MovieDetailsFailure.UnsupportedResponse)
            } catch (_: IOException) {
                return@withContext failure(MovieDetailsFailure.ProviderUnavailable)
            }
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
                connection.readTimeout = READ_TIMEOUT_MILLIS
                connection.instanceFollowRedirects = false
                connection.useCaches = false
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("User-Agent", userAgent())
                connection.setRequestProperty("Accept-Encoding", "gzip, identity")
                when (connection.responseCode) {
                    HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN ->
                        failure(MovieDetailsFailure.AuthenticationRejected)
                    in 200..299 -> {
                        val decoded = decode(connection.inputStream, connection.contentEncoding)
                        InputStreamReader(CatalogLimitInputStream(decoded, responseLimitBytes), StandardCharsets.UTF_8).use {
                            parser.parse(it, account.accountId, movieId, CatalogArtworkPolicy(account))
                        }
                    }
                    in 500..599 -> failure(MovieDetailsFailure.ProviderUnavailable)
                    else -> failure(MovieDetailsFailure.UnsupportedResponse)
                }
            } catch (_: SocketTimeoutException) {
                failure(MovieDetailsFailure.Timeout)
            } catch (_: CatalogResponseTooLargeException) {
                failure(MovieDetailsFailure.ResponseTooLarge)
            } catch (_: IOException) {
                failure(if (networkAvailable()) MovieDetailsFailure.ProviderUnavailable else MovieDetailsFailure.NetworkUnavailable)
            } finally {
                connection.disconnect()
            }
        }

    private fun decode(input: InputStream, encoding: String?): InputStream =
        if (encoding.equals("gzip", ignoreCase = true)) GZIPInputStream(input) else input

    private fun failure(reason: MovieDetailsFailure) = MovieDetailsResult.Failure(reason)

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 8_000
        const val READ_TIMEOUT_MILLIS = 30_000
        const val MAX_RESPONSE_BYTES = 8L * 1024L * 1024L
    }
}

internal object XtreamMovieDetailsRequestBuilder {
    fun build(account: SavedXtreamAccount, movieId: String): String? {
        val query = publicQuery(movieId) ?: return null
        return "${account.endpoint.baseUrl}/player_api.php?username=${encode(account.username)}" +
            "&password=${encode(account.password)}&$query"
    }

    fun publicQuery(movieId: String): String? = movieId
        .takeIf { it.isNotBlank() && it.codePointCount(0, it.length) <= 256 }
        ?.let { "action=get_vod_info&vod_id=${encode(it)}" }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20")
}
