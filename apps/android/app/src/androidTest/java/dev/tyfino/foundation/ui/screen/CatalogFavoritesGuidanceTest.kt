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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import java.text.NumberFormat
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogFavoritesGuidanceTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @Before fun keyboardInput() { instrumentation.setInTouchMode(false) }
    @After fun restoreInput() { instrumentation.setInTouchMode(true) }

    @Test fun countChangesDoNotChangeSelectionOrInvokeFilterActions() {
        var count by mutableStateOf<Int?>(null)
        var favorites = 0
        var otherActions = 0
        val resources = context.resources
        val locale = resources.configuration.locales[0]
        compose.setContent { TyfinoTheme {
            CatalogFilterTabs(true, false, { otherActions++ }, { favorites++ }, { otherActions++ }, count)
        } }
        compose.onNodeWithTag("catalog-favorites-filter").assertIsSelected()
            .assertTextEquals(resources.getString(R.string.catalog_favorites_only))
        for (next in listOf(0, 12, 200)) {
            compose.runOnIdle { count = next }
            compose.onNodeWithTag("catalog-favorites-filter").assertIsSelected()
                .assertTextEquals(resources.getString(R.string.catalog_favorites_count,
                    NumberFormat.getIntegerInstance(locale).format(next)))
        }
        compose.runOnIdle { count = null; assertEquals(0, favorites + otherActions) }
        compose.onNodeWithTag("catalog-favorites-filter")
            .assertTextEquals(resources.getString(R.string.catalog_favorites_only)).performClick()
        compose.runOnIdle { assertEquals(1, favorites); assertEquals(0, otherActions) }
    }

    @Test fun shortEmptyPanelRequiresExplicitEnterToBrowse() {
        var browsed = 0
        var host: View? = null
        var input: InputModeManager? = null
        compose.setContent {
            host = LocalView.current
            input = LocalInputModeManager.current
            TyfinoTheme { Box(Modifier.width(280.dp).height(200.dp)) {
                CatalogFavoritesEmptyState { browsed++ }
            } }
        }
        val browse = compose.onNodeWithTag("catalog-favorites-browse")
        browse.performScrollTo().assertIsDisplayed()
        compose.waitUntil(5_000) { compose.runOnIdle { host?.hasWindowFocus() == true } }
        compose.runOnIdle { assertTrue(input!!.requestInputMode(InputMode.Keyboard)) }
        compose.waitUntil(5_000) { compose.runOnIdle { input?.inputMode == InputMode.Keyboard } }
        browse.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        browse.assertIsFocused()
        assertScroll()
        compose.runOnIdle { assertEquals(0, browsed) }
        browse.performKeyInput { pressKey(Key.Enter) }
        compose.runOnIdle { assertEquals(1, browsed) }
    }

    @Test fun actualArabicFontTwoPanelAndCountUseLocalizedResources() {
        val resources = context.createConfigurationContext(Configuration(context.resources.configuration)
            .apply { setLocale(Locale.forLanguageTag("ar")) }).resources
        val title = resources.getString(R.string.catalog_favorites_only)
        var browsed = 0
        var showGuide by mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources,
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                TyfinoTheme { Box(Modifier.width(280.dp).height(240.dp)) {
                    if (showGuide) CatalogFavoritesEmptyState { browsed++ }
                    else CatalogFilterTabs(true, false, {}, {}, {}, favoriteCount = 12)
                } }
            }
        }
        compose.onNodeWithTag("catalog-favorites-filter").assertIsSelected()
            .assertTextEquals(resources.getString(R.string.catalog_favorites_count,
                NumberFormat.getIntegerInstance(resources.configuration.locales[0]).format(12)))
        compose.runOnIdle { showGuide = true }
        compose.onNodeWithText(title, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                val layouts = mutableListOf<TextLayoutResult>()
                assertTrue(action(layouts))
                assertEquals(2f, layouts.single().layoutInput.density.fontScale, 0f)
            }
        compose.onNodeWithTag("catalog-favorites-browse").performScrollTo().assertIsDisplayed()
        assertScroll()
        compose.onNodeWithTag("catalog-favorites-browse").performClick()
        compose.runOnIdle { assertEquals(1, browsed) }
    }

    private fun assertScroll() {
        val range = compose.onNodeWithTag("catalog-status-content").fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue(range.maxValue() > 0f)
        assertTrue(range.value() > 0f)
    }
}
