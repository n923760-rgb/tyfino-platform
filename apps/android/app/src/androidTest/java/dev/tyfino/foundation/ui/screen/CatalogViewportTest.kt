package dev.tyfino.foundation.ui.screen

import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.R
import dev.tyfino.foundation.playback.ContinueWatchingItem
import dev.tyfino.foundation.ui.components.ProductHeader
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import dev.tyfino.foundation.xtream.CatalogCategory
import dev.tyfino.foundation.xtream.CatalogItem
import dev.tyfino.foundation.xtream.CatalogSection
import dev.tyfino.foundation.xtream.CatalogState
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogViewportTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val arabicResources get() = context.createConfigurationContext(
        Configuration(context.resources.configuration).apply { setLocale(Locale("ar")) }
    ).resources
    private val category = CatalogCategory("fixture-category", "قسم التجربة", 0)
    private val item = CatalogItem("fixture-item", category.providerId, "محتوى التجربة", 0,
        null, null, null, null)
    private val resume = item.copy(providerId = "fixture-resume", name = "فيلم سابق")

    @Test fun shortArabicViewportKeepsResultsAndScrollableResumeControlsReachableAfterResize() {
        var section by mutableStateOf(CatalogSection.Movies)
        var height by mutableStateOf(480.dp)
        var scale by mutableStateOf(1f)
        var selected by mutableStateOf<CatalogCategory?>(null)
        val played = mutableListOf<CatalogItem>()
        var refreshes = 0
        compose.setContent { ArabicPane(height, scale) {
            CatalogViewport(
                controlsScrollState = remember(section) { ScrollState(0) },
                controls = {
                    ProductHeader("TYFINO")
                    CatalogFilterTabs(false, false, {}, {}, {})
                    CatalogSearchField("", {})
                    if (section == CatalogSection.Movies) {
                        ContinueWatchingStrip(listOf(ContinueWatchingItem(resume, 35))) { played += it }
                    }
                },
                content = {
                    CatalogBrowsePane(CatalogState.Content(listOf(category), 1L, false),
                        selected?.providerId, selected?.name,
                        if (selected == null) CatalogState.Empty else CatalogState.Content(listOf(item), 1L, false),
                        section, { selected = it }, { refreshes++ }, { refreshes++ }, { played += it })
                },
            )
        } }
        compose.runOnIdle { height = 320.dp; scale = 1.6f }
        for (current in CatalogSection.entries) {
            compose.runOnIdle { section = current; selected = null }
            val controls = compose.onNodeWithTag("catalog-controls").fetchSemanticsNode().boundsInRoot
            val results = compose.onNodeWithTag("catalog-results").fetchSemanticsNode().boundsInRoot
            assertTrue("Controls cannot overlay results", controls.bottom <= results.top)
            assertTrue("Short windows retain a nonzero results region", results.height > 0f)
            assertTrue("Results retain at least as much height as overflowing controls", results.height >= controls.height)
            compose.onNodeWithContentDescription(category.name).assertIsDisplayed().performClick()
            compose.onNodeWithContentDescription(item.name).assertIsDisplayed().performClick()
        }
        val description = resume.name + ", " + arabicResources.getString(R.string.continue_watching_progress, 35)
        compose.runOnIdle { section = CatalogSection.Movies }
        // Scroll the rail through its vertical parent before targeting its horizontal child card.
        compose.onNodeWithTag("continue-watching").performScrollTo().assertIsDisplayed()
        val verticalScroll = compose.onNodeWithTag("catalog-controls").fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue("The controls must actually scroll vertically", verticalScroll.value() > 0f)
        compose.onNodeWithContentDescription(description).assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(listOf(item, item, item, resume), played)
            assertEquals(0, refreshes)
            height = 480.dp; scale = 1f
        }
        val controls = compose.onNodeWithTag("catalog-controls").fetchSemanticsNode().boundsInRoot
        val results = compose.onNodeWithTag("catalog-results").fetchSemanticsNode().boundsInRoot
        assertTrue("Resizing keeps independent content regions", controls.bottom <= results.top && results.height > 0f)
    }

    @Test fun searchGuidanceCountsCodePointsAndClearAndImeDispatchOnlyTheirOwnedActions() {
        var query by mutableStateOf("")
        val changes = mutableListOf<String>()
        var hides = 0
        val keyboard = object : SoftwareKeyboardController {
            override fun show() = Unit
            override fun hide() { hides++ }
        }
        compose.setContent { ArabicPane(360.dp, 1.6f) {
            CompositionLocalProvider(LocalSoftwareKeyboardController provides keyboard) {
                CatalogSearchField(query, { query = it; changes += it })
            }
        } }
        compose.onNodeWithTag("catalog-clear-search").assertDoesNotExist()
        compose.onNodeWithTag("catalog-search").performTextInput("🎬")
        compose.onNodeWithTag("catalog-search-guidance", useUnmergedTree = true)
            .assertTextEquals(arabicResources.getString(R.string.catalog_search_minimum))
        compose.onNodeWithTag("catalog-search").performTextInput("ا")
        compose.onNodeWithTag("catalog-search-guidance", useUnmergedTree = true)
            .assertTextEquals(arabicResources.getString(R.string.catalog_search_scope))
        compose.onNodeWithTag("catalog-search").performImeAction()
        compose.runOnIdle { assertEquals(1, hides); assertEquals(listOf("🎬", "🎬ا"), changes) }
        compose.onNodeWithTag("catalog-clear-search")
            .assertContentDescriptionEquals(arabicResources.getString(R.string.catalog_clear_search))
            .assertIsDisplayed().performClick()
        compose.onNodeWithTag("catalog-clear-search").assertDoesNotExist()
        compose.onNodeWithTag("catalog-search-guidance", useUnmergedTree = true).assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf("🎬", "🎬ا", ""), changes); query = "  " }
        compose.onNodeWithTag("catalog-search-guidance", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("catalog-clear-search").performClick()
        compose.runOnIdle { assertEquals("", query); assertEquals(listOf("🎬", "🎬ا", "", ""), changes) }
    }

    @Test fun keyboardMovesFromSearchControlsToResultsAndActivatesEachCallbackOnce() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.setInTouchMode(false)
        try {
            var view: View? = null
            var input: InputModeManager? = null
            var query by mutableStateOf("فيلم")
            val changes = mutableListOf<String>()
            var plays = 0
            compose.setContent {
                view = LocalView.current
                input = LocalInputModeManager.current
                ArabicPane(320.dp, 1f) {
                    CatalogViewport(
                        controls = { CatalogSearchField(query, { query = it; changes += it }) },
                        content = {
                            CatalogTile(item.name, false, { plays++ },
                                Modifier.fillMaxWidth().testTag("viewport-keyboard-item"))
                        },
                    )
                }
            }
            compose.waitUntil(5_000) { compose.runOnIdle { view?.hasWindowFocus() == true } }
            compose.runOnIdle { assertTrue(input!!.requestInputMode(InputMode.Keyboard)) }
            compose.waitUntil(5_000) { compose.runOnIdle { input?.inputMode == InputMode.Keyboard } }
            compose.onNodeWithTag("catalog-clear-search")
                .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
                .assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
            compose.onNodeWithTag("viewport-keyboard-item").assertIsFocused()
                .performKeyInput { pressKey(Key.Enter) }
            compose.runOnIdle { assertEquals(1, plays); assertTrue(changes.isEmpty()) }
            compose.onNodeWithTag("catalog-clear-search")
                .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
                .assertIsFocused().performKeyInput { pressKey(Key.Enter) }
            compose.runOnIdle { assertEquals(listOf(""), changes); assertEquals(1, plays) }
        } finally {
            instrumentation.setInTouchMode(true)
        }
    }

    @Composable private fun ArabicPane(height: Dp, scale: Float, content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalResources provides arabicResources,
            LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalDensity provides Density(context.resources.displayMetrics.density, scale),
        ) { TyfinoTheme { Box(Modifier.width(280.dp).height(height)) { content() } } }
    }
}
