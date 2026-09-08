package dev.tyfino.foundation.app

import androidx.annotation.StringRes
import dev.tyfino.foundation.R

internal enum class AppDestination(
    val route: String,
    @StringRes val labelRes: Int,
) {
    Foundation("foundation", R.string.destination_foundation),
    Settings("settings", R.string.destination_settings),
}
