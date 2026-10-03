package dev.tyfino.foundation.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.R
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import dev.tyfino.foundation.xtream.PlaylistInfo
import dev.tyfino.foundation.xtream.PlaylistState
import dev.tyfino.foundation.xtream.PlaylistStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class PlaylistSummaryTest {
    @get:Rule val compose = createComposeRule()

    @Test fun providerExpiryIsSeparateFromAppLicenseAndMissingExpiryIsNotLifetime() {
        var refreshes = 0
        compose.setContent {
            TyfinoTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    PlaylistSummaryPanel(PlaylistState.Ready("https://provider.example", "fixture-user",
                        PlaylistInfo(PlaylistStatus.Active, null, false, 1, 2))) { refreshes++ }
                }
            }
        }
        compose.onNodeWithTag("playlist-provider").assertTextContains("https://provider.example")
        compose.onNodeWithTag("playlist-username").assertTextContains("fixture-user", substring = true)
        compose.onNodeWithTag("playlist-expiry").assertTextContains("Not provided", substring = true)
        compose.onNodeWithTag("license-summary").assertDoesNotExist()
        compose.onNodeWithTag("playlist-refresh").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, refreshes) }
    }

    @Test fun knownProviderExpiryAndExpiredStatusAreDisplayedWithoutChangingLicensing() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expiry = 1_800_000_000_000L
        compose.setContent { TyfinoTheme {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                PlaylistSummaryPanel(PlaylistState.Ready("https://provider.example", "fixture-user",
                    PlaylistInfo(PlaylistStatus.Expired, expiry, null, null, null))) {}
            }
        } }
        val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT,
            context.resources.configuration.locales[0]).format(Date(expiry))
        compose.onNodeWithTag("playlist-expiry").assertTextEquals(context.getString(R.string.playlist_expiry, date))
        compose.onNodeWithTag("playlist-status").assertTextEquals(context.getString(R.string.playlist_expired))
        compose.onNodeWithTag("license-summary").assertDoesNotExist()
    }

    @Test fun compactArabicLargeFontKeepsSubscriptionFactsAndRefreshReachable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val locale = Locale.forLanguageTag("ar")
        val resources = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocale(locale)
        }).resources
        val expiry = 1_800_000_000_000L
        var refreshes = 0
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources,
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(context.resources.displayMetrics.density, 1.6f)) {
                TyfinoTheme {
                    Column(Modifier.width(280.dp).verticalScroll(rememberScrollState())) {
                        PlaylistSummaryPanel(PlaylistState.Ready("https://long-provider-name.example", "fixture-user",
                            PlaylistInfo(PlaylistStatus.Disabled, expiry, true, 2, 12))) { refreshes++ }
                    }
                }
            }
        }
        val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(expiry))
        val numbers = NumberFormat.getIntegerInstance(locale).apply { isGroupingUsed = false }
        compose.onNodeWithTag("playlist-status").performScrollTo().assertIsDisplayed()
            .assertTextEquals(resources.getString(R.string.playlist_disabled))
        compose.onNodeWithTag("playlist-expiry").performScrollTo().assertIsDisplayed()
            .assertTextEquals(resources.getString(R.string.playlist_expiry, date))
        compose.onNodeWithTag("playlist-connections").performScrollTo().assertIsDisplayed()
            .assertTextEquals(resources.getString(R.string.playlist_connections, numbers.format(2), numbers.format(12)))
        val panel = compose.onNodeWithTag("playlist-summary").fetchSemanticsNode().boundsInRoot
        for (tag in listOf("playlist-status", "playlist-expiry", "playlist-connections")) {
            val bounds = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            assertTrue("$tag stays inside compact panel", bounds.left >= panel.left - 1f && bounds.right <= panel.right + 1f)
        }
        compose.onNodeWithTag("playlist-refresh").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, refreshes) }
        compose.onNodeWithTag("license-summary").assertDoesNotExist()
    }

    @Test fun replacementProviderStateRemovesPriorExpiryAndOptionalConnections() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val state = mutableStateOf<PlaylistState>(PlaylistState.Ready("https://first.example", "first-user",
            PlaylistInfo(PlaylistStatus.Expired, 1_800_000_000_000L, false, 1, 2)))
        compose.setContent { TyfinoTheme {
            Column(Modifier.verticalScroll(rememberScrollState())) { PlaylistSummaryPanel(state.value) {} }
        } }
        compose.onNodeWithTag("playlist-status").assertTextEquals(context.getString(R.string.playlist_expired))
        compose.onNodeWithTag("playlist-connections").assertExists()
        compose.runOnIdle { state.value = PlaylistState.Ready("https://second.example", "second-user",
            PlaylistInfo(PlaylistStatus.Active, null, null, null, null)) }
        compose.onNodeWithTag("playlist-provider").assertTextEquals("https://second.example")
        compose.onNodeWithTag("playlist-username").assertTextEquals(context.getString(R.string.playlist_username, "second-user"))
        compose.onNodeWithTag("playlist-status").assertTextEquals(context.getString(R.string.playlist_active))
        compose.onNodeWithTag("playlist-expiry").assertTextEquals(context.getString(R.string.playlist_expiry,
            context.getString(R.string.playlist_not_provided)))
        compose.onNodeWithTag("playlist-connections").assertDoesNotExist()
        compose.onNodeWithTag("license-summary").assertDoesNotExist()
    }
}
