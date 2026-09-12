package dev.tyfino.foundation.playback

import dev.tyfino.foundation.xtream.ProviderEndpoint
import dev.tyfino.foundation.xtream.SavedXtreamAccount
import dev.tyfino.foundation.xtream.SeriesEpisode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodePlaybackSelectionTest {
    @Test
    fun episodeReferenceUsesEncodedEpisodeIdAndNeverSeriesCatalogId() {
        val account = account(username = "user/name", password = "p a?", url = "https://provider.example/panel")
        val selected = requireNotNull(EpisodePlaybackSelection.from(
            "account", 7, "series-catalog-id", 3,
            episode(id = "episode/one", extension = "m3u8"),
        )).atDestination(2)

        val result = XtreamPlaybackReferenceBuilder.buildEpisode(account, selected) as PlaybackReferenceResult.Ready

        assertEquals("/panel/series/user%2Fname/p%20a%3F/episode%2Fone.m3u8", result.reference.uri.rawPath)
        assertFalse(result.reference.uri.rawPath.contains("series-catalog-id"))
        assertEquals("[redacted playback reference]", result.reference.toString())
        assertEquals("[episode playback selection]", selected.toString())
    }

    @Test
    fun unsafeOrMissingEpisodeMetadataCannotCreateSelection() {
        assertNull(EpisodePlaybackSelection.from("account", 7, "series", 3, episode(extension = null)))
        assertNull(EpisodePlaybackSelection.from("account", 7, "series", 3, episode(extension = "mp4/path")))
        assertNull(EpisodePlaybackSelection.from("account", 7, "series", 0, episode(extension = "mp4")))
        assertNull(EpisodePlaybackSelection.from("account", 7, "another", 3, episode(extension = "mp4")))
        val first = EpisodePlaybackSelection.from("account", 7, "series", 3, episode(extension = "mp4"))!!
        val second = EpisodePlaybackSelection.from("account", 7, "series", 3, episode(extension = "mp4"))!!
        assertTrue(first.operationId != second.operationId)
    }

    @Test
    fun accountAndDestinationMustMatchAndHttpRequiresConsent() {
        val selection = EpisodePlaybackSelection.from("account", 7, "series", 3, episode(extension = "mp4"))!!
        assertEquals(
            PlaybackReferenceResult.Failure(PlaybackReferenceFailure.InvalidMetadata),
            XtreamPlaybackReferenceBuilder.buildEpisode(account(), selection),
        )
        val scoped = selection.atDestination(1)
        assertEquals(
            PlaybackReferenceResult.Failure(PlaybackReferenceFailure.AccountChanged),
            XtreamPlaybackReferenceBuilder.buildEpisode(account(generation = 8), scoped),
        )
        assertEquals(
            PlaybackReferenceResult.Failure(PlaybackReferenceFailure.CleartextNotApproved),
            XtreamPlaybackReferenceBuilder.buildEpisode(account(url = "http://provider.example"), scoped),
        )
    }

    private fun episode(id: String = "episode", extension: String?) = SeriesEpisode(
        "account", "series", id, 1, 1, "Episode", extension, 0, null, null, null, null,
    )

    private fun account(
        username: String = "user",
        password: String = "password",
        url: String = "https://provider.example",
        generation: Long = 7,
    ) = SavedXtreamAccount(
        "account", generation, ProviderEndpoint(url, url.startsWith("http://")),
        username, password, false,
    )
}
