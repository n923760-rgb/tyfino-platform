package dev.tyfino.foundation.ui.screen

import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
import dev.tyfino.foundation.ui.components.MediaDetailsHero
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import dev.tyfino.foundation.xtream.CatalogItem
import dev.tyfino.foundation.xtream.MovieDetailsFailure
import dev.tyfino.foundation.xtream.MovieDetailsState
import dev.tyfino.foundation.xtream.SeriesFailure
import dev.tyfino.foundation.xtream.SeriesState
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailsAdaptiveLayoutTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun posterReflowsWithActualFontScaleAndWindowWidth() {
        var width by mutableStateOf(400.dp)
        var fontScale by mutableStateOf(1f)
        compose.setContent {
            ArabicPane(width, fontScale) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    MediaDetailsHero("عنوان العمل", "2026 • 8.5", artwork = null)
                }
            }
        }
        fun assertBesidePoster() {
            val artwork = compose.onNodeWithTag("media-details-artwork").getUnclippedBoundsInRoot()
            val title = compose.onNodeWithTag("media-details-title").getUnclippedBoundsInRoot()
            assertTrue("RTL heading must fit beside the poster", title.right <= artwork.left)
        }
        fun assertStacked() {
            compose.onNodeWithTag("media-details-title").performScrollTo().assertIsDisplayed()
            val artwork = compose.onNodeWithTag("media-details-artwork").getUnclippedBoundsInRoot()
            val title = compose.onNodeWithTag("media-details-title").getUnclippedBoundsInRoot()
            assertTrue("Heading must use the space below the poster", title.top >= artwork.bottom)
            assertTextFits("media-details-title", fontScale)
            assertTextFits("media-details-facts", fontScale)
        }
        assertBesidePoster()
        compose.runOnIdle { fontScale = 2f }
        assertStacked()
        compose.runOnIdle { fontScale = 1f; width = 240.dp }
        assertStacked()
        compose.runOnIdle { width = 400.dp }
        assertBesidePoster()
    }

    @Test fun compactLandscapeHeroRetainsFullWidthReadableHeadingWithoutArtwork() {
        compose.setContent {
            ArabicPane(240.dp, 2f) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    MediaDetailsHero("عنوان العمل", "2026 • 8.5", artwork = null, landscape = true)
                }
            }
        }
        compose.onNodeWithTag("media-details-title").performScrollTo().assertIsDisplayed()
        val artwork = compose.onNodeWithTag("media-details-artwork").getUnclippedBoundsInRoot()
        val title = compose.onNodeWithTag("media-details-title").getUnclippedBoundsInRoot()
        assertTrue(title.top >= artwork.bottom)
        val artworkWidth = (artwork.right - artwork.left).value
        val artworkHeight = (artwork.bottom - artwork.top).value
        assertEquals(200f, artworkWidth, 1f)
        assertEquals(9f / 16f, artworkHeight / artworkWidth, 0.02f)
        assertTextFits("media-details-title", 2f)
        assertTextFits("media-details-facts", 2f)
    }

    @Test fun compactArabicMovieActionsWrapAndEmitOnlyExplicitCallbacks() {
        var backs = 0
        var refreshes = 0
        var plays = 0
        compose.setContent {
            ArabicPane(240.dp, 2f) {
                MovieDetailsContent(
                    MovieSelection("fixture-account", 1L, item("movie")),
                    MovieDetailsState.Error(MovieDetailsFailure.NetworkUnavailable),
                    ownerChanged = false, canResume = true,
                    onBack = { backs++ }, onRefresh = { refreshes++ }, onPlay = { plays++ },
                )
            }
        }
        val back = compose.onNodeWithTag("movie-back").getUnclippedBoundsInRoot()
        val refresh = compose.onNodeWithTag("movie-refresh").getUnclippedBoundsInRoot()
        assertTrue("Refresh must wrap below Back instead of being squeezed", refresh.top >= back.bottom)
        compose.runOnIdle { assertEquals(0, backs); assertEquals(0, refreshes); assertEquals(0, plays) }
        for (tag in listOf("movie-refresh", "movie-play", "movie-back")) {
            compose.onNodeWithTag("movie-details").performScrollToNode(hasTestTag(tag))
            compose.onNodeWithTag(tag).assertIsDisplayed().performClick()
        }
        compose.onNodeWithTag("movie-details").performScrollToNode(hasTestTag("media-details-title"))
        assertTextFits("media-details-title", 2f)
        compose.runOnIdle { assertEquals(1, backs); assertEquals(1, refreshes); assertEquals(1, plays) }
    }

    @Test fun wrappedSeriesActionsKeepDirectionalFocusAndExplicitActivation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.setInTouchMode(false)
        try {
            var view: View? = null
            var input: InputModeManager? = null
            var backs = 0
            var refreshes = 0
            compose.setContent {
                view = LocalView.current
                input = LocalInputModeManager.current
                ArabicPane(240.dp, 2f) {
                    SeriesDetailsContent(
                        SeriesSelection("fixture-account", 1L, item("series")),
                        SeriesState.Error(SeriesFailure.NetworkUnavailable),
                        ownerChanged = false, preferredSeason = null, onSeasonSelected = {},
                        onBack = { backs++ }, onRefresh = { refreshes++ },
                    )
                }
            }
            compose.waitUntil(5_000) { compose.runOnIdle { view?.hasWindowFocus() == true } }
            compose.runOnIdle { assertTrue(input!!.requestInputMode(InputMode.Keyboard)) }
            compose.waitUntil(5_000) { compose.runOnIdle { input?.inputMode == InputMode.Keyboard } }
            val back = compose.onNodeWithTag("series-back").getUnclippedBoundsInRoot()
            val refresh = compose.onNodeWithTag("series-refresh").getUnclippedBoundsInRoot()
            assertTrue(refresh.top >= back.bottom)
            compose.onNodeWithTag("series-back")
                .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
                .assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
            compose.onNodeWithTag("series-refresh").assertIsFocused().assertIsDisplayed()
                .performKeyInput { pressKey(Key.Enter) }
            compose.runOnIdle { assertEquals(0, backs); assertEquals(1, refreshes) }
            compose.onNodeWithTag("series-refresh").performKeyInput { pressKey(Key.DirectionUp) }
            compose.onNodeWithTag("series-back").assertIsFocused()
                .performKeyInput { pressKey(Key.Enter) }
            compose.runOnIdle { assertEquals(1, backs); assertEquals(1, refreshes) }
        } finally {
            instrumentation.setInTouchMode(true)
        }
    }

    private fun assertTextFits(tag: String, expectedFontScale: Float) {
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
            val results = mutableListOf<TextLayoutResult>()
            assertTrue(action(results))
            assertEquals(expectedFontScale, results.single().layoutInput.density.fontScale, 0.001f)
            assertFalse("Text must remain readable without clipping", results.single().hasVisualOverflow)
        }
    }

    private fun item(id: String) = CatalogItem(
        id, "category", "عنوان العمل من المكتبة", 0, null, "8.5", "2026", "mp4",
    )

    @Composable private fun ArabicPane(width: Dp, fontScale: Float, content: @Composable () -> Unit) {
        val resources = context.createConfigurationContext(
            Configuration(context.resources.configuration).apply { setLocale(Locale("ar")) },
        ).resources
        CompositionLocalProvider(LocalResources provides resources,
            LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalDensity provides Density(context.resources.displayMetrics.density, fontScale)) {
            TyfinoTheme { Box(Modifier.width(width).height(360.dp)) { content() } }
        }
    }
}
