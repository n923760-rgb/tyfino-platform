package dev.tyfino.foundation.app

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Before fun useDpadInputMode() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(false)
    }

    @After fun restoreTouchInputMode() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
    }

    @Test fun bottomMenuKeepsEveryDestinationAndExclusiveSelection() {
        var selected by mutableStateOf(AppDestination.Home.route)
        compose.setContent {
            TyfinoTheme {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
                    AppBottomBar(selected, { selected = it.route })
                }
            }
        }
        for (destination in AppDestination.entries) {
            compose.onNodeWithTag("destination-${destination.route}")
                .assertIsDisplayed().assertHasClickAction().performClick().assertIsSelected()
            for (other in AppDestination.entries.filter { it != destination }) {
                compose.onNodeWithTag("destination-${other.route}").assertIsNotSelected()
            }
            compose.runOnIdle { assertEquals(destination.route, selected) }
        }
    }

    @Test fun expandedMenuSupportsArabicRtlAndLargerText() {
        var selected by mutableStateOf(AppDestination.Home.route)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("ar"))
        }
        val arabicContext = context.createConfigurationContext(configuration)
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(
                LocalContext provides arabicContext,
                LocalConfiguration provides configuration,
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(density, 1.35f),
            ) {
                TyfinoTheme { AppNavigationRail(selected, { selected = it.route }, expanded = true) }
            }
        }
        compose.onNodeWithTag("app-expanded-menu").assertIsDisplayed()
        compose.onNodeWithTag("destination-movies").assertTextContains("الأفلام")
            .assertHasClickAction().performClick().assertIsSelected()
        compose.onNodeWithTag("destination-home").assertIsNotSelected()
        compose.onNodeWithTag("app-side-menu").performScrollToNode(hasTestTag("destination-settings"))
        compose.onNodeWithTag("destination-settings").assertIsDisplayed()
            .performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(AppDestination.Settings.route, selected) }
    }

    @Test fun compactMenuKeepsSettingsReachableInShortPane() {
        var opened: AppDestination? = null
        compose.setContent {
            TyfinoTheme {
                Box(Modifier.height(260.dp)) {
                    AppNavigationRail(AppDestination.Home.route, { opened = it }, expanded = false)
                }
            }
        }
        compose.onNodeWithTag("app-compact-menu").assertIsDisplayed()
        compose.onNodeWithTag("app-side-menu").performScrollToNode(hasTestTag("destination-settings"))
        compose.onNodeWithTag("destination-settings").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(AppDestination.Settings, opened) }
    }

    @Test fun sideMenuSupportsDirectionalFocusAndKeyboardActivation() {
        var opened: AppDestination? = null
        compose.setContent {
            TyfinoTheme { AppNavigationRail(AppDestination.Home.route, { opened = it }, expanded = true) }
        }
        compose.onNodeWithTag("destination-home")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("destination-live").assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        compose.runOnIdle { assertEquals(AppDestination.Live, opened) }
    }
}
