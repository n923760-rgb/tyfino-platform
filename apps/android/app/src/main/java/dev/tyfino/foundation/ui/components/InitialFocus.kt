package dev.tyfino.foundation.ui.components

import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalView

/** Gives D-pad/keyboard users one deterministic entry target after the screen is attached. */
@Composable
internal fun rememberInitialFocusRequester(key: Any? = Unit): FocusRequester {
    val requester = remember { FocusRequester() }
    val view = LocalView.current

    DisposableEffect(view, requester, key) {
        val listener = ViewTreeObserver.OnWindowFocusChangeListener { hasWindowFocus ->
            if (hasWindowFocus) requester.requestFocus()
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
            if (view.hasWindowFocus()) requester.requestFocus()
        }
    }
    return requester
}
