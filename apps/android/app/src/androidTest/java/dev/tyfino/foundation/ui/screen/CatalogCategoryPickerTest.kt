package dev.tyfino.foundation.ui.screen

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Root
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import dev.tyfino.foundation.xtream.CatalogCategory
import dev.tyfino.foundation.xtream.CatalogSection
import dev.tyfino.foundation.xtream.CatalogState
import java.util.Locale
import kotlin.math.roundToInt
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogCategoryPickerTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val categories = (0 until 40).map { CatalogCategory("fixture-$it", "Category $it", it) }

    @Before fun directionalInput() { instrumentation.setInTouchMode(false) }
    @After fun restoreInput() { instrumentation.setInTouchMode(true) }

    @Test fun actualBrowsePaneOpensClosesAndSelectsWithoutRefreshOrPlaybackInEverySection() {
        var section by mutableStateOf(CatalogSection.Live)
        var chosen by mutableStateOf<CatalogCategory?>(null)
        val selections = mutableListOf<CatalogCategory>()
        var otherActions = 0
        compose.setContent { TyfinoTheme { Box(Modifier.width(328.dp).height(480.dp)) {
            CatalogBrowsePane(CatalogState.Content(categories.take(3), 1L, false),
                chosen?.providerId, chosen?.name, CatalogState.Empty, section,
                { chosen = it; selections += it }, { otherActions++ }, { otherActions++ }, { otherActions++ })
        } } }
        for (current in CatalogSection.entries) {
            compose.runOnIdle { section = current; chosen = null }
            compose.onNodeWithTag("catalog-category-picker-open").performClick()
            compose.onNodeWithTag("catalog-category-picker-close").assertIsDisplayed().performClick()
            compose.onNodeWithTag("catalog-category-picker").assertDoesNotExist()
            compose.runOnIdle { assertEquals(null, chosen); assertEquals(0, otherActions) }
            compose.onNodeWithTag("catalog-category-picker-open").performClick()
            compose.onNodeWithTag("catalog-category-picker-option-1").performClick()
            compose.onNodeWithTag("catalog-category-picker").assertDoesNotExist()
            compose.runOnIdle { assertEquals(categories[1], chosen); assertEquals(0, otherActions) }
        }
        compose.runOnIdle { assertEquals(List(3) { categories[1] }, selections) }
    }

    @Test fun shortNativeDialogStartsAtCurrentCategoryAndDpadSelectsOnlyOnEnter() {
        var chosen: CatalogCategory? = null
        compose.setContent {
            var visible by remember { mutableStateOf(true) }
            TyfinoTheme { if (visible) CatalogCategoryPickerDialog(categories, categories[30].providerId,
                { chosen = it; visible = false }, { visible = false }) }
        }
        constrainWindow()
        compose.onNodeWithTag("catalog-category-picker-close").assertIsDisplayed().assertIsFocused()
        compose.onNodeWithTag("catalog-category-picker-option-30").assertIsSelected().assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            .performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("catalog-category-picker-option-31").assertIsFocused().assertIsDisplayed()
        compose.runOnIdle { assertEquals(null, chosen) }
        compose.onNodeWithTag("catalog-category-picker-option-31").performKeyInput { pressKey(Key.Enter) }
        compose.onNodeWithTag("catalog-category-picker").assertDoesNotExist()
        compose.runOnIdle { assertEquals(categories[31], chosen) }
    }

    @Test fun actualArabicFontTwoRendererKeepsCloseAndLazySelectionReachable() {
        val context = instrumentation.targetContext
        val resources = context.createConfigurationContext(Configuration(context.resources.configuration)
            .apply { setLocale(Locale.forLanguageTag("ar")) }).resources
        val title = resources.getString(R.string.catalog_category_picker)
        val records = categories.take(10).map { it.copy(name = "قسم الأفلام والمسلسلات ${it.providerOrder}") }
        var chosen: CatalogCategory? = null
        var closed = 0
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources,
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                TyfinoTheme { Box(Modifier.width(280.dp).height(360.dp)) {
                    CatalogCategoryPickerContent(records, records[2].providerId,
                        remember { FocusRequester() }, { chosen = it }, { closed++ })
                } }
            }
        }
        compose.onNodeWithText(title, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                val layouts = mutableListOf<TextLayoutResult>()
                assertTrue(action(layouts))
                assertEquals(2f, layouts.single().layoutInput.density.fontScale, 0f)
            }
        compose.onNodeWithTag("catalog-category-picker-close").assertIsDisplayed()
        compose.onNodeWithTag("catalog-category-picker-list").performScrollToIndex(5)
        compose.onNodeWithTag("catalog-category-picker-option-5").assertIsDisplayed().performClick()
        val range = compose.onNodeWithTag("catalog-category-picker-list").fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue(range.maxValue() > 0f)
        assertTrue(range.value() > 0f)
        compose.onNodeWithTag("catalog-category-picker-close").performClick()
        compose.runOnIdle { assertEquals(records[5], chosen); assertEquals(1, closed) }
    }

    @Test fun nativeSearchFiltersWithoutSelectionAndClearRestoresCurrentCategory() {
        var chosen: CatalogCategory? = null
        compose.setContent { TyfinoTheme {
            CatalogCategoryPickerDialog(categories, categories[30].providerId, { chosen = it }, {})
        } }
        constrainWindow()
        compose.onNodeWithTag("catalog-category-picker-search-toggle").performClick()
        compose.onNodeWithTag("catalog-category-picker-query").performTextReplacement("Category 39")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasTestTag("catalog-category-picker-option-0") and hasText("Category 39"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("catalog-category-picker-option-0").assertIsDisplayed()
        compose.runOnIdle { assertEquals(null, chosen) }
        compose.onNodeWithTag("catalog-category-picker-search-toggle").performClick()
        compose.onNodeWithTag("catalog-category-picker-query").assertDoesNotExist()
        compose.onNodeWithTag("catalog-category-picker-option-30").assertIsSelected().assertIsDisplayed()
        compose.onNodeWithTag("catalog-category-picker-search-toggle").performClick()
        compose.onNodeWithTag("catalog-category-picker-query").performTextReplacement("Category 39")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasTestTag("catalog-category-picker-option-0") and hasText("Category 39"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("catalog-category-picker-option-0").performClick()
        compose.runOnIdle { assertEquals(categories[39], chosen) }
    }

    @Test fun searchSnapshotReplacementNeverOffersOldCategoryAndNoMatchKeepsClose() {
        var records by mutableStateOf(categories)
        var chosen: CatalogCategory? = null
        var closed = 0
        compose.setContent { TyfinoTheme { Box(Modifier.width(280.dp).height(320.dp)) {
            CatalogCategoryPickerContent(records, null, remember { FocusRequester() },
                { chosen = it }, { closed++ })
        } } }
        compose.onNodeWithTag("catalog-category-picker-search-toggle").performClick()
        compose.onNodeWithTag("catalog-category-picker-query").performTextReplacement("Category 39")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasTestTag("catalog-category-picker-option-0") and hasText("Category 39"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        val replacement = CatalogCategory("new-owner", "Category 39 replacement", 0)
        compose.runOnIdle { records = listOf(replacement) }
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasTestTag("catalog-category-picker-option-0") and hasText(replacement.name))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasTestTag("catalog-category-picker-option-0") and hasText("Category 39")).assertDoesNotExist()
        compose.onNodeWithTag("catalog-category-picker-option-0").performClick()
        compose.runOnIdle { assertEquals(replacement, chosen) }
        compose.onNodeWithTag("catalog-category-picker-query").performTextReplacement("not present")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText(instrumentation.targetContext.getString(R.string.catalog_category_search_empty)))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("catalog-category-picker-close").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, closed); assertEquals(replacement, chosen) }
    }

    @Test fun filteredNativeDialogUsesRealDpadEnterWithoutImplicitSelection() {
        var chosen: CatalogCategory? = null
        compose.setContent {
            var visible by remember { mutableStateOf(true) }
            TyfinoTheme { if (visible) CatalogCategoryPickerDialog(categories, null,
                { chosen = it; visible = false }, { visible = false }) }
        }
        constrainWindow()
        compose.onNodeWithTag("catalog-category-picker-search-toggle").performClick()
        compose.onNodeWithTag("catalog-category-picker-query").performTextReplacement("Category 39")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        waitForOption("Category 39")
        compose.onNodeWithTag("catalog-category-picker-option-0")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }.assertIsFocused()
        compose.runOnIdle { assertEquals(null, chosen) }
        compose.onNodeWithTag("catalog-category-picker-option-0").performKeyInput { pressKey(Key.Enter) }
        compose.onNodeWithTag("catalog-category-picker").assertDoesNotExist()
        compose.runOnIdle { assertEquals(categories[39], chosen) }
    }

    @Test fun nativeBackDismissesFilteredDialogWithoutChoosingOrChangingCategory() {
        var chosen: CatalogCategory? = null
        var closed = 0
        compose.setContent {
            var visible by remember { mutableStateOf(true) }
            TyfinoTheme { if (visible) CatalogCategoryPickerDialog(categories, categories[30].providerId,
                { chosen = it }, { closed++; visible = false }) }
        }
        constrainWindow()
        compose.onNodeWithTag("catalog-category-picker-search-toggle").performClick()
        compose.onNodeWithTag("catalog-category-picker-query").performTextReplacement("Category 39")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        waitForOption("Category 39")
        compose.onNodeWithTag("catalog-category-picker-close")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }.assertIsFocused()
        androidx.test.espresso.Espresso.pressBack()
        compose.onNodeWithTag("catalog-category-picker").assertDoesNotExist()
        compose.runOnIdle { assertEquals(null, chosen); assertEquals(1, closed) }
    }

    @Test fun arabicSearchAtFontTwoKeepsSelectionAndNoMatchCloseReachableWithExactStatus() {
        val context = instrumentation.targetContext
        val resources = context.createConfigurationContext(Configuration(context.resources.configuration)
            .apply { setLocale(Locale.forLanguageTag("ar")) }).resources
        val records = listOf(CatalogCategory("films", "أفلام عربية", 0),
            CatalogCategory("news", "قنوات إخبارية", 1))
        var chosen: CatalogCategory? = null
        var closed = 0
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources,
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                TyfinoTheme { Box(Modifier.width(280.dp).height(320.dp)) {
                    CatalogCategoryPickerContent(records, null, remember { FocusRequester() },
                        { chosen = it }, { closed++ })
                } }
            }
        }
        compose.onNodeWithTag("catalog-category-picker-search-toggle").performClick()
        compose.onNodeWithTag("catalog-category-picker-query").performTextReplacement("أفلام")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        waitForOption(records[0].name)
        compose.onNodeWithTag("catalog-category-picker-query")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,
                resources.getString(R.string.catalog_category_search_count, 1)))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        compose.onNodeWithText(resources.getString(R.string.catalog_category_search), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                val layouts = mutableListOf<TextLayoutResult>()
                assertTrue(action(layouts))
                assertEquals(2f, layouts.single().layoutInput.density.fontScale, 0f)
            }
        compose.onNodeWithTag("catalog-category-picker-option-0").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(records[0], chosen) }
        compose.onNodeWithTag("catalog-category-picker-query").performTextReplacement("غير موجود")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        val empty = resources.getString(R.string.catalog_category_search_empty)
        compose.waitUntil(5_000) { compose.onAllNodes(hasText(empty)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("catalog-category-picker-query")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, empty))
        val pane = compose.onNodeWithTag("catalog-category-picker").fetchSemanticsNode().boundsInRoot
        val close = compose.onNodeWithTag("catalog-category-picker-close").fetchSemanticsNode().boundsInRoot
        assertTrue("Close must be fully inside the short pane", close.top >= pane.top && close.bottom <= pane.bottom + 1f)
        compose.onNodeWithTag("catalog-category-picker-close").assertIsDisplayed().performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, closed); assertEquals(records[0], chosen) }
    }

    private fun waitForOption(name: String) {
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasTestTag("catalog-category-picker-option-0") and hasText(name))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun constrainWindow() {
        val focusedDialog = object : TypeSafeMatcher<Root>() {
            override fun describeTo(description: Description) { description.appendText("focused category picker") }
            override fun matchesSafely(root: Root): Boolean = isDialog().matches(root) && root.decorView.hasWindowFocus()
        }
        onView(isRoot()).inRoot(focusedDialog).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isRoot()
            override fun getDescription(): String = "bound actual picker window to 280x320dp"
            override fun perform(uiController: UiController, view: View) {
                val window = findWindow(view) ?: throw AssertionError("native Dialog required")
                val density = view.resources.displayMetrics.density
                window.setLayout((280 * density).roundToInt(), (320 * density).roundToInt())
                uiController.loopMainThreadUntilIdle()
            }
        })
        compose.waitForIdle()
        val bounds = compose.onNodeWithTag("catalog-category-picker").fetchSemanticsNode().boundsInRoot
        val density = instrumentation.targetContext.resources.displayMetrics.density
        assertTrue(bounds.width <= 280 * density + 1f)
        assertTrue(bounds.height <= 320 * density + 1f)
    }

    private fun findWindow(view: View): Window? {
        if (view is DialogWindowProvider) return view.window
        if (view is ViewGroup) for (i in 0 until view.childCount) findWindow(view.getChildAt(i))?.let { return it }
        return null
    }
}
