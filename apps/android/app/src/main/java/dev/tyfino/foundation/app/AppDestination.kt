package dev.tyfino.foundation.app

import androidx.annotation.StringRes
import dev.tyfino.foundation.R

internal enum class AppDestination(
    val route: String,
    @StringRes val labelRes: Int,
) {
    Home("home", R.string.destination_home),
    Live("live", R.string.destination_live),
    Movies("movies", R.string.destination_movies),
    Series("series", R.string.destination_series),
    Settings("settings", R.string.destination_settings),
}
