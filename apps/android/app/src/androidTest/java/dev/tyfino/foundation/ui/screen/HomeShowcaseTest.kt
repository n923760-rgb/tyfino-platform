package dev.tyfino.foundation.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.test.assertIsDisplayed
import android.content.res.Configuration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.R
import dev.tyfino.foundation.playback.ContinueWatchingItem
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import dev.tyfino.foundation.xtream.CatalogItem
import dev.tyfino.foundation.xtream.CatalogSection
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class HomeShowcaseTest {
    @get:Rule val compose = createComposeRule()

    @Test fun spotlightMovesBetweenMovieAndSeriesWithoutStartingPlayback() {
        var movie = ""
        var series = ""
        compose.setContent {
            TyfinoTheme {
                HomeShowcase(listOf(HomeHighlight(CatalogSection.Movies, item("movie")),
                    HomeHighlight(CatalogSection.Series, item("series"))),
                    onOpenMovie = { movie = it.providerId }, onOpenSeries = { series = it.providerId })
            }
        }
        compose.onNodeWithText("Title movie").assertIsDisplayed()
        compose.onNodeWithTag("home-featured-action").performClick()
        compose.onNodeWithTag("home-showcase-next").assertIsDisplayed().performClick()
        compose.onNodeWithText("Title series").assertIsDisplayed()
        compose.onNodeWithTag("home-featured-action").performClick()
        compose.runOnIdle { assertEquals("movie", movie); assertEquals("series", series) }
        compose.onNodeWithTag("home-showcase-next").assertIsDisplayed().performClick()
        compose.onNodeWithText("Title movie").assertIsDisplayed()
        compose.onNodeWithTag("home-showcase-previous").assertIsDisplayed().performClick()
        compose.onNodeWithText("Title series").assertIsDisplayed()
    }

    @Test fun continueWatchingDisplaysProgressAndUsesTheResumeAction() {
        var resumed = ""
        compose.setContent {
            TyfinoTheme {
                Column {
                    HomeContinueWatching(listOf(ContinueWatchingItem(item("movie"), 42)), emptyList(),
                        onMovie = { resumed = it.providerId }, onSeries = {}, tileWidth = 128.dp)
                }
            }
        }
        compose.onNodeWithTag("home-continue-watching").assertExists()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithTag("home-resume-movie-movie").assertContentDescriptionEquals(
            "Title movie, ${context.getString(R.string.continue_watching_progress, 42)}")
        compose.onNodeWithTag("home-resume-movie-movie").performClick()
        compose.runOnIdle { assertEquals("movie", resumed) }
    }

    @Test fun compactArabicLargeFontKeepsShowcaseControlsReachable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resources = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocale(Locale("ar"))
        }).resources
        var opened = ""
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources,
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(context.resources.displayMetrics.density, 1.6f)) {
                TyfinoTheme {
                    Column(Modifier.width(280.dp).verticalScroll(rememberScrollState())) {
                        HomeShowcase(listOf(HomeHighlight(CatalogSection.Movies, item("movie")),
                            HomeHighlight(CatalogSection.Series, item("series"))),
                            onOpenMovie = {}, onOpenSeries = { opened = it.providerId })
                    }
                }
            }
        }
        compose.onNodeWithTag("home-showcase-next").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("home-featured-action").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("series", opened) }
    }

    private fun item(id: String) = CatalogItem(id, "category", "Title $id", 0,
        null, "8", "2026", "mp4", 1_000)
}
