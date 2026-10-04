package dev.tyfino.foundation.ui.screen

import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import dev.tyfino.foundation.xtream.CatalogItem
import dev.tyfino.foundation.xtream.CatalogSection
import dev.tyfino.foundation.xtream.MovieDetailsFailure
import dev.tyfino.foundation.xtream.MovieDetailsState
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CinematicDesignTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val arabicResources get() = context.createConfigurationContext(
        Configuration(context.resources.configuration).apply { setLocale(Locale("ar")) }
    ).resources

    @Test fun rtlShortcutsReflowForLargeTextWithoutEmittingNavigation() {
        var fontScale by mutableStateOf(1f)
        var width by mutableStateOf(328.dp)
        val opened = mutableListOf<CatalogSection>()
        compose.setContent { ArabicPane(width, fontScale) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                HomeBrowseSections { opened += it }
            }
        } }
        val live = compose.onNodeWithTag("home-browse-live").fetchSemanticsNode().boundsInRoot
        val movies = compose.onNodeWithTag("home-browse-movies").fetchSemanticsNode().boundsInRoot
        val series = compose.onNodeWithTag("home-browse-series").fetchSemanticsNode().boundsInRoot
        assertEquals(live.center.y, movies.center.y, 1f)
        assertEquals(movies.center.y, series.center.y, 1f)
        assertTrue(live.left > movies.left && movies.left > series.left)
        compose.runOnIdle { assertTrue(opened.isEmpty()); width = 280.dp; fontScale = 1.6f }
        val denseLive = compose.onNodeWithTag("home-browse-live").fetchSemanticsNode().boundsInRoot
        val denseMovies = compose.onNodeWithTag("home-browse-movies").fetchSemanticsNode().boundsInRoot
        val denseSeries = compose.onNodeWithTag("home-browse-series").fetchSemanticsNode().boundsInRoot
        assertTrue(denseLive.bottom <= denseMovies.top && denseMovies.bottom <= denseSeries.top)
        compose.runOnIdle { assertTrue(opened.isEmpty()) }
        for ((section, label) in listOf(CatalogSection.Live to R.string.destination_live,
            CatalogSection.Movies to R.string.destination_movies, CatalogSection.Series to R.string.destination_series)) {
            compose.onNodeWithText(arabicResources.getString(label), useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                    val layouts = mutableListOf<TextLayoutResult>()
                    assertTrue(action(layouts))
                    assertFalse(layouts.single().hasVisualOverflow)
                }
            compose.onNodeWithTag("home-browse-${section.name.lowercase()}")
                .performScrollTo().assertIsDisplayed().performClick()
        }
        compose.runOnIdle { assertEquals(CatalogSection.entries.toList(), opened) }
    }

    @Test fun shortcutsAcceptDirectionalActivationInBothLayouts() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.setInTouchMode(false)
        try {
            var view: View? = null
            var input: InputModeManager? = null
            var fontScale by mutableStateOf(1f)
            var width by mutableStateOf(328.dp)
            val opened = mutableListOf<CatalogSection>()
            compose.setContent {
                view = LocalView.current
                input = LocalInputModeManager.current
                ArabicPane(width, fontScale) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        HomeBrowseSections { opened += it }
                    }
                }
            }
            compose.waitUntil(5_000) { compose.runOnIdle { view?.hasWindowFocus() == true } }
            compose.runOnIdle { assertTrue(input!!.requestInputMode(InputMode.Keyboard)) }
            compose.waitUntil(5_000) { compose.runOnIdle { input?.inputMode == InputMode.Keyboard } }
            compose.onNodeWithTag("home-browse-live")
                .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
                .assertIsFocused().performKeyInput { pressKey(Key.DirectionLeft) }
            compose.onNodeWithTag("home-browse-movies").assertIsFocused()
                .performKeyInput { pressKey(Key.Enter) }
            compose.runOnIdle { assertEquals(listOf(CatalogSection.Movies), opened); width = 280.dp; fontScale = 1.6f }
            compose.onNodeWithTag("home-browse-movies")
                .performScrollTo().performSemanticsAction(SemanticsActions.RequestFocus) { it() }
                .assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
            compose.onNodeWithTag("home-browse-series").assertIsFocused()
                .performKeyInput { pressKey(Key.Enter) }
            compose.runOnIdle { assertEquals(listOf(CatalogSection.Movies, CatalogSection.Series), opened) }
        } finally {
            instrumentation.setInTouchMode(true)
        }
    }

    @Test fun compactArabicMovieFallbackKeepsTitleAndActionsReachableWithoutArtwork() {
        var plays = 0
        var backs = 0
        var refreshes = 0
        val title = "فيلم من المكتبة"
        compose.setContent { ArabicPane(280.dp, 1.6f) {
            MovieDetailsContent(
                MovieSelection("fixture-account", 1L,
                    CatalogItem("movie", "category", title, 0, null, "7.5", "2024", "mp4")),
                MovieDetailsState.Error(MovieDetailsFailure.NetworkUnavailable),
                ownerChanged = false, canResume = false,
                onBack = { backs++ }, onRefresh = { refreshes++ }, onPlay = { plays++ },
            )
        } }
        compose.onNodeWithTag("movie-details").performScrollToNode(hasText(title))
        compose.onNodeWithText(title).assertIsDisplayed()
        compose.onNodeWithTag("movie-details").performScrollToNode(hasTestTag("movie-play"))
        compose.onNodeWithTag("movie-play").assertIsDisplayed().performClick()
        compose.onNodeWithTag("movie-details").performScrollToNode(hasTestTag("movie-back"))
        compose.onNodeWithTag("movie-back").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, plays); assertEquals(1, backs); assertEquals(0, refreshes) }
    }

    @Composable private fun ArabicPane(width: Dp, fontScale: Float, content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalResources provides arabicResources,
            LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalDensity provides Density(context.resources.displayMetrics.density, fontScale)) {
            TyfinoTheme { Box(Modifier.width(width).height(360.dp)) { content() } }
        }
    }
}
