package dev.tyfino.foundation.ui.screen

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
import java.util.Date

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
}
