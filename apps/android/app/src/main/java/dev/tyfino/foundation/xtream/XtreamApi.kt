package dev.tyfino.foundation.xtream

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.IDN
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

internal sealed interface HostParseResult {
    data class Valid(val endpoint: ProviderEndpoint) : HostParseResult
    data object Invalid : HostParseResult
}

internal object XtreamHostCanonicalizer {
    fun parse(input: String): HostParseResult {
        val raw = input.trim()
        if (raw.isEmpty() || raw.any { it.isISOControl() } || '\\' in raw) return HostParseResult.Invalid
        val uri = runCatching { URI(raw) }.getOrNull() ?: return HostParseResult.Invalid
        val scheme = uri.scheme?.lowercase() ?: return HostParseResult.Invalid
        if (scheme != "https" && scheme != "http") return HostParseResult.Invalid
        if (uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null) {
            return HostParseResult.Invalid
        }

        val authority = uri.rawAuthority ?: return HostParseResult.Invalid
        if ('@' in authority) return HostParseResult.Invalid
        val authorityParts = parseAuthority(authority, uri.host, uri.port)
            ?: return HostParseResult.Invalid
        val rawHost = authorityParts.first
        val asciiHost = runCatching {
            if (rawHost.contains(':')) rawHost.lowercase() else IDN.toASCII(
                rawHost,
                IDN.USE_STD3_ASCII_RULES,
            ).lowercase()
        }.getOrNull()?.takeIf(String::isNotBlank) ?: return HostParseResult.Invalid

        val port = authorityParts.second
        if (port !in -1..65535 || port == 0) return HostParseResult.Invalid
        val canonicalPort = when {
            scheme == "https" && port == 443 -> -1
            scheme == "http" && port == 80 -> -1
            else -> port
        }

        val rawPath = uri.rawPath.orEmpty()
        val unsafePath = rawPath.lowercase()
        if (
            uri.normalize().rawPath != rawPath ||
            "%2e" in unsafePath ||
            "%2f" in unsafePath ||
            "%5c" in unsafePath
        ) {
            return HostParseResult.Invalid
        }
        val path = uri.path.orEmpty().trimEnd('/').takeUnless { it.isEmpty() }
        val canonical = runCatching {
            URI(scheme, null, asciiHost, canonicalPort, path, null, null).toASCIIString()
        }.getOrNull() ?: return HostParseResult.Invalid
        return HostParseResult.Valid(ProviderEndpoint(canonical, scheme == "http"))
    }

    private fun parseAuthority(
        authority: String,
        parsedHost: String?,
        parsedPort: Int,
    ): Pair<String, Int>? {
        if (parsedHost != null) return parsedHost to parsedPort
        if (authority.startsWith('[')) return null
        val colon = authority.lastIndexOf(':')
        if (colon < 0) return authority to -1
        val portText = authority.substring(colon + 1)
        if (portText.isEmpty() || portText.any { !it.isDigit() }) return null
        val port = portText.toIntOrNull() ?: return null
        return authority.substring(0, colon).takeIf(String::isNotBlank)?.let { it to port }
    }
}

internal interface XtreamApi {
    suspend fun authenticate(
        endpoint: ProviderEndpoint,
        username: String,
        password: String,
    ): XtreamAuthResult
}

internal class HttpXtreamApi(context: Context) : XtreamApi {
    private val userAgent = ProviderUserAgent(context)
    private val connectivityManager =
        context.getSystemService(ConnectivityManager::class.java)

    override suspend fun authenticate(
        endpoint: ProviderEndpoint,
        username: String,
        password: String,
    ): XtreamAuthResult = withContext(Dispatchers.IO) {
        if (!hasUsableNetwork()) {
            return@withContext XtreamAuthResult.Failure(XtreamFailure.NetworkUnavailable)
        }
        val requestUrl = buildSecretRequestUrl(endpoint, username, password)
        val connection = try {
            URI(requestUrl).toURL().openConnection() as HttpURLConnection
        } catch (_: IllegalArgumentException) {
            return@withContext XtreamAuthResult.Failure(XtreamFailure.InvalidHost)
        } catch (_: IOException) {
            return@withContext XtreamAuthResult.Failure(XtreamFailure.InvalidHost)
        }
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", userAgent.header())

            val status = connection.responseCode
            if (status == HttpURLConnection.HTTP_UNAUTHORIZED || status == HttpURLConnection.HTTP_FORBIDDEN) {
                return@withContext XtreamAuthResult.Failure(XtreamFailure.InvalidCredentials)
            }
            if (status !in 200..299) {
                return@withContext XtreamAuthResult.Failure(XtreamFailure.UnsupportedResponse)
            }
            val body = connection.inputStream.use(::readLimited)
            XtreamResponseClassifier.classify(status, body)
        } catch (_: SocketTimeoutException) {
            XtreamAuthResult.Failure(XtreamFailure.Timeout)
        } catch (_: ResponseTooLargeException) {
            XtreamAuthResult.Failure(XtreamFailure.UnsupportedResponse)
        } catch (_: IOException) {
            val reason = if (hasUsableNetwork()) {
                XtreamFailure.ProviderUnavailable
            } else {
                XtreamFailure.NetworkUnavailable
            }
            XtreamAuthResult.Failure(reason)
        } finally {
            connection.disconnect()
        }
    }

    private fun hasUsableNetwork(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun buildSecretRequestUrl(
        endpoint: ProviderEndpoint,
        username: String,
        password: String,
    ): String {
        val encodedUsername = encodeQueryValue(username)
        val encodedPassword = encodeQueryValue(password)
        return "${endpoint.baseUrl}/player_api.php?username=$encodedUsername&password=$encodedPassword"
    }

    private fun encodeQueryValue(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20")

    private fun readLimited(stream: java.io.InputStream): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = stream.read(buffer)
            if (read == -1) break
            total += read
            if (total > MAX_RESPONSE_BYTES) throw ResponseTooLargeException()
            output.write(buffer, 0, read)
        }
        return output.toString(StandardCharsets.UTF_8.name())
    }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 8_000
        const val READ_TIMEOUT_MILLIS = 12_000
        const val MAX_RESPONSE_BYTES = 256 * 1024
    }

    private class ResponseTooLargeException : IOException()
}

internal object XtreamResponseClassifier {
    fun classify(statusCode: Int, body: String): XtreamAuthResult {
        if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED ||
            statusCode == HttpURLConnection.HTTP_FORBIDDEN
        ) {
            return XtreamAuthResult.Failure(XtreamFailure.InvalidCredentials)
        }
        if (statusCode !in 200..299) {
            return XtreamAuthResult.Failure(XtreamFailure.UnsupportedResponse)
        }
        val root = try {
            JSONObject(body)
        } catch (_: JSONException) {
            return XtreamAuthResult.Failure(XtreamFailure.MalformedResponse)
        }
        if (!root.has("user_info") || root.isNull("user_info")) {
            return XtreamAuthResult.Failure(XtreamFailure.MalformedResponse)
        }
        val userInfo = root.opt("user_info") as? JSONObject
            ?: return XtreamAuthResult.Failure(XtreamFailure.UnsupportedResponse)
        if (!userInfo.has("auth") || !userInfo.has("status")) {
            return XtreamAuthResult.Failure(XtreamFailure.MalformedResponse)
        }
        val auth = when (val value = userInfo.opt("auth")) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        } ?: return XtreamAuthResult.Failure(XtreamFailure.UnsupportedResponse)
        if (auth == 0) return XtreamAuthResult.Failure(XtreamFailure.InvalidCredentials)
        if (auth != 1) return XtreamAuthResult.Failure(XtreamFailure.UnsupportedResponse)

        val providerStatus = userInfo.opt("status") as? String
            ?: return XtreamAuthResult.Failure(XtreamFailure.UnsupportedResponse)
        return when (providerStatus.lowercase()) {
            "active" -> XtreamAuthResult.Success
            "expired" -> XtreamAuthResult.Failure(XtreamFailure.AccountExpired)
            "disabled", "banned", "inactive" ->
                XtreamAuthResult.Failure(XtreamFailure.AccountDisabled)
            else -> XtreamAuthResult.Failure(XtreamFailure.UnsupportedResponse)
        }
    }
}
