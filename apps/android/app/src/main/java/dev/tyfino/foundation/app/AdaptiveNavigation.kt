package dev.tyfino.foundation.app

import androidx.window.core.layout.WindowSizeClass

internal enum class AppNavigationType {
    BottomBar,
    Rail,
    Sidebar,
}

internal fun navigationTypeFor(windowSizeClass: WindowSizeClass): AppNavigationType = when {
    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) ->
        AppNavigationType.Sidebar
    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) ->
        AppNavigationType.Rail
    else -> AppNavigationType.BottomBar
}
