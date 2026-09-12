package dev.tyfino.foundation.playback

import dev.tyfino.foundation.xtream.CatalogItem
import dev.tyfino.foundation.xtream.CatalogSection
import dev.tyfino.foundation.xtream.SavedXtreamAccount
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

internal data class PlaybackSelection(
    val accountId: String,
    val accountGeneration: Long,
    val section: CatalogSection,
    val providerItemId: String,
    val containerExtension: String?,
) {
    companion object {
        fun from(account: SavedXtreamAccount, section: CatalogSection, item: CatalogItem) =
            PlaybackSelection(
                accountId = account.accountId,
                accountGeneration = account.generation,
                section = section,
                providerItemId = item.providerId,
                containerExtension = item.containerExtension,
            )
    }
}

internal enum class PlaybackReferenceFailure {
    AccountChanged,
    InvalidMetadata,
    CleartextNotApproved,
}

internal sealed interface PlaybackReferenceResult {
    class Ready internal constructor(val reference: SecretPlaybackReference) : PlaybackReferenceResult
    data class Failure(val reason: PlaybackReferenceFailure) : PlaybackReferenceResult
}

internal class SecretPlaybackReference internal constructor(internal val uri: URI) {
    override fun toString(): String = REDACTED

    private companion object {
        const val REDACTED = "[redacted playback reference]"
    }
}

internal object XtreamPlaybackReferenceBuilder {
    fun build(
        account: SavedXtreamAccount,
        selection: PlaybackSelection,
    ): PlaybackReferenceResult {
        if (
            selection.accountId != account.accountId ||
            selection.accountGeneration != account.generation
        ) {
            return PlaybackReferenceResult.Failure(PlaybackReferenceFailure.AccountChanged)
        }
        if (account.endpoint.isCleartext && !account.cleartextConsent) {
            return PlaybackReferenceResult.Failure(PlaybackReferenceFailure.CleartextNotApproved)
        }
        val providerId = boundedProviderId(selection.providerItemId)
            ?: return PlaybackReferenceResult.Failure(PlaybackReferenceFailure.InvalidMetadata)
        val extension = extension(selection.section, selection.containerExtension)
            ?: return PlaybackReferenceResult.Failure(PlaybackReferenceFailure.InvalidMetadata)
        val cluster = when (selection.section) {
            CatalogSection.Live -> "live"
            CatalogSection.Movies -> "movie"
            CatalogSection.Series -> {
                return PlaybackReferenceResult.Failure(PlaybackReferenceFailure.InvalidMetadata)
            }
        }
        return reference(account, cluster, providerId, extension)
    }

    fun buildEpisode(
        account: SavedXtreamAccount,
        selection: EpisodePlaybackSelection,
    ): PlaybackReferenceResult {
        if (selection.accountId != account.accountId ||
            selection.accountGeneration != account.generation
        ) return PlaybackReferenceResult.Failure(PlaybackReferenceFailure.AccountChanged)
        if (account.endpoint.isCleartext && !account.cleartextConsent) {
            return PlaybackReferenceResult.Failure(PlaybackReferenceFailure.CleartextNotApproved)
        }
        val episodeId = boundedProviderId(selection.providerEpisodeId)
            ?: return PlaybackReferenceResult.Failure(PlaybackReferenceFailure.InvalidMetadata)
        if (boundedProviderId(selection.providerSeriesId) == null ||
            selection.seriesGeneration <= 0L || selection.playbackDestinationEpoch <= 0L ||
            selection.operationId.isBlank()
        ) return PlaybackReferenceResult.Failure(PlaybackReferenceFailure.InvalidMetadata)
        val extension = selection.containerExtension.takeIf(SAFE_EXTENSION::matches)
            ?: return PlaybackReferenceResult.Failure(PlaybackReferenceFailure.InvalidMetadata)
        return reference(account, "series", episodeId, extension)
    }

    private fun reference(
        account: SavedXtreamAccount,
        cluster: String,
        providerId: String,
        extension: String,
    ): PlaybackReferenceResult {
        val value = buildString {
            append(account.endpoint.baseUrl.trimEnd('/'))
            append('/')
            append(cluster)
            append('/')
            append(segment(account.username))
            append('/')
            append(segment(account.password))
            append('/')
            append(segment(providerId))
            append('.')
            append(extension)
        }
        val uri = runCatching { URI(value) }.getOrNull()
            ?: return PlaybackReferenceResult.Failure(PlaybackReferenceFailure.InvalidMetadata)
        if (!retainsEndpoint(account, uri)) {
            return PlaybackReferenceResult.Failure(PlaybackReferenceFailure.InvalidMetadata)
        }
        return PlaybackReferenceResult.Ready(SecretPlaybackReference(uri))
    }

    private fun extension(section: CatalogSection, candidate: String?): String? {
        val normalized = candidate?.lowercase(Locale.ROOT)
        val value = when (section) {
            CatalogSection.Live -> normalized ?: DEFAULT_LIVE_EXTENSION
            CatalogSection.Movies -> normalized ?: return null
            CatalogSection.Series -> return null
        }
        return value.takeIf(SAFE_EXTENSION::matches)
    }

    private fun boundedProviderId(value: String): String? = value
        .takeIf(String::isNotBlank)
        ?.takeIf { it.codePointCount(0, it.length) <= PROVIDER_ID_LIMIT }

    private fun segment(value: String): String = URLEncoder
        .encode(value, StandardCharsets.UTF_8.toString())
        .replace("+", "%20")

    private fun retainsEndpoint(account: SavedXtreamAccount, candidate: URI): Boolean {
        val base = URI(account.endpoint.baseUrl)
        return candidate.scheme.equals(base.scheme, ignoreCase = true) &&
            candidate.host.equals(base.host, ignoreCase = true) &&
            candidate.port == base.port &&
            candidate.rawUserInfo == null &&
            candidate.rawQuery == null &&
            candidate.rawFragment == null
    }

    private const val DEFAULT_LIVE_EXTENSION = "ts"
    private const val PROVIDER_ID_LIMIT = 256
    private val SAFE_EXTENSION = Regex("[a-z0-9]{1,12}")
}

internal enum class PlaybackRedirectFailure {
    TooManyRedirects,
    InvalidTarget,
    CleartextNotApproved,
    HttpsDowngrade,
}

internal sealed interface PlaybackRedirectResult {
    class Accepted internal constructor(internal val target: URI) : PlaybackRedirectResult {
        override fun toString(): String = "[accepted playback redirect]"
    }
    data class Rejected(val reason: PlaybackRedirectFailure) : PlaybackRedirectResult
}

internal object PlaybackRedirectPolicy {
    const val MAX_REDIRECTS = 3

    fun evaluate(
        current: URI,
        targetValue: String,
        redirectsFollowed: Int,
        cleartextConsent: Boolean,
    ): PlaybackRedirectResult {
        if (redirectsFollowed >= MAX_REDIRECTS) {
            return PlaybackRedirectResult.Rejected(PlaybackRedirectFailure.TooManyRedirects)
        }
        val target = runCatching { URI(targetValue) }.getOrNull()
            ?: return PlaybackRedirectResult.Rejected(PlaybackRedirectFailure.InvalidTarget)
        if (!target.isAbsolute || target.host.isNullOrBlank() || target.rawUserInfo != null || target.rawFragment != null) {
            return PlaybackRedirectResult.Rejected(PlaybackRedirectFailure.InvalidTarget)
        }
        val currentScheme = current.scheme?.lowercase(Locale.ROOT)
        val targetScheme = target.scheme?.lowercase(Locale.ROOT)
        if (targetScheme != "https" && targetScheme != "http") {
            return PlaybackRedirectResult.Rejected(PlaybackRedirectFailure.InvalidTarget)
        }
        if (currentScheme == "https" && targetScheme == "http") {
            return PlaybackRedirectResult.Rejected(PlaybackRedirectFailure.HttpsDowngrade)
        }
        if (targetScheme == "http" && !cleartextConsent) {
            return PlaybackRedirectResult.Rejected(PlaybackRedirectFailure.CleartextNotApproved)
        }
        return PlaybackRedirectResult.Accepted(target)
    }
}
