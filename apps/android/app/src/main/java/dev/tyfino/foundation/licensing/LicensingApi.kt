package dev.tyfino.foundation.licensing

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal class LicensingApiException(
    val code: String,
    val retryable: Boolean,
) : Exception(code)

internal interface LicensingApi {
    suspend fun startTrial(installationId: String, appVersion: String): LicenseEnvelope
    suspend fun activate(installationId: String, appVersion: String, activationCode: String): LicenseEnvelope
    suspend fun refresh(installationId: String, appVersion: String, sessionToken: String): LicenseEnvelope
    suspend fun revoke(sessionToken: String)
}

internal class HttpLicensingApi(private val baseUrl: String) : LicensingApi {
    override suspend fun startTrial(installationId: String, appVersion: String): LicenseEnvelope = withContext(Dispatchers.IO) {
        post("/v1/licensing/trials/start", installationBody(installationId, appVersion))
    }

    override suspend fun activate(
        installationId: String,
        appVersion: String,
        activationCode: String,
    ): LicenseEnvelope = withContext(Dispatchers.IO) {
        post(
            "/v1/licensing/activations",
            installationBody(installationId, appVersion).put("activationCode", activationCode),
        )
    }

    override suspend fun refresh(
        installationId: String,
        appVersion: String,
        sessionToken: String,
    ): LicenseEnvelope = withContext(Dispatchers.IO) {
        post(
            "/v1/licensing/entitlements/refresh",
            installationBody(installationId, appVersion),
            sessionToken,
        )
    }

    override suspend fun revoke(sessionToken: String) {
        withContext(Dispatchers.IO) {
            execute("/v1/licensing/sessions/revoke", JSONObject(), sessionToken, expectBody = false)
        }
    }

    private fun installationBody(installationId: String, appVersion: String): JSONObject = JSONObject()
        .put("installationId", installationId)
        .put("platform", "android")
        .put("appVersion", appVersion)

    private fun post(path: String, body: JSONObject, token: String? = null): LicenseEnvelope =
        parseEnvelope(execute(path, body, token, expectBody = true))

    private fun execute(path: String, body: JSONObject, token: String?, expectBody: Boolean): JSONObject {
        val origin = baseUrl.trimEnd('/').takeIf { it.startsWith("https://") }
            ?: throw LicensingApiException("LICENSE_UNAVAILABLE", true)
        val payload = body.toString().toByteArray(Charsets.UTF_8)
        val connection = (URL(origin + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            instanceFollowRedirects = false
            doOutput = true
            setFixedLengthStreamingMode(payload.size)
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
        }
        return try {
            connection.outputStream.use { it.write(payload) }
            val status = connection.responseCode
            if (status == HttpURLConnection.HTTP_NO_CONTENT && !expectBody) return JSONObject()
            val response = readLimited(if (status in 200..299) connection.inputStream else connection.errorStream)
            val json = runCatching { JSONObject(response) }
                .getOrElse { throw LicensingApiException("LICENSE_UNAVAILABLE", true) }
            if (status !in 200..299) {
                val error = json.optJSONObject("error")
                throw LicensingApiException(
                    code = error?.optString("code")?.takeIf(String::isNotBlank) ?: "LICENSE_UNAVAILABLE",
                    retryable = error?.optBoolean("retryable", status >= 500) ?: (status >= 500),
                )
            }
            json
        } catch (error: LicensingApiException) {
            throw error
        } catch (_: Exception) {
            throw LicensingApiException("LICENSE_UNAVAILABLE", true)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseEnvelope(json: JSONObject): LicenseEnvelope {
        return runCatching {
            val entitlement = json.getJSONObject("entitlement")
            val session = json.getJSONObject("session")
            require(entitlement.getString("status") == "active")
            LicenseEnvelope(
                serverTimeMillis = parseInstant(json.getString("serverTime")),
                entitlement = EntitlementPayload(
                    kind = when (entitlement.getString("kind")) {
                        "trial" -> EntitlementKind.Trial
                        "one_year" -> EntitlementKind.OneYear
                        "lifetime" -> EntitlementKind.Lifetime
                        else -> error("Unsupported entitlement kind")
                    },
                    startsAtMillis = parseInstant(entitlement.getString("startsAt")),
                    expiresAtMillis = entitlement.optInstant("expiresAt"),
                    offlineValidUntilMillis = parseInstant(entitlement.getString("offlineValidUntil")),
                ),
                sessionToken = session.getString("token").also { require(it.isNotBlank()) },
                refreshAfterMillis = parseInstant(session.getString("refreshAfter")),
            )
        }.getOrElse { throw LicensingApiException("LICENSE_UNAVAILABLE", true) }
    }

    private fun JSONObject.optInstant(name: String): Long? =
        if (!has(name) || isNull(name)) null else parseInstant(getString(name))

    private fun parseInstant(value: String): Long {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return requireNotNull(formatter.parse(value)).time
    }

    private fun readLimited(stream: InputStream?): String {
        if (stream == null) return ""
        return stream.bufferedReader(Charsets.UTF_8).use { reader ->
            val output = StringBuilder()
            val buffer = CharArray(4_096)
            while (true) {
                val count = reader.read(buffer)
                if (count < 0) break
                if (output.length + count > MAX_RESPONSE_CHARS) {
                    throw LicensingApiException("LICENSE_UNAVAILABLE", true)
                }
                output.append(buffer, 0, count)
            }
            output.toString()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 8_000
        const val READ_TIMEOUT_MILLIS = 12_000
        const val MAX_RESPONSE_CHARS = 64 * 1_024
    }
}
