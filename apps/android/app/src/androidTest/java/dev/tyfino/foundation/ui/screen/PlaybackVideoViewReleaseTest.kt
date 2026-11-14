@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package dev.tyfino.foundation.ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import java.util.Collections
import java.util.IdentityHashMap
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackVideoViewReleaseTest {
    @get:Rule val compose = createComposeRule()

    @Test fun discardedViewsDetachPlayerAndListenersWithoutReleasingTheExternalOwner() {
        lateinit var player: ForwardingPlayer
        val listeners = Collections.newSetFromMap(IdentityHashMap<Player.Listener, Boolean>())
        var releases = 0
        var visible by mutableStateOf(true)
        compose.runOnIdle {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            player = object : ForwardingPlayer(ExoPlayer.Builder(context).build()) {
                override fun addListener(listener: Player.Listener) {
                    listeners.add(listener)
                    super.addListener(listener)
                }
                override fun removeListener(listener: Player.Listener) {
                    listeners.remove(listener)
                    super.removeListener(listener)
                }
                override fun release() { releases++; super.release() }
            }
        }
        try {
            compose.setContent {
                TyfinoTheme {
                    if (visible) PlaybackVideoView(player, PlaybackAspectMode.Fit)
                }
            }
            val discarded = mutableListOf<PlayerView>()
            repeat(3) {
                compose.runOnIdle { visible = true }
                lateinit var current: PlayerView
                onView(isAssignableFrom(PlayerView::class.java)).check { view, error ->
                    if (error != null) throw error
                    current = view as PlayerView
                    assertSame(player, current.player)
                    assertTrue(listeners.isNotEmpty())
                    assertTrue(discarded.none { it === current })
                }
                compose.runOnIdle { visible = false }
                compose.waitForIdle()
                compose.runOnIdle {
                    assertNull("Discarded native view must stop retaining its player", current.player)
                    assertTrue("Discarded view/control listeners must be removed", listeners.isEmpty())
                    assertEquals("The view must not release the externally owned engine", 0, releases)
                }
                discarded += current
            }
        } finally {
            compose.runOnIdle { visible = false }
            compose.waitForIdle()
            compose.runOnIdle { player.release(); assertEquals(1, releases) }
        }
    }
}
