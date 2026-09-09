package dev.tyfino.foundation.playback

import dev.tyfino.foundation.xtream.CatalogSection
import dev.tyfino.foundation.xtream.ProviderEndpoint
import dev.tyfino.foundation.xtream.SavedXtreamAccount
import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackReferenceTest {
    @Test
    fun liveReferenceEncodesEverySecretAndProviderPathSegment() {
        val account = account(
            username = "user/name",
            password = "p a?",
            baseUrl = "https://provider.example:8443/panel",
        )

        val result = build(account, CatalogSection.Live, "abc/1", null) as PlaybackReferenceResult.Ready

        assertEquals(
            "/panel/live/user%2Fname/p%20a%3F/abc%2F1.ts",
            result.reference.uri.rawPath,
        )
        assertEquals("[redacted playback reference]", result.reference.toString())
    }

    @Test
    fun movieRequiresSafeProviderExtensionAndSeriesIsNotPlayable() {
        val account = account()

        assertFailure(
            PlaybackReferenceFailure.InvalidMetadata,
            build(account, CatalogSection.Movies, "42", null),
        )
        assertFailure(
            PlaybackReferenceFailure.InvalidMetadata,
            build(account, CatalogSection.Movies, "42", ".mp4"),
        )
        assertFailure(
            PlaybackReferenceFailure.InvalidMetadata,
            build(account, CatalogSection.Movies, "42", "mp4/../ts"),
        )
        assertTrue(build(account, CatalogSection.Movies, "42", "MKV") is PlaybackReferenceResult.Ready)
        assertFailure(
            PlaybackReferenceFailure.InvalidMetadata,
            build(account, CatalogSection.Series, "42", "mkv"),
        )
    }

    @Test
    fun selectionMustStillBelongToExactAccountGeneration() {
        val account = account(id = "account-a", generation = 4)
        val wrongAccount = selection("account-b", 4, CatalogSection.Live, "1", "ts")
        val oldGeneration = selection("account-a", 3, CatalogSection.Live, "1", "ts")

        assertFailure(
            PlaybackReferenceFailure.AccountChanged,
            XtreamPlaybackReferenceBuilder.build(account, wrongAccount),
        )
        assertFailure(
            PlaybackReferenceFailure.AccountChanged,
            XtreamPlaybackReferenceBuilder.build(account, oldGeneration),
        )
    }

    @Test
    fun cleartextReferenceRequiresStoredAccountConsent() {
        val unapproved = account(baseUrl = "http://provider.example", cleartextConsent = false)
        val approved = account(baseUrl = "http://provider.example", cleartextConsent = true)

        assertFailure(
            PlaybackReferenceFailure.CleartextNotApproved,
            build(unapproved, CatalogSection.Live, "1", "ts"),
        )
        assertTrue(build(approved, CatalogSection.Live, "1", "ts") is PlaybackReferenceResult.Ready)
    }

    @Test
    fun redirectPolicyAcceptsOnlyBoundedSafeTargets() {
        val current = URI("https://provider.example/live/secret")
        val accepted = PlaybackRedirectPolicy.evaluate(
            current,
            "https://cdn.example/video.m3u8?signed=value",
            redirectsFollowed = 0,
            cleartextConsent = false,
        ) as PlaybackRedirectResult.Accepted

        assertEquals("cdn.example", accepted.target.host)
        assertEquals("[accepted playback redirect]", accepted.toString())
        assertRedirectFailure(
            PlaybackRedirectFailure.HttpsDowngrade,
            current,
            "http://cdn.example/video.ts",
            redirects = 0,
            consent = true,
        )
        assertRedirectFailure(
            PlaybackRedirectFailure.InvalidTarget,
            current,
            "https://user:password@cdn.example/video.ts",
            redirects = 0,
            consent = false,
        )
        assertRedirectFailure(
            PlaybackRedirectFailure.InvalidTarget,
            current,
            "/relative/video.ts",
            redirects = 0,
            consent = false,
        )
        assertRedirectFailure(
            PlaybackRedirectFailure.TooManyRedirects,
            current,
            "https://cdn.example/video.ts",
            redirects = PlaybackRedirectPolicy.MAX_REDIRECTS,
            consent = false,
        )
    }

    @Test
    fun operationGateRejectsOlderSelectionAccountChangesAndDestinationExit() {
        val gate = PlaybackOperationGate()
        gate.activateDestination()
        val firstSelection = selection("account-a", 4, CatalogSection.Live, "1", "ts")
        val secondSelection = selection("account-a", 4, CatalogSection.Live, "2", "ts")
        val first = gate.begin(firstSelection)
        val second = gate.begin(secondSelection)

        assertFalse(gate.isCurrent(first, account()))
        assertTrue(gate.isCurrent(second, account()))
        assertFalse(gate.isCurrent(second, account(generation = 5)))

        var published = false
        assertTrue(gate.commit(second, account()) { published = true })
        assertTrue(published)
        gate.deactivateDestination()
        assertFalse(gate.isCurrent(second, account()))
    }

    private fun build(
        account: SavedXtreamAccount,
        section: CatalogSection,
        providerId: String,
        extension: String?,
    ) = XtreamPlaybackReferenceBuilder.build(
        account,
        selection(account.accountId, account.generation, section, providerId, extension),
    )

    private fun selection(
        accountId: String,
        generation: Long,
        section: CatalogSection,
        providerId: String,
        extension: String?,
    ) = PlaybackSelection(accountId, generation, section, providerId, extension)

    private fun account(
        id: String = "account-a",
        generation: Long = 4,
        username: String = "user",
        password: String = "password",
        baseUrl: String = "https://provider.example",
        cleartextConsent: Boolean = false,
    ) = SavedXtreamAccount(
        accountId = id,
        generation = generation,
        endpoint = ProviderEndpoint(baseUrl, baseUrl.startsWith("http://")),
        username = username,
        password = password,
        cleartextConsent = cleartextConsent,
    )

    private fun assertFailure(expected: PlaybackReferenceFailure, actual: PlaybackReferenceResult) {
        assertEquals(PlaybackReferenceResult.Failure(expected), actual)
    }

    private fun assertRedirectFailure(
        expected: PlaybackRedirectFailure,
        current: URI,
        target: String,
        redirects: Int,
        consent: Boolean,
    ) {
        assertEquals(
            PlaybackRedirectResult.Rejected(expected),
            PlaybackRedirectPolicy.evaluate(current, target, redirects, consent),
        )
    }
}
