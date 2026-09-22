@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package dev.tyfino.foundation.playback

import android.content.res.AssetManager
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.BufferedReader
import java.io.Closeable
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalPlaybackFixtureTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var server: PlaybackFixtureServer

    @Before
    fun setUp() {
        server = PlaybackFixtureServer(instrumentation.context.assets).also { it.start() }
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun progressiveMovieFixturePreparesAndSupportsSeek() {
        val player = createPlayer()
        try {
            prepareAndAwaitReady(player, server.url("/movie.mp4"))
            assertTrue("The local progressive Movie fixture must be seekable.", onMain {
                player.isCurrentMediaItemSeekable
            })

            onMain { player.seekTo(500L) }
            waitUntil("Movie seek did not reach the requested local fixture position.") {
                onMain { player.currentPosition >= 400L }
            }
        } finally {
            onMain { player.release() }
        }
    }

    @Test
    fun hlsTransportStreamFixturePreparesThroughProductionDataSource() {
        val player = createPlayer()
        try {
            prepareAndAwaitReady(player, server.url("/live/index.m3u8"))
            val duration = onMain { player.duration }
            assertTrue(
                "The local HLS fixture must expose a finite parsed duration.",
                duration != C.TIME_UNSET && duration > 0L,
            )
        } finally {
            onMain { player.release() }
        }
    }

    @Test
    fun missingMediaFixtureReportsFailureWithoutBecomingReady() {
        val player = createPlayer()
        try {
            prepareAndAwaitFailure(player, server.url("/missing.mp4"))
        } finally {
            onMain { player.release() }
        }
    }

    @Test
    fun progressiveFixtureSurvivesRepeatedPlayerRecreation() {
        repeat(4) {
            val player = createPlayer()
            try {
                prepareAndAwaitReady(player, server.url("/movie.mp4"))
            } finally {
                onMain { player.release() }
            }
        }
    }

    private fun createPlayer(): ExoPlayer = onMain {
        val context = instrumentation.targetContext.applicationContext
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(BoundedRedirectDataSource.Factory(cleartextConsent = true))
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    private fun prepareAndAwaitReady(player: ExoPlayer, uri: String) {
        val terminal = CountDownLatch(1)
        val state = AtomicInteger(Player.STATE_IDLE)
        val failure = AtomicReference<PlaybackException?>()
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                state.set(playbackState)
                if (playbackState == Player.STATE_READY) terminal.countDown()
            }

            override fun onPlayerError(error: PlaybackException) {
                failure.set(error)
                terminal.countDown()
            }
        }

        onMain {
            player.addListener(listener)
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
        }

        assertTrue(
            "Timed out preparing the credential-free local playback fixture.",
            terminal.await(20, TimeUnit.SECONDS),
        )
        assertNull("Local playback fixture failed to prepare.", failure.get())
        assertEquals(Player.STATE_READY, state.get())
        onMain { player.removeListener(listener) }
    }

    private fun prepareAndAwaitFailure(player: ExoPlayer, uri: String) {
        val terminal = CountDownLatch(1)
        val state = AtomicInteger(Player.STATE_IDLE)
        val failure = AtomicReference<PlaybackException?>()
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                state.set(playbackState)
                if (playbackState == Player.STATE_READY) terminal.countDown()
            }

            override fun onPlayerError(error: PlaybackException) {
                failure.set(error)
                terminal.countDown()
            }
        }

        onMain {
            player.addListener(listener)
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
        }

        assertTrue(
            "Timed out waiting for the expected credential-free playback failure.",
            terminal.await(20, TimeUnit.SECONDS),
        )
        assertTrue(
            "Missing local media unexpectedly reached Player.STATE_READY.",
            state.get() != Player.STATE_READY,
        )
        assertTrue(
            "Missing local media did not report a PlaybackException.",
            failure.get() != null,
        )
        onMain { player.removeListener(listener) }
    }

    private fun waitUntil(message: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (System.nanoTime() < deadline) {
            if (condition()) return
            Thread.sleep(25)
        }
        assertTrue(message, condition())
    }

    private fun <T> onMain(block: () -> T): T {
        val task = FutureTask(Callable { block() })
        instrumentation.runOnMainSync(task)
        return task.get()
    }
}

internal class PlaybackFixtureServer(
    assets: AssetManager,
) : Closeable {
    private val fixtures = mapOf(
        "/movie.mp4" to Fixture(
            bytes = assets.open("playback/movie.mp4").use { it.readBytes() },
            contentType = "video/mp4",
        ),
        "/live/index.m3u8" to Fixture(
            bytes = assets.open("playback/live/index.m3u8").use { it.readBytes() },
            contentType = "application/vnd.apple.mpegurl",
        ),
        "/live/segment000.ts" to Fixture(
            bytes = assets.open("playback/live/segment000.ts").use { it.readBytes() },
            contentType = "video/mp2t",
        ),
    )
    private val socket = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))
    private val running = AtomicBoolean(false)
    private val activeSlowStreams = AtomicInteger(0)
    private val clients = mutableSetOf<Socket>()
    private var acceptThread: Thread? = null

    fun start() {
        check(running.compareAndSet(false, true))
        acceptThread = thread(
            start = true,
            isDaemon = true,
            name = "tyfino-local-playback-fixture",
        ) {
            while (running.get()) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                synchronized(clients) { clients += client }
                try {
                    runCatching { client.use(::serve) }
                } finally {
                    synchronized(clients) { clients -= client }
                }
            }
        }
    }

    fun baseUrl(): String = "http://127.0.0.1:${socket.localPort}"

    fun url(path: String): String = "${baseUrl()}$path"

    fun activeSlowStreamCount(): Int = activeSlowStreams.get()

    override fun close() {
        if (!running.compareAndSet(true, false)) return
        runCatching { socket.close() }
        synchronized(clients) {
            clients.toList().forEach { client -> runCatching { client.close() } }
        }
        acceptThread?.join(2_000)
        acceptThread = null
    }

    private fun serve(client: Socket) {
        client.soTimeout = 5_000
        val reader = BufferedReader(
            InputStreamReader(client.getInputStream(), StandardCharsets.US_ASCII),
        )
        val request = reader.readLine()?.split(' ') ?: return
        if (request.size < 2) return
        val method = request[0]
        val path = request[1].substringBefore('?')
        var rangeHeader: String? = null
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) break
            if (line.startsWith("Range:", ignoreCase = true)) {
                rangeHeader = line.substringAfter(':').trim()
            }
        }

        if (method != "GET" && method != "HEAD") {
            respond(client, 405, "Method Not Allowed", "text/plain", ByteArray(0), null, method)
            return
        }
        if (path == STREAMING_PLAYLIST_PATH) {
            respond(
                client = client,
                status = 200,
                reason = "OK",
                contentType = "application/vnd.apple.mpegurl",
                body = STREAMING_PLAYLIST.toByteArray(StandardCharsets.US_ASCII),
                range = null,
                method = method,
            )
            return
        }
        if (path == STREAMING_SEGMENT_PATH) {
            respondSlowStream(client, method)
            return
        }
        val fixture = fixtures[path]
        if (fixture == null) {
            respond(client, 404, "Not Found", "text/plain", ByteArray(0), null, method)
            return
        }

        val range = parseRange(rangeHeader, fixture.bytes.size)
        if (rangeHeader != null && range == null) {
            val output = client.getOutputStream()
            output.write(
                (
                    "HTTP/1.1 416 Range Not Satisfiable\r\n" +
                        "Content-Range: bytes */${fixture.bytes.size}\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n\r\n"
                ).toByteArray(StandardCharsets.US_ASCII),
            )
            output.flush()
            return
        }

        val body = if (range == null) {
            fixture.bytes
        } else {
            fixture.bytes.copyOfRange(range.first, range.last + 1)
        }
        respond(
            client = client,
            status = if (range == null) 200 else 206,
            reason = if (range == null) "OK" else "Partial Content",
            contentType = fixture.contentType,
            body = body,
            range = range?.let { "bytes ${it.first}-${it.last}/${fixture.bytes.size}" },
            method = method,
        )
    }

    private fun respondSlowStream(client: Socket, method: String) {
        val chunk = requireNotNull(fixtures["/live/segment000.ts"]).bytes
        val declaredLength = chunk.size.toLong() * SLOW_STREAM_CHUNKS
        val output = client.getOutputStream()
        val headers = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: video/mp2t\r\n")
            append("Content-Length: $declaredLength\r\n")
            append("Connection: close\r\n\r\n")
        }
        output.write(headers.toByteArray(StandardCharsets.US_ASCII))
        output.flush()
        if (method == "HEAD") return

        activeSlowStreams.incrementAndGet()
        try {
            repeat(SLOW_STREAM_CHUNKS) {
                output.write(chunk)
                output.flush()
                Thread.sleep(SLOW_STREAM_DELAY_MILLIS)
            }
        } finally {
            activeSlowStreams.decrementAndGet()
        }
    }

    private fun respond(
        client: Socket,
        status: Int,
        reason: String,
        contentType: String,
        body: ByteArray,
        range: String?,
        method: String,
    ) {
        val headers = buildString {
            append("HTTP/1.1 $status $reason\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${body.size}\r\n")
            append("Accept-Ranges: bytes\r\n")
            if (range != null) append("Content-Range: $range\r\n")
            append("Connection: close\r\n\r\n")
        }
        val output = client.getOutputStream()
        output.write(headers.toByteArray(StandardCharsets.US_ASCII))
        if (method != "HEAD") output.write(body)
        output.flush()
    }

    private fun parseRange(value: String?, total: Int): IntRange? {
        if (value == null) return null
        if (!value.startsWith("bytes=")) return null
        val bounds = value.removePrefix("bytes=").split('-', limit = 2)
        val start = bounds.getOrNull(0)?.toIntOrNull() ?: return null
        val requestedEnd = bounds.getOrNull(1)?.takeIf(String::isNotBlank)?.toIntOrNull()
        if (start < 0 || start >= total) return null
        val end = (requestedEnd ?: (total - 1)).coerceAtMost(total - 1)
        if (end < start) return null
        return start..end
    }

    private data class Fixture(
        val bytes: ByteArray,
        val contentType: String,
    )

    private companion object {
        const val STREAMING_PLAYLIST_PATH = "/live/fixture/fixture/42.m3u8"
        const val STREAMING_SEGMENT_PATH = "/live/fixture/fixture/segment000.ts"
        const val SLOW_STREAM_CHUNKS = 512
        const val SLOW_STREAM_DELAY_MILLIS = 50L
        val STREAMING_PLAYLIST = """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXT-X-TARGETDURATION:2
            #EXT-X-MEDIA-SEQUENCE:0
            #EXTINF:2.0,
            segment000.ts
        """.trimIndent() + "\n"
    }
}
