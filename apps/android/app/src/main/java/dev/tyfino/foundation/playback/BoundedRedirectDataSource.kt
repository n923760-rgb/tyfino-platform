@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package dev.tyfino.foundation.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.Locale

internal class BoundedRedirectDataSource(
    private val cleartextConsent: Boolean,
) : BaseDataSource(true) {
    internal class Factory(
        private val cleartextConsent: Boolean,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource =
            BoundedRedirectDataSource(cleartextConsent)
    }

    private var connection: HttpURLConnection? = null
    private var stream: InputStream? = null
    private var openedUri: Uri? = null
    private var bytesRemaining = C.LENGTH_UNSET.toLong()
    private var transferOpen = false

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        val opened = connect(dataSpec)
        connection = opened.connection
        openedUri = Uri.parse(opened.uri.toASCIIString())

        val responseCode = opened.connection.responseCode
        val bytesToSkip = if (responseCode == HttpURLConnection.HTTP_OK) {
            dataSpec.position
        } else {
            0L
        }
        stream = opened.connection.inputStream
        if (bytesToSkip > 0) skipFully(stream!!, bytesToSkip)

        bytesRemaining = when {
            dataSpec.length != C.LENGTH_UNSET.toLong() -> dataSpec.length
            opened.connection.contentLengthLong < 0 -> C.LENGTH_UNSET.toLong()
            else -> (opened.connection.contentLengthLong - bytesToSkip).coerceAtLeast(0)
        }
        transferOpen = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val requested = if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
            length
        } else {
            minOf(length.toLong(), bytesRemaining).toInt()
        }
        val read = stream?.read(buffer, offset, requested) ?: C.RESULT_END_OF_INPUT
        if (read == C.RESULT_END_OF_INPUT) return C.RESULT_END_OF_INPUT
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) bytesRemaining -= read
        bytesTransferred(read)
        return read
    }

    override fun getUri(): Uri? = openedUri

    override fun getResponseHeaders(): Map<String, List<String>> =
        connection?.headerFields?.entries
            ?.mapNotNull { entry -> entry.key?.let { key -> key to entry.value.toList() } }
            ?.toMap()
            .orEmpty()

    override fun close() {
        var failure: IOException? = null
        try {
            stream?.close()
        } catch (error: IOException) {
            failure = error
        } finally {
            stream = null
            connection?.disconnect()
            connection = null
            openedUri = null
            bytesRemaining = C.LENGTH_UNSET.toLong()
            if (transferOpen) {
                transferOpen = false
                transferEnded()
            }
        }
        failure?.let { throw it }
    }

    private fun connect(dataSpec: DataSpec): OpenedConnection {
        var current = parseInitial(dataSpec.uri)
        var redirectsFollowed = 0
        while (true) {
            val candidate = (current.toURL().openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = CONNECT_TIMEOUT_MILLIS
                readTimeout = READ_TIMEOUT_MILLIS
                requestMethod = requestMethod(dataSpec)
                setRequestProperty("Accept-Encoding", "identity")
                rangeHeader(dataSpec)?.let { setRequestProperty("Range", it) }
                connect()
            }
            val responseCode = candidate.responseCode
            if (responseCode !in REDIRECT_CODES) {
                if (responseCode !in 200..299) {
                    candidate.disconnect()
                    throw IOException("Playback provider returned HTTP $responseCode")
                }
                return OpenedConnection(candidate, current)
            }

            val location = candidate.getHeaderField("Location")
            val redirect = if (location == null) {
                PlaybackRedirectResult.Rejected(PlaybackRedirectFailure.InvalidTarget)
            } else {
                PlaybackRedirectPolicy.evaluate(
                    current = current,
                    targetValue = location,
                    redirectsFollowed = redirectsFollowed,
                    cleartextConsent = cleartextConsent,
                )
            }
            candidate.disconnect()
            current = when (redirect) {
                is PlaybackRedirectResult.Accepted -> redirect.target
                is PlaybackRedirectResult.Rejected -> {
                    throw IOException("Playback redirect rejected: ${redirect.reason.name}")
                }
            }
            redirectsFollowed++
        }
    }

    private fun parseInitial(value: Uri): URI {
        val parsed = runCatching { URI(value.toString()) }.getOrNull()
            ?: throw IOException("Invalid playback address")
        val scheme = parsed.scheme?.lowercase(Locale.ROOT)
        if (
            !parsed.isAbsolute ||
            parsed.host.isNullOrBlank() ||
            parsed.rawUserInfo != null ||
            parsed.rawFragment != null ||
            (scheme != "https" && scheme != "http")
        ) {
            throw IOException("Invalid playback address")
        }
        if (scheme == "http" && !cleartextConsent) {
            throw IOException("Cleartext playback rejected")
        }
        return parsed
    }

    private fun requestMethod(dataSpec: DataSpec): String = when (dataSpec.httpMethod) {
        DataSpec.HTTP_METHOD_GET -> "GET"
        DataSpec.HTTP_METHOD_HEAD -> "HEAD"
        else -> throw IOException("Unsupported playback request method")
    }

    private fun rangeHeader(dataSpec: DataSpec): String? {
        if (dataSpec.position == 0L && dataSpec.length == C.LENGTH_UNSET.toLong()) return null
        val end = if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
            ""
        } else {
            (dataSpec.position + dataSpec.length - 1).toString()
        }
        return "bytes=${dataSpec.position}-$end"
    }

    private fun skipFully(input: InputStream, bytes: Long) {
        var remaining = bytes
        val buffer = ByteArray(SKIP_BUFFER_SIZE)
        while (remaining > 0) {
            val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (read < 0) throw EOFException("Playback response ended before requested range")
            remaining -= read
            bytesTransferred(read)
        }
    }

    private data class OpenedConnection(
        val connection: HttpURLConnection,
        val uri: URI,
    )

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 10_000
        const val SKIP_BUFFER_SIZE = 8 * 1024
        val REDIRECT_CODES = setOf(300, 301, 302, 303, 307, 308)
    }
}
