package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import dev.tyfino.foundation.xtream.PlaylistInfo
import dev.tyfino.foundation.xtream.PlaylistRepository
import dev.tyfino.foundation.xtream.PlaylistResult
import dev.tyfino.foundation.xtream.PlaylistStatus
import dev.tyfino.foundation.xtream.ProviderEndpoint
import dev.tyfino.foundation.xtream.SavedXtreamAccount
import dev.tyfino.foundation.xtream.XtreamAccountStore
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistLifecycleTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pauseClearsMetadataAndLateResultCannotReplaceResumedRequest() {
        val owner = owner()
        val calls = AtomicInteger()
        val release = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        val repository = PlaylistRepository(Store()) {
            if (calls.incrementAndGet() == 2) {
                // Model transport work that continues after coroutine cancellation.
                withContext(NonCancellable) { release.await(); finished.complete(Unit) }
                ready(PlaylistStatus.Expired)
            } else ready(PlaylistStatus.Active)
        }
        try {
            compose.setContent { CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                TyfinoTheme { Column(Modifier.verticalScroll(rememberScrollState())) { PlaylistSummary(repository) } }
            } }
            compose.runOnIdle { assertEquals(0, calls.get()); owner.registry.currentState = Lifecycle.State.RESUMED }
            awaitStatus()
            compose.onNodeWithTag("playlist-status").assertTextContains("Active")
            compose.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
            compose.waitForIdle()
            compose.onNodeWithTag("playlist-status").assertDoesNotExist()
            compose.onNodeWithTag("playlist-provider").assertDoesNotExist()
            compose.runOnIdle { assertEquals(1, calls.get()); owner.registry.currentState = Lifecycle.State.RESUMED }
            compose.waitUntil(5_000) { calls.get() == 2 }
            compose.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
            compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
            awaitStatus()
            compose.onNodeWithTag("playlist-status").assertTextContains("Active")
            release.complete(Unit)
            compose.waitUntil(5_000) { finished.isCompleted }
            compose.waitForIdle()
            compose.onNodeWithTag("playlist-status").assertTextContains("Active")
            compose.runOnIdle { assertEquals(3, calls.get()) }
        } finally {
            release.complete(Unit)
        }
    }

    @Test fun leavingCompositionDiscardsPendingProviderMetadata() {
        val owner = owner()
        val visible = mutableStateOf(true)
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        val repository = PlaylistRepository(Store()) {
            started.complete(Unit)
            withContext(NonCancellable) { release.await(); finished.complete(Unit) }
            ready(PlaylistStatus.Active)
        }
        try {
            compose.setContent { CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                TyfinoTheme { if (visible.value) PlaylistSummary(repository) }
            } }
            compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
            compose.waitUntil(5_000) { started.isCompleted }
            compose.runOnIdle { visible.value = false }
            compose.waitForIdle()
            release.complete(Unit)
            compose.waitUntil(5_000) { finished.isCompleted }
            compose.waitForIdle()
            compose.onNodeWithTag("playlist-summary").assertDoesNotExist()
            compose.onNodeWithTag("playlist-provider").assertDoesNotExist()
        } finally {
            release.complete(Unit)
        }
    }

    private fun owner(): Owner {
        lateinit var owner: Owner
        compose.runOnUiThread { owner = Owner(); owner.registry.currentState = Lifecycle.State.STARTED }
        return owner
    }

    private fun ready(status: PlaylistStatus) = PlaylistResult.Ready(PlaylistInfo(status, null, null, null, null))

    private fun awaitStatus() = compose.waitUntil(5_000) {
        compose.onAllNodesWithTag("playlist-status").fetchSemanticsNodes().isNotEmpty()
    }

    private class Owner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }

    private class Store : XtreamAccountStore {
        private var account: SavedXtreamAccount? = SavedXtreamAccount("fixture-account", 1,
            ProviderEndpoint("https://provider.example", false), "fixture-user", "fixture-secret", false)
        override fun load() = account
        override fun save(account: SavedXtreamAccount) { this.account = account }
        override fun clear() { account = null }
    }
}
