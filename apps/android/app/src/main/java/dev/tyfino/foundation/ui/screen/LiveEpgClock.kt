package dev.tyfino.foundation.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/** Local presentation time only; no guide fetch, persistence or account authority. */
internal interface LiveEpgTimeSource {
    fun nowMillis(): Long
    suspend fun awaitNextTick()
}

internal object WallLiveEpgTimeSource : LiveEpgTimeSource {
    override fun nowMillis(): Long = System.currentTimeMillis()
    override suspend fun awaitNextTick() { delay(30_000L) }
}

@Composable
internal fun rememberLiveEpgTime(
    active: Boolean,
    clock: LiveEpgTimeSource = WallLiveEpgTimeSource,
): Long {
    val lifecycleOwner = LocalLifecycleOwner.current
    var now by remember(clock) { mutableLongStateOf(clock.nowMillis()) }
    LaunchedEffect(active, lifecycleOwner, clock) {
        if (!active) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                now = clock.nowMillis()
                clock.awaitNextTick()
            }
        }
    }
    return now
}
