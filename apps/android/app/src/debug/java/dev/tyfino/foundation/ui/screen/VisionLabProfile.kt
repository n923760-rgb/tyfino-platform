package dev.tyfino.foundation.ui.screen

/** Session-only laboratory state; never a provider or account preference. */
internal data class VisionLabProfile(
    val balanced: Boolean = false,
    val strength: Int = 50,
    val compareOriginal: Boolean = false,
) {
    init { require(strength in 0..100) }
    val effectiveStrength: Float
        get() = if (balanced && !compareOriginal) strength / 100f else 0f
}
