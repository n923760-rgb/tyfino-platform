package dev.tyfino.foundation.ui.screen

import org.junit.Assert.*
import org.junit.Test

class VisionLabProfileTest {
    @Test fun originalAndComparisonAreIdentityAndKeepChosenStrength() {
        val chosen = VisionLabProfile(balanced = true, strength = 75)
        assertEquals(.75f, chosen.effectiveStrength, 0f)
        assertEquals(0f, chosen.copy(balanced = false).effectiveStrength, 0f)
        val comparison = chosen.copy(compareOriginal = true)
        assertEquals(0f, comparison.effectiveStrength, 0f)
        assertEquals(chosen, comparison.copy(compareOriginal = false))
        assertEquals(0f, chosen.copy(strength = 0).effectiveStrength, 0f)
    }
    @Test fun invalidStrengthNeverReachesTheShader() {
        for (strength in listOf(-1, 101)) assertThrows(IllegalArgumentException::class.java) { VisionLabProfile(strength = strength) }
        for (strength in listOf(Float.NaN, Float.POSITIVE_INFINITY, -.1f, 1.1f)) {
            assertThrows(IllegalArgumentException::class.java) { VisionBalancedEffect(strength) }
        }
    }
}
