package dev.tyfino.foundation.app

import androidx.annotation.StringRes
import androidx.annotation.DrawableRes
import dev.tyfino.foundation.R

internal enum class AppDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    Home("home", R.string.destination_home, R.drawable.ic_nav_home),
    Live("live", R.string.destination_live, R.drawable.ic_nav_live),
    Movies("movies", R.string.destination_movies, R.drawable.ic_nav_movies),
    Series("series", R.string.destination_series, R.drawable.ic_nav_series),
    Settings("settings", R.string.destination_settings, R.drawable.ic_nav_settings),
}
