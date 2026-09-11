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

internal interface XtreamSeriesApi {
    suspend fun details(
        account: SavedXtreamAccount,
        seriesId: String,
    ): SeriesDetailsResult
}

internal class HttpXtreamSeriesApi(
    private val networkAvailable: () -> Boolean,
    private val connectionFactory: (URL) -> HttpURLConnection,
    private val parser: XtreamSeriesParser = XtreamSeriesParser(),
    private val responseLimitBytes: Long = MAX_RESPONSE_BYTES,
) : XtreamSeriesApi {
    constructor(context: Context) : this(
        networkAvailable = {
            val manager = context.getSystemService(ConnectivityManager::class.java)
            val network = manager.activeNetwork
            val capabilities = network?.let(manager::getNetworkCapabilities)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        },
        connectionFactory = { url -> url.openConnection() as HttpURLConnection },
    )

    override suspend fun details(
        account: SavedXtreamAccount,
        seriesId: String,
    ): SeriesDetailsResult = withContext(Dispatchers.IO) {
        if (!networkAvailable()) {
            return@withContext SeriesDetailsResult.Failure(SeriesFailure.NetworkUnavailable)
        }
        if (account.endpoint.isCleartext && !account.cleartextConsent) {
            return@withContext SeriesDetailsResult.Failure(SeriesFailure.UnsupportedResponse)
        }
        val secretUrl = XtreamSeriesRequestBuilder.build(account, seriesId)
            ?: return@withContext SeriesDetailsResult.Failure(SeriesFailure.UnsupportedResponse)
        val connection = try {
            connectionFactory(URI(secretUrl).toURL())
        } catch (_: IllegalArgumentException) {
            return@withContext SeriesDetailsResult.Failure(SeriesFailure.UnsupportedResponse)
        } catch (_: IOException) {
            return@withContext SeriesDetailsResult.Failure(SeriesFailure.ProviderUnavailable)
        }
        try {
            configure(connection)
            when (connection.responseCode) {
                HttpURLConnection.HTTP_UNAUTHORIZED,
                HttpURLConnection.HTTP_FORBIDDEN,
                -> SeriesDetailsResult.Failure(SeriesFailure.AuthenticationRejected)
                in 200..299 -> {
                    val decoded = decode(connection.inputStream, connection.contentEncoding)
                    val limited = CatalogLimitInputStream(decoded, responseLimitBytes)
                    InputStreamReader(limited, StandardCharsets.UTF_8).use { reader ->
                        parser.parse(
                            reader = reader,
                            accountId = account.accountId,
                            seriesId = seriesId,
                            artworkPolicy = CatalogArtworkPolicy(account),
                        )
                    }
                }
                in 500..599 -> SeriesDetailsResult.Failure(SeriesFailure.ProviderUnavailable)
                else -> SeriesDetailsResult.Failure(SeriesFailure.UnsupportedResponse)
            }
        } catch (_: SocketTimeoutException) {
            SeriesDetailsResult.Failure(SeriesFailure.Timeout)
        } catch (_: CatalogResponseTooLargeException) {
            SeriesDetailsResult.Failure(SeriesFailure.ResponseTooLarge)
        } catch (_: IOException) {
            val reason = if (networkAvailable()) {
                SeriesFailure.ProviderUnavailable
            } else {
                SeriesFailure.NetworkUnavailable
            }
            SeriesDetailsResult.Failure(reason)
        } finally {
            connection.disconnect()
        }
    }

    private fun configure(connection: HttpURLConnection) {
        connection.requestMethod = "GET"
        connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
        connection.readTimeout = READ_TIMEOUT_MILLIS
        connection.instanceFollowRedirects = false
        connection.useCaches = false
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Accept-Encoding", "gzip, identity")
    }

    private fun decode(input: InputStream, encoding: String?): InputStream =
        if (encoding.equals("gzip", ignoreCase = true)) GZIPInputStream(input) else input

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 8_000
        const val READ_TIMEOUT_MILLIS = 60_000
        const val MAX_RESPONSE_BYTES = 32L * 1024L * 1024L
    }
}

internal object XtreamSeriesRequestBuilder {
    fun build(account: SavedXtreamAccount, seriesId: String): String? {
        val publicQuery = publicQuery(seriesId) ?: return null
        return buildString {
            append(account.endpoint.baseUrl)
            append("/player_api.php?username=")
            append(encode(account.username))
            append("&password=")
            append(encode(account.password))
            append('&')
            append(publicQuery)
        }
    }

    fun publicQuery(seriesId: String): String? {
        if (seriesId.isBlank() || seriesId.codePointCount(0, seriesId.length) > PROVIDER_ID_LIMIT) {
            return null
        }
        return "action=get_series_info&series_id=${encode(seriesId)}"
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20")

    private const val PROVIDER_ID_LIMIT = 256
}
