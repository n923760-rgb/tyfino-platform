package dev.tyfino.foundation.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.playback.MovieResumeRecord
import dev.tyfino.foundation.playback.MovieResumeRepository
import dev.tyfino.foundation.playback.MovieResumeStore
import dev.tyfino.foundation.playback.PlaybackFixtureServer
import dev.tyfino.foundation.playback.PlaybackSelection
import dev.tyfino.foundation.playback.PreviousLiveChannelController
import dev.tyfino.foundation.xtream.CatalogSection
import dev.tyfino.foundation.xtream.ProviderEndpoint
import dev.tyfino.foundation.xtream.SavedXtreamAccount
import dev.tyfino.foundation.xtream.XtreamAccountStore
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackLifecycleReleaseTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun leavingPlaybackDestinationClosesActiveMediaConnection() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val server = PlaybackFixtureServer(instrumentation.context.assets).also { it.start() }
        val account = SavedXtreamAccount(
            accountId = "fixture-account",
            generation = 1L,
            endpoint = ProviderEndpoint(
                baseUrl = server.baseUrl(),
                isCleartext = true,
            ),
            username = "fixture",
            password = "fixture",
            cleartextConsent = true,
        )
        val accountStore = FixedAccountStore(account)
        val resumeRepository = MovieResumeRepository(accountStore, EmptyResumeStore)
        val visible = mutableStateOf(true)

        try {
            compose.setContent {
                MaterialTheme {
                    if (visible.value) {
                        PlaybackScreen(
                            selection = PlaybackSelection(
                                accountId = account.accountId,
                                accountGeneration = account.generation,
                                section = CatalogSection.Live,
                                providerItemId = "42",
                                containerExtension = "m3u8",
                            ),
                            accountStore = accountStore,
                            resumeRepository = resumeRepository,
                            previousLiveChannelController = PreviousLiveChannelController(),
                            onPreviousLive = {},
                            onBack = {},
                        )
                    }
                }
            }

            waitUntil(
                message = "Playback never opened the bounded local streaming connection.",
                timeoutMillis = 10_000L,
            ) {
                server.activeSlowStreamCount() > 0
            }

            compose.runOnIdle { visible.value = false }
            compose.waitForIdle()

            waitUntil(
                message = "Playback connection remained active after the destination left composition.",
                timeoutMillis = 5_000L,
            ) {
                server.activeSlowStreamCount() == 0
            }
            assertEquals(0, server.activeSlowStreamCount())
        } finally {
            server.close()
        }
    }

    private fun waitUntil(
        message: String,
        timeoutMillis: Long,
        condition: () -> Boolean,
    ) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (System.nanoTime() < deadline) {
            if (condition()) return
            Thread.sleep(25L)
        }
        assertTrue(message, condition())
    }
}

private class FixedAccountStore(
    private val account: SavedXtreamAccount,
) : XtreamAccountStore {
    override fun load(): SavedXtreamAccount = account
    override fun save(account: SavedXtreamAccount) = Unit
    override fun clear() = Unit
}

private object EmptyResumeStore : MovieResumeStore {
    override fun load(accountId: String, providerItemId: String): MovieResumeRecord? = null
    override fun list(accountId: String, limit: Int): List<MovieResumeRecord> = emptyList()
    override fun upsert(record: MovieResumeRecord) = Unit
    override fun delete(accountId: String, providerItemId: String) = Unit
    override fun clearAccount(accountId: String) = Unit
}
