package dev.tyfino.foundation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester

/** Gives D-pad/keyboard users one deterministic entry target after the screen is attached. */
@Composable
internal fun rememberInitialFocusRequester(key: Any? = Unit): FocusRequester {
    val requester = remember { FocusRequester() }
    LaunchedEffect(requester, key) {
        repeat(2) {
            withFrameNanos { }
            requester.requestFocus()
        }
    }
    return requester
}
