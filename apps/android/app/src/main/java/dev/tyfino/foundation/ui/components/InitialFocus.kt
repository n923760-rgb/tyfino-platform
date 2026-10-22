package dev.tyfino.foundation.ui.components

import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalView

/** Gives D-pad/keyboard users one deterministic entry target after the screen is attached. */
@Composable
internal fun rememberInitialFocusRequester(key: Any? = Unit): FocusRequester {
    val requester = remember { FocusRequester() }
    val view = LocalView.current
    val completed = remember(view, requester, key) { mutableStateOf(false) }

    DisposableEffect(view, requester, key) {
        val listener = ViewTreeObserver.OnWindowFocusChangeListener { hasWindowFocus ->
            if (hasWindowFocus && !completed.value) {
                completed.value = requester.requestFocus()
            }
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        onDispose {
            if (view.viewTreeObserver.isAlive) {
                view.viewTreeObserver.removeOnWindowFocusChangeListener(listener)
            }
        }
    }

    LaunchedEffect(view, requester, key) {
        repeat(2) {
            withFrameNanos { }
            // A later frame/window notification must not undo the user's next focus move.
            if (completed.value) return@LaunchedEffect
            if (view.hasWindowFocus()) completed.value = requester.requestFocus()
        }
    }
    return requester
}
