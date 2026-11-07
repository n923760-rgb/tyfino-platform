package dev.tyfino.foundation.ui.screen

import android.content.res.Configuration
import android.view.View
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.ui.components.FocusVisibleButton
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import dev.tyfino.foundation.xtream.LiveEpgFailure
import dev.tyfino.foundation.xtream.LiveEpgProgram
import dev.tyfino.foundation.xtream.LiveEpgState
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.channels.Channel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveEpgClockTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Before fun directionalInput() { instrumentation.setInTouchMode(false) }
    @After fun restoreInput() { instrumentation.setInTouchMode(true) }

    @Test fun currentAndNextAdvanceWithoutRefreshInEnglish() = guideClock(LayoutDirection.Ltr, false)
    @Test fun savedCurrentAndNextAdvanceWithoutRefreshInArabic() = guideClock(LayoutDirection.Rtl, true)

    private fun guideClock(direction: LayoutDirection, stale: Boolean) {
        val start = 1_000_000L
        val clock = ControlledClock(start)
        val programs = listOf(
            program("First", start - 30_000L, start + 30_000L),
            program("Second", start + 30_000L, start + 60_000L),
            program("Third", start + 60_000L, start + 90_000L),
        )
        val state = if (stale) LiveEpgState.Stale(programs, start, LiveEpgFailure.Timeout)
            else LiveEpgState.Content(programs, start, false)
        val context = instrumentation.targetContext
        val resources = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocale(Locale(if (direction == LayoutDirection.Rtl) "ar" else "en"))
        }).resources
        var mounted by mutableStateOf(false)
        var refreshes = 0
        var dismissed = 0
        lateinit var host: View
        lateinit var input: InputModeManager
        compose.setContent {
            host = LocalView.current
            input = LocalInputModeManager.current
            TyfinoTheme {
                FocusVisibleButton("Input setup", {})
                if (mounted) CompositionLocalProvider(
                    LocalResources provides resources, LocalLayoutDirection provides direction,
                ) {
                    LiveEpgDialog(state, { refreshes++ }, { dismissed++ }, clock)
                }
            }
        }
        compose.waitUntil(5_000) { compose.runOnIdle { host.hasWindowFocus() } }
        compose.runOnIdle { assertTrue(input.requestInputMode(InputMode.Keyboard)) }
        compose.waitUntil(5_000) { compose.runOnIdle { input.inputMode == InputMode.Keyboard } }
        compose.runOnIdle { mounted = true }
        compose.waitForIdle()
        awaitKeyboardDialogWindow()
        compose.waitUntil(5_000) {
            compose.onNodeWithTag("epg-refresh").fetchSemanticsNode().config[SemanticsProperties.Focused]
        }
        compose.onNodeWithTag("epg-refresh").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("epg-close").assertIsFocused()
        compose.onNodeWithTag("epg-current-program").assertTextEquals("First")
        compose.onNodeWithTag("epg-next-program").assertTextEquals("Second")

        // The clock is not snapshot state: only the actual production ticker can publish this change.
        clock.now.set(start + 30_000L)
        compose.onNodeWithTag("epg-current-program").assertTextEquals("First")
        pulse(clock)
        compose.waitUntil(5_000) {
            compose.onNodeWithTag("epg-current-program").fetchSemanticsNode()
                .config[SemanticsProperties.Text].single().text == "Second"
        }
        compose.onNodeWithTag("epg-current-program").assertTextEquals("Second")
        compose.onNodeWithTag("epg-next-program").assertTextEquals("Third")
        compose.onNodeWithTag("epg-close").assertIsFocused()
        compose.runOnIdle { assertEquals(0, refreshes); assertEquals(0, dismissed) }
        compose.runOnIdle { mounted = false }
        compose.onNodeWithTag("live-epg-dialog").assertDoesNotExist()
        compose.waitUntil(5_000) { !clock.waiting.get() }
        val stoppedReads = clock.reads.get()
        assertFalse(clock.ticks.trySend(Unit).isSuccess)
        compose.waitForIdle()
        assertEquals(stoppedReads, clock.reads.get())
    }

    @Test fun clockStopsWithoutProgramsInBackgroundAndAfterDisposalAndRefreshesOnResume() {
        val clock = ControlledClock(100L)
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry(this)
            override val lifecycle: Lifecycle get() = registry
        }
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        var active by mutableStateOf(true)
        var mounted by mutableStateOf(true)
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                TyfinoTheme {
                    if (mounted) Text(rememberLiveEpgTime(active, clock).toString(), Modifier.testTag("clock"))
                }
            }
        }
        compose.onNodeWithTag("clock").assertTextEquals("100")
        clock.now.set(200L)
        pulse(clock)
        compose.waitUntil(5_000) { clock.reads.get() >= 3 }
        compose.onNodeWithTag("clock").assertTextEquals("200")
        compose.runOnIdle { active = false }
        assertStopped(clock)
        clock.now.set(300L)
        compose.runOnIdle { active = true }
        compose.waitUntil(5_000) { clock.waiting.get() }
        compose.onNodeWithTag("clock").assertTextEquals("300")
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        assertStopped(clock)
        clock.now.set(400L)
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.waitUntil(5_000) { clock.waiting.get() }
        compose.onNodeWithTag("clock").assertTextEquals("400")
        compose.runOnIdle { mounted = false }
        compose.onNodeWithTag("clock").assertDoesNotExist()
        assertStopped(clock)
    }

    private fun pulse(clock: ControlledClock) {
        compose.waitUntil(5_000) { clock.waiting.get() }
        runBlocking { withTimeout(5_000L) { clock.ticks.send(Unit) } }
    }

    private fun assertStopped(clock: ControlledClock) {
        compose.waitUntil(5_000) { !clock.waiting.get() }
        val reads = clock.reads.get()
        assertFalse("No background tick receiver may remain", clock.ticks.trySend(Unit).isSuccess)
        compose.waitForIdle()
        assertEquals(reads, clock.reads.get())
    }

    private fun program(title: String, start: Long, end: Long) =
        LiveEpgProgram("fixture-account", "fixture-channel", title, null, start, end)

    private class ControlledClock(initial: Long) : LiveEpgTimeSource {
        val now = AtomicLong(initial)
        val reads = AtomicInteger(0)
        val waiting = AtomicBoolean(false)
        val ticks = Channel<Unit>(Channel.RENDEZVOUS)
        override fun nowMillis(): Long { reads.incrementAndGet(); return now.get() }
        override suspend fun awaitNextTick() {
            waiting.set(true)
            try { ticks.receive() } finally { waiting.set(false) }
        }
    }
}
