package dev.tyfino.foundation.app

import androidx.window.core.layout.WindowSizeClass

internal enum class AppNavigationType {
    BottomBar,
    Rail,
}

internal fun navigationTypeFor(windowSizeClass: WindowSizeClass): AppNavigationType =
    if (windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
        AppNavigationType.Rail
    } else {
        AppNavigationType.BottomBar
    }
