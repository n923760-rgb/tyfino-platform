package dev.tyfino.foundation.xtream

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.FilterInputStream
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

internal interface XtreamCatalogApi {
    suspend fun categories(
        account: SavedXtreamAccount,
        section: CatalogSection,
    ): CatalogResult<CatalogCategory>

    suspend fun items(
        account: SavedXtreamAccount,
        section: CatalogSection,
        categoryId: String,
    ): CatalogResult<CatalogItem>
}

internal class HttpXtreamCatalogApi(
    private val networkAvailable: () -> Boolean,
    private val connectionFactory: (URL) -> HttpURLConnection,
    private val parser: XtreamCatalogParser = XtreamCatalogParser(),
    private val responseLimitBytes: Long = MAX_RESPONSE_BYTES,
    private val userAgent: () -> String = { ProviderUserAgentPreset.Tyfino.header() },
) : XtreamCatalogApi {
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

    override suspend fun categories(
        account: SavedXtreamAccount,
        section: CatalogSection,
    ): CatalogResult<CatalogCategory> = execute(account, CatalogRequest.Categories(section)) { reader ->
        parser.parseCategories(reader)
    }

    override suspend fun items(
        account: SavedXtreamAccount,
        section: CatalogSection,
        categoryId: String,
    ): CatalogResult<CatalogItem> = execute(
        account = account,
        request = CatalogRequest.Items(section, categoryId),
    ) { reader ->
        parser.parseItems(reader, section, categoryId, CatalogArtworkPolicy(account))
    }

    private suspend fun <T> execute(
        account: SavedXtreamAccount,
        request: CatalogRequest,
        parse: (InputStreamReader) -> CatalogResult<T>,
    ): CatalogResult<T> = withContext(Dispatchers.IO) {
        if (!networkAvailable()) {
            return@withContext CatalogResult.Failure(CatalogFailure.NetworkUnavailable)
        }
        if (account.endpoint.isCleartext && !account.cleartextConsent) {
            return@withContext CatalogResult.Failure(CatalogFailure.UnsupportedResponse)
        }
        val secretUrl = XtreamCatalogRequestBuilder.build(account, request)
            ?: return@withContext CatalogResult.Failure(CatalogFailure.UnsupportedResponse)
        val connection = try {
            connectionFactory(URI(secretUrl).toURL())
        } catch (_: IllegalArgumentException) {
            return@withContext CatalogResult.Failure(CatalogFailure.UnsupportedResponse)
        } catch (_: IOException) {
            return@withContext CatalogResult.Failure(CatalogFailure.ProviderUnavailable)
        }
        try {
            configure(connection)
            when (connection.responseCode) {
                HttpURLConnection.HTTP_UNAUTHORIZED,
                HttpURLConnection.HTTP_FORBIDDEN,
                -> CatalogResult.Failure(CatalogFailure.AuthenticationRejected)
                in 200..299 -> {
                    val decoded = decode(connection.inputStream, connection.contentEncoding)
                    val limited = CatalogLimitInputStream(decoded, responseLimitBytes)
                    InputStreamReader(limited, StandardCharsets.UTF_8).use(parse)
                }
                in 500..599 -> CatalogResult.Failure(CatalogFailure.ProviderUnavailable)
                else -> CatalogResult.Failure(CatalogFailure.UnsupportedResponse)
            }
        } catch (_: SocketTimeoutException) {
            CatalogResult.Failure(CatalogFailure.Timeout)
        } catch (_: CatalogResponseTooLargeException) {
            CatalogResult.Failure(CatalogFailure.ResponseTooLarge)
        } catch (_: IOException) {
            val reason = if (networkAvailable()) {
                CatalogFailure.ProviderUnavailable
            } else {
                CatalogFailure.NetworkUnavailable
            }
            CatalogResult.Failure(reason)
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
        connection.setRequestProperty("User-Agent", userAgent())
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

internal object XtreamCatalogRequestBuilder {
    fun build(account: SavedXtreamAccount, request: CatalogRequest): String? {
        val publicQuery = publicQuery(request) ?: return null
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

    fun publicQuery(request: CatalogRequest): String? = when (request) {
        is CatalogRequest.Categories -> "action=${encode(request.section.categoriesAction)}"
        is CatalogRequest.Items -> {
            if (request.categoryId.isBlank() || request.categoryId.codePointCount(0, request.categoryId.length) > 256) {
                null
            } else {
                "action=${encode(request.section.itemsAction)}&category_id=${encode(request.categoryId)}"
            }
        }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20")
}

internal class CatalogLimitInputStream(
    input: InputStream,
    private val limit: Long,
) : FilterInputStream(input) {
    private var consumed = 0L

    override fun read(): Int {
        val value = super.read()
        if (value >= 0) addConsumed(1)
        return value
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val count = super.read(buffer, offset, length)
        if (count > 0) addConsumed(count.toLong())
        return count
    }

    private fun addConsumed(count: Long) {
        consumed += count
        if (consumed > limit) throw CatalogResponseTooLargeException()
    }
}

internal class CatalogResponseTooLargeException : IOException()
