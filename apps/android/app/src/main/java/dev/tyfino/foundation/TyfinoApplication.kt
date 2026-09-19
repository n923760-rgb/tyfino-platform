package dev.tyfino.foundation

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.Path.Companion.toOkioPath
import okio.buffer

class TyfinoApplication : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, ARTWORK_MEMORY_PERCENT)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("catalog_artwork").toOkioPath())
                .maxSizeBytes(ARTWORK_DISK_BYTES)
                .build()
        }
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = { artworkHttpClient() }))
        }
        .build()

    private fun artworkHttpClient(): OkHttpClient {
        val dispatcher = Dispatcher().apply {
            maxRequests = ARTWORK_MAX_REQUESTS
            maxRequestsPerHost = ARTWORK_MAX_REQUESTS_PER_HOST
        }
        return OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectTimeout(ARTWORK_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(ARTWORK_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(ARTWORK_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .addNetworkInterceptor(BoundedArtworkResponseInterceptor())
            .build()
    }

    private companion object {
        const val ARTWORK_MEMORY_PERCENT = 0.08
        const val ARTWORK_DISK_BYTES = 64L * 1024L * 1024L
        const val ARTWORK_MAX_REQUESTS = 8
        const val ARTWORK_MAX_REQUESTS_PER_HOST = 4
        const val ARTWORK_CONNECT_TIMEOUT_SECONDS = 5L
        const val ARTWORK_READ_TIMEOUT_SECONDS = 8L
        const val ARTWORK_CALL_TIMEOUT_SECONDS = 12L
    }
}

internal object ArtworkNetworkPolicy {
    const val MAX_RESPONSE_BYTES = 8L * 1024L * 1024L

    fun acceptsContentLength(contentLength: Long): Boolean =
        contentLength < 0L || contentLength <= MAX_RESPONSE_BYTES
}

private class BoundedArtworkResponseInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val response = chain.proceed(chain.request())
        val body = response.body ?: return response
        if (!ArtworkNetworkPolicy.acceptsContentLength(body.contentLength())) {
            response.close()
            throw IOException("Artwork response exceeds limit")
        }
        val bounded = object : ForwardingSource(body.source()) {
            private var received = 0L

            override fun read(sink: Buffer, byteCount: Long): Long {
                val remaining = ArtworkNetworkPolicy.MAX_RESPONSE_BYTES - received
                if (remaining < 0L) throw IOException("Artwork response exceeds limit")
                val read = super.read(sink, minOf(byteCount, remaining + 1L))
                if (read > 0L) {
                    received += read
                    if (received > ArtworkNetworkPolicy.MAX_RESPONSE_BYTES) {
                        throw IOException("Artwork response exceeds limit")
                    }
                }
                return read
            }
        }.buffer()
        return response.newBuilder()
            .body(bounded.asResponseBody(body.contentType(), body.contentLength()))
            .build()
    }
}
