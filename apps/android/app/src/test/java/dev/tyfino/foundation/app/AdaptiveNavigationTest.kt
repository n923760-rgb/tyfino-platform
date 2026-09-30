package dev.tyfino.foundation.app

import androidx.window.core.layout.WindowSizeClass
import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveNavigationTest {
    @Test fun compactWindowUsesBottomBar() {
        assertEquals(AppNavigationType.BottomBar, navigationTypeFor(WindowSizeClass(599, 480)))
    }

    @Test fun mediumWindowUsesCompactRail() {
        assertEquals(AppNavigationType.Rail, navigationTypeFor(WindowSizeClass(600, 480)))
        assertEquals(AppNavigationType.Rail, navigationTypeFor(WindowSizeClass(839, 480)))
    }

    @Test fun expandedWindowUsesLabeledSidebar() {
        assertEquals(AppNavigationType.Sidebar, navigationTypeFor(WindowSizeClass(840, 480)))
        assertEquals(AppNavigationType.Sidebar, navigationTypeFor(WindowSizeClass(1200, 480)))
    }

    @Test fun heightDoesNotChangeTheAvailableWidthDecision() {
        assertEquals(AppNavigationType.Sidebar, navigationTypeFor(WindowSizeClass(840, 200)))
        assertEquals(AppNavigationType.BottomBar, navigationTypeFor(WindowSizeClass(599, 1000)))
    }
}
