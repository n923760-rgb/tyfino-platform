package dev.tyfino.foundation.xtream

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal enum class PlaylistStatus { Active, Expired, Disabled }

/** Only optional provider subscription metadata; never a license entitlement. */
internal data class PlaylistInfo(
    val status: PlaylistStatus,
    val expiresAtMillis: Long?,
    val trial: Boolean?,
    val activeConnections: Int?,
    val maxConnections: Int?,
)

internal sealed interface PlaylistResult {
    data class Ready(val info: PlaylistInfo) : PlaylistResult
    data class Failure(val reason: XtreamFailure) : PlaylistResult
}

internal sealed interface PlaylistState {
    data object Loading : PlaylistState
    data object NoAccount : PlaylistState
    data object Stale : PlaylistState
    data class Ready(val providerOrigin: String, val username: String, val info: PlaylistInfo) : PlaylistState {
        override fun toString() = "PlaylistState.Ready(redacted)"
    }
    data class Failure(val reason: XtreamFailure) : PlaylistState
}

internal fun interface PlaylistApi {
    suspend fun read(account: SavedXtreamAccount): PlaylistResult
}

/** Revalidates account, credential generation and latest request before publishing metadata. */
internal class PlaylistRepository(private val accounts: XtreamAccountStore, private val api: PlaylistApi) {
    private val request = AtomicLong()

    suspend fun read(): PlaylistState = withContext(Dispatchers.IO) {
        val operation = request.incrementAndGet()
        val account = try { accounts.load() } catch (_: RuntimeException) {
            return@withContext PlaylistState.Failure(XtreamFailure.LocalStorage)
        } ?: return@withContext PlaylistState.NoAccount
        val result = api.read(account)
        val current = try { accounts.load() } catch (_: RuntimeException) {
            return@withContext PlaylistState.Failure(XtreamFailure.LocalStorage)
        }
        if (operation != request.get() || current?.accountId != account.accountId || current.generation != account.generation) {
            return@withContext PlaylistState.Stale
        }
        when (result) {
            is PlaylistResult.Failure -> PlaylistState.Failure(result.reason)
            is PlaylistResult.Ready -> {
                val uri = URI(account.endpoint.baseUrl)
                val origin = URI(uri.scheme, null, uri.host, uri.port, null, null, null).toASCIIString()
                PlaylistState.Ready(origin, account.username.filterNot(Char::isISOControl).take(128), result.info)
            }
        }
    }
}

internal object PlaylistParser {
    fun parse(body: String): PlaylistResult {
        val root = runCatching { JSONObject(body) }.getOrNull()
            ?: return PlaylistResult.Failure(XtreamFailure.MalformedResponse)
        val info = root.opt("user_info") as? JSONObject
            ?: return PlaylistResult.Failure(XtreamFailure.MalformedResponse)
        if (!info.has("auth") || !info.has("status")) return PlaylistResult.Failure(XtreamFailure.MalformedResponse)
        val auth = integer(info.opt("auth"))
        if (auth == 0L) return PlaylistResult.Failure(XtreamFailure.InvalidCredentials)
        if (auth != 1L) return PlaylistResult.Failure(XtreamFailure.UnsupportedResponse)
        val status = when ((info.opt("status") as? String)?.lowercase(Locale.ROOT)) {
            "active" -> PlaylistStatus.Active
            "expired" -> PlaylistStatus.Expired
            "disabled", "banned", "inactive" -> PlaylistStatus.Disabled
            else -> return PlaylistResult.Failure(XtreamFailure.UnsupportedResponse)
        }
        // Missing, zero and malformed expiry do not prove a lifetime subscription.
        val expiry = integer(info.opt("exp_date"))?.takeIf { it in 1..253_402_300_799L }?.times(1_000)
        fun count(name: String) = integer(info.opt(name))?.takeIf { it in 0..10_000L }?.toInt()
        return PlaylistResult.Ready(PlaylistInfo(status, expiry,
            when (integer(info.opt("is_trial"))) { 0L -> false; 1L -> true; else -> null },
            count("active_cons"), count("max_connections")))
    }

    private fun integer(value: Any?): Long? = when (value) {
        is Number -> value.toString().toLongOrNull()
        is String -> value.takeIf { it.length <= 18 && it.all(Char::isDigit) }?.toLongOrNull()
        else -> null
    }
}

/** One bounded direct provider query on Settings entry or explicit refresh. No polling/persistence. */
internal class HttpPlaylistApi(context: Context) : PlaylistApi {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val userAgent = ProviderUserAgent(context)

    override suspend fun read(account: SavedXtreamAccount): PlaylistResult = withContext(Dispatchers.IO) {
        val parsed = XtreamHostCanonicalizer.parse(account.endpoint.baseUrl) as? HostParseResult.Valid
        if (parsed?.endpoint != account.endpoint || (account.endpoint.isCleartext && !account.cleartextConsent)) {
            return@withContext PlaylistResult.Failure(XtreamFailure.InvalidHost)
        }
        if (!connected()) return@withContext PlaylistResult.Failure(XtreamFailure.NetworkUnavailable)
        val connection = try {
            URI(requestUrl(account)).toURL().openConnection() as HttpURLConnection
        } catch (_: IOException) {
            return@withContext PlaylistResult.Failure(XtreamFailure.ProviderUnavailable)
        } catch (_: IllegalArgumentException) {
            return@withContext PlaylistResult.Failure(XtreamFailure.InvalidHost)
        }
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 12_000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", userAgent.header())
            val code = connection.responseCode
            if (code == 401 || code == 403) return@withContext PlaylistResult.Failure(XtreamFailure.InvalidCredentials)
            if (code !in 200..299) return@withContext PlaylistResult.Failure(XtreamFailure.UnsupportedResponse)
            val output = ByteArrayOutputStream()
            connection.inputStream.use { stream ->
                val buffer = ByteArray(8_192)
                while (true) {
                    val size = stream.read(buffer)
                    if (size < 0) break
                    if (output.size() + size > 256 * 1_024) {
                        return@withContext PlaylistResult.Failure(XtreamFailure.UnsupportedResponse)
                    }
                    output.write(buffer, 0, size)
                }
            }
            PlaylistParser.parse(output.toString("UTF-8"))
        } catch (_: SocketTimeoutException) {
            PlaylistResult.Failure(XtreamFailure.Timeout)
        } catch (_: IOException) {
            PlaylistResult.Failure(if (connected()) XtreamFailure.ProviderUnavailable else XtreamFailure.NetworkUnavailable)
        } finally {
            connection.disconnect()
        }
    }

    private fun connected(): Boolean = connectivity.activeNetwork?.let {
        connectivity.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } == true

    private fun requestUrl(account: SavedXtreamAccount): String {
        fun encode(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
        return "${account.endpoint.baseUrl}/player_api.php?username=${encode(account.username)}&password=${encode(account.password)}"
    }
}
