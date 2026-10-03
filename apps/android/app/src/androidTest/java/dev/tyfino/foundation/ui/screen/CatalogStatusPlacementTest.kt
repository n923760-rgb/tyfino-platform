package dev.tyfino.foundation.ui.screen

import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import dev.tyfino.foundation.xtream.CatalogCategory
import dev.tyfino.foundation.xtream.CatalogFailure
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
class CatalogStatusPlacementTest {
    @get:Rule val compose = createComposeRule()
    private val category = CatalogCategory("fixture-category", "Fixture category", 0)
    private val item = CatalogItem("fixture-item", category.providerId, "Fixture content", 0,
        null, null, null, null)

    @Test fun compactGuidanceIsCenteredBelowCategoriesAndSelectionOpensContentInAllSections() {
        var section by mutableStateOf(CatalogSection.Live)
        var selected by mutableStateOf<CatalogCategory?>(null)
        var played: CatalogItem? = null
        var refreshes = 0
        compose.setContent { TyfinoTheme {
            Box(Modifier.width(328.dp).height(480.dp)) {
                CatalogBrowsePane(
                    categories = CatalogState.Content(listOf(category), 1L, false),
                    selectedCategoryId = selected?.providerId, selectedCategoryName = selected?.name,
                    items = if (selected == null) CatalogState.Empty else CatalogState.Content(listOf(item), 1L, false),
                    section = section, onSelect = { selected = it },
                    onRefreshCategories = { refreshes++ }, onRefreshItems = { refreshes++ },
                    onPlay = { played = it },
                )
            }
        } }
        for (current in CatalogSection.entries) {
            compose.runOnIdle { section = current; selected = null; played = null }
            compose.onNodeWithTag("catalog-guidance-icon").assertIsDisplayed()
            assertCentered("catalog-item-pane", "catalog-status-content")
            val categories = bounds("catalog-categories")
            val panel = bounds("catalog-status-content")
            assertTrue("Guidance belongs below the category controls", panel.top >= categories.bottom)
            compose.runOnIdle { assertEquals("Rendering must not refresh or choose automatically", 0, refreshes) }
            compose.onNodeWithContentDescription(category.name).assertIsDisplayed().performClick()
            compose.onNodeWithTag("catalog-guidance-icon").assertDoesNotExist()
            compose.onNodeWithContentDescription(item.name).assertIsDisplayed().performClick()
            compose.runOnIdle { assertEquals(item, played); assertEquals(category, selected) }
        }
    }

    @Test fun expandedArabicGuidanceStaysBesideCategoriesAndReflowsToCompactWidth() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resources = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("ar"))
        }).resources
        var width by mutableStateOf(900.dp)
        var section by mutableStateOf(CatalogSection.Live)
        compose.setContent {
            // Explicit synthetic viewport: fits the same wide layout on both managed-device profiles.
            CompositionLocalProvider(
                LocalResources provides resources,
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(context.resources.displayMetrics.density * 0.35f, 1.6f),
            ) { TyfinoTheme {
                Box(Modifier.width(width).height(600.dp)) {
                    CatalogBrowsePane(CatalogState.Content(listOf(category), 1L, false), null, null,
                        CatalogState.Empty, section, {}, {}, {}, {})
                }
            } }
        }
        for (current in CatalogSection.entries) {
            compose.runOnIdle { width = 900.dp; section = current }
            compose.onNodeWithTag("catalog-empty").assertIsDisplayed()
                .assertTextEquals(resources.getString(R.string.catalog_choose_category))
            assertCentered("catalog-item-pane", "catalog-status-content")
            val categories = bounds("catalog-categories")
            val panel = bounds("catalog-status-content")
            assertTrue("RTL sidebar and guidance occupy separate horizontal regions", panel.right <= categories.left)
            val pane = bounds("catalog-item-pane")
            assertTrue("Large-font guidance remains within its item pane",
                panel.left >= pane.left && panel.right <= pane.right)
            compose.runOnIdle { width = 328.dp }
            assertCentered("catalog-item-pane", "catalog-status-content")
            assertTrue("Compact layout moves the guide below categories",
                bounds("catalog-status-content").top >= bounds("catalog-categories").bottom)
        }
    }

    @Test fun unavailableCategoriesShowTheirOwnStateAndRetryWithoutASelectionHint() {
        var state by mutableStateOf<CatalogState<CatalogCategory>>(CatalogState.Loading)
        var selected by mutableStateOf<CatalogCategory?>(null)
        var retries = 0
        compose.setContent { TyfinoTheme {
            Box(Modifier.width(328.dp).height(480.dp)) {
                CatalogBrowsePane(state, selected?.providerId, selected?.name,
                    if (selected == null) CatalogState.Empty else CatalogState.Content(listOf(item), 1L, false),
                    CatalogSection.Movies, { selected = it }, { retries++ }, {}, {})
            }
        } }
        compose.onNodeWithTag("catalog-loading").assertIsDisplayed()
        compose.onNodeWithTag("catalog-guidance-icon").assertDoesNotExist()
        compose.onNodeWithTag("catalog-categories").assertDoesNotExist()
        compose.runOnIdle { state = CatalogState.EmptyContent(1L, false) }
        compose.onNodeWithTag("catalog-empty").assertIsDisplayed()
            .assertTextEquals(InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.catalog_no_categories))
        compose.onNodeWithTag("catalog-status-retry").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, retries); state = CatalogState.Error(CatalogFailure.NetworkUnavailable) }
        compose.onNodeWithTag("catalog-empty").assertDoesNotExist()
        compose.onNodeWithTag("catalog-error").assertIsDisplayed()
        compose.onNodeWithTag("catalog-status-retry").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(2, retries); state = CatalogState.EmptyContent(1L, true) }
        compose.onNodeWithTag("catalog-loading").assertIsDisplayed()
        compose.onNodeWithTag("catalog-status-retry").assertDoesNotExist()
        compose.runOnIdle { state = CatalogState.StaleContent(listOf(category), 1L, CatalogFailure.Timeout) }
        compose.onNodeWithTag("catalog-guidance-icon").assertIsDisplayed()
        compose.onNodeWithContentDescription(category.name).assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription(item.name).assertIsDisplayed()
        compose.runOnIdle { state = CatalogState.Error(CatalogFailure.Timeout) }
        compose.onNodeWithTag("catalog-error").assertIsDisplayed()
        compose.onNodeWithContentDescription(item.name).assertIsDisplayed()
        compose.runOnIdle { state = CatalogState.EmptyContent(1L, false) }
        compose.onNodeWithContentDescription(item.name).assertIsDisplayed()
    }

    @Test fun shortArabicItemStatesKeepRetryReachableByScrollAndKeyboard() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val resources = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("ar"))
        }).resources
        instrumentation.setInTouchMode(false)
        try {
            var state by mutableStateOf<CatalogState<CatalogItem>>(CatalogState.Loading)
            var host: View? = null
            var input: InputModeManager? = null
            var retries = 0
            compose.setContent {
                host = LocalView.current
                input = LocalInputModeManager.current
                CompositionLocalProvider(
                    LocalResources provides resources, LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalDensity provides Density(context.resources.displayMetrics.density, 1.6f),
                ) { TyfinoTheme {
                    Box(Modifier.width(280.dp).height(180.dp)) {
                        CatalogItemContent(state, CatalogSection.Series, true, {}, { retries++ }, Modifier)
                    }
                } }
            }
            compose.onNodeWithTag("catalog-loading").assertIsDisplayed()
            compose.onNodeWithTag("catalog-guidance-icon").assertDoesNotExist()
            compose.runOnIdle { state = CatalogState.EmptyContent(1L, false) }
            compose.onNodeWithTag("catalog-status-retry").performScrollTo().assertIsDisplayed().performClick()
            compose.runOnIdle { assertEquals(1, retries); state = CatalogState.Error(CatalogFailure.ResponseTooLarge) }
            compose.onNodeWithTag("catalog-error").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("catalog-status-retry").performScrollTo().assertIsDisplayed()
            compose.waitUntil(5_000) { compose.runOnIdle { host?.hasWindowFocus() == true } }
            compose.runOnIdle { assertTrue(input!!.requestInputMode(InputMode.Keyboard)) }
            compose.waitUntil(5_000) { compose.runOnIdle { input?.inputMode == InputMode.Keyboard } }
            compose.onNodeWithTag("catalog-status-retry")
                .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
                .assertIsFocused().performKeyInput { pressKey(Key.Enter) }
            compose.runOnIdle { assertEquals(2, retries) }
        } finally {
            instrumentation.setInTouchMode(true)
        }
    }

    @Test fun refreshAndStaleNoticesReserveSpaceAbovePlayableItems() {
        var state by mutableStateOf<CatalogState<CatalogItem>>(CatalogState.Content(listOf(item), 1L, true))
        var plays = 0
        compose.setContent { TyfinoTheme {
            Box(Modifier.width(328.dp).height(480.dp)) {
                CatalogItemContent(state, CatalogSection.Live, true, { plays++ }, {}, Modifier)
            }
        } }
        compose.onNodeWithTag("catalog-refreshing").assertIsDisplayed()
        assertAbove("catalog-refreshing", item.name)
        compose.onNodeWithContentDescription(item.name).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, plays); state = CatalogState.StaleContent(listOf(item), 1L, CatalogFailure.Timeout) }
        compose.onNodeWithTag("catalog-refreshing").assertDoesNotExist()
        compose.onNodeWithTag("catalog-stale").assertIsDisplayed()
        assertAbove("catalog-stale", item.name)
        compose.onNodeWithContentDescription(item.name).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(2, plays); state = CatalogState.Content(listOf(item), 2L, false) }
        compose.onNodeWithTag("catalog-stale").assertDoesNotExist()
        compose.onNodeWithContentDescription(item.name).assertIsDisplayed()
    }

    @Test fun limitedSearchNoticeDoesNotOverlayTheFirstResult() {
        var limited by mutableStateOf(true)
        var played: CatalogItem? = null
        compose.setContent { TyfinoTheme {
            Box(Modifier.width(328.dp).height(480.dp)) {
                CatalogSearchGrid(listOf(item), CatalogSection.Movies, { played = it }, limited)
            }
        } }
        compose.onNodeWithTag("catalog-search-limit").assertIsDisplayed()
        assertAbove("catalog-search-limit", item.name)
        compose.onNodeWithContentDescription(item.name).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(item, played); limited = false }
        compose.onNodeWithTag("catalog-search-limit").assertDoesNotExist()
        compose.onNodeWithContentDescription(item.name).assertIsDisplayed()
    }

    private fun bounds(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun assertCentered(pane: String, panel: String) {
        val area = bounds(pane)
        val content = bounds(panel)
        assertTrue("The tested content pane must have positive height", area.height > 0f)
        assertEquals("Horizontal centering follows the remaining pane", area.center.x, content.center.x, 1f)
        assertEquals("Vertical centering follows the remaining pane", area.center.y, content.center.y, 1f)
    }

    private fun assertAbove(notice: String, itemName: String) {
        val message = bounds(notice)
        val tile = compose.onNodeWithContentDescription(itemName).fetchSemanticsNode().boundsInRoot
        assertTrue("Status text must reserve space instead of overlaying the first row", message.bottom <= tile.top)
    }
}
