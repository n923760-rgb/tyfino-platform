package dev.tyfino.foundation.app

import androidx.window.core.layout.WindowSizeClass
import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveNavigationTest {
    @Test
    fun compactWindowUsesBottomBar() {
        assertEquals(
            AppNavigationType.BottomBar,
            navigationTypeFor(WindowSizeClass(599, 480)),
        )
    }

    @Test
    fun widerWindowsUseNavigationRail() {
        assertEquals(
            AppNavigationType.Rail,
            navigationTypeFor(WindowSizeClass(600, 480)),
        )
        assertEquals(
            AppNavigationType.Rail,
            navigationTypeFor(WindowSizeClass(840, 480)),
        )
    }
}
