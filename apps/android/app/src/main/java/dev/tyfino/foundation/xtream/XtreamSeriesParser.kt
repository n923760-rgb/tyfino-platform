package dev.tyfino.foundation.xtream

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.IOException
import java.io.Reader
import java.math.BigDecimal

internal class XtreamSeriesParser(
    private val seasonGroupLimit: Int = SEASON_GROUP_LIMIT,
    private val episodeLimit: Int = EPISODE_LIMIT,
) {
    fun parse(
        reader: Reader,
        accountId: String,
        seriesId: String,
        artworkPolicy: CatalogArtworkPolicy,
    ): SeriesDetailsResult {
        if (boundedProviderId(accountId) == null || boundedProviderId(seriesId) == null) {
            return SeriesDetailsResult.Failure(SeriesFailure.UnsupportedResponse)
        }
        return try {
            JsonReader(reader).use { json ->
                parseDocument(json, accountId, seriesId, artworkPolicy)
            }
        } catch (_: SeriesEntryLimitException) {
            SeriesDetailsResult.Failure(SeriesFailure.ResponseTooLarge)
        } catch (_: CatalogResponseTooLargeException) {
            SeriesDetailsResult.Failure(SeriesFailure.ResponseTooLarge)
        } catch (_: IOException) {
            SeriesDetailsResult.Failure(SeriesFailure.MalformedResponse)
        } catch (_: IllegalStateException) {
            SeriesDetailsResult.Failure(SeriesFailure.MalformedResponse)
        }
    }

    private fun parseDocument(
        json: JsonReader,
        accountId: String,
        seriesId: String,
        artworkPolicy: CatalogArtworkPolicy,
    ): SeriesDetailsResult {
        if (json.peek() != JsonToken.BEGIN_OBJECT) return malformed()
        var summary = SeriesSummary.EMPTY
        val advisories = linkedMapOf<Int, SeasonAdvisory>()
        val episodes = ArrayList<SeriesEpisode>()
        val seenEpisodeIds = HashSet<String>()
        val counters = ParseCounters()
        var foundEpisodes = false

        json.beginObject()
        while (json.hasNext()) {
            when (json.nextName()) {
                "info" -> summary = parseSummaryOrSkip(json, artworkPolicy)
                "seasons" -> parseSeasonsOrSkip(json, artworkPolicy, advisories, counters)
                "episodes" -> {
                    if (foundEpisodes) return malformed()
                    foundEpisodes = true
                    if (!parseEpisodeGroups(
                            json,
                            accountId,
                            seriesId,
                            episodes,
                            seenEpisodeIds,
                            counters,
                        )
                    ) {
                        return malformed()
                    }
                }
                else -> json.skipValue()
            }
        }
        json.endObject()
        if (json.peek() != JsonToken.END_DOCUMENT || !foundEpisodes) return malformed()

        val orderedEpisodes = episodes.sortedWith(
            compareBy<SeriesEpisode> { it.seasonNumber }
                .thenBy { it.episodeNumber ?: Int.MAX_VALUE }
                .thenBy { it.providerOrder },
        )
        val episodeCounts = orderedEpisodes.groupingBy(SeriesEpisode::seasonNumber).eachCount()
        val seasons = episodeCounts.keys.sorted().map { number ->
            val advisory = advisories[number]
            SeriesSeason(
                accountId = accountId,
                providerSeriesId = seriesId,
                seasonNumber = number,
                displayLabel = advisory?.name?.takeIf(String::isNotBlank) ?: number.toString(),
                overview = advisory?.overview,
                airDate = advisory?.airDate,
                coverUrl = advisory?.coverUrl,
                providerOrder = advisory?.providerOrder ?: number,
                episodeCount = episodeCounts.getValue(number),
            )
        }
        return SeriesDetailsResult.Success(
            SeriesDetailsCandidate(
                accountId = accountId,
                providerSeriesId = seriesId,
                summary = summary,
                seasons = seasons,
                episodes = orderedEpisodes,
                skippedEntries = counters.skipped,
                seasonMismatchCount = counters.seasonMismatches,
            ),
        )
    }

    private fun parseSummaryOrSkip(
        json: JsonReader,
        artworkPolicy: CatalogArtworkPolicy,
    ): SeriesSummary {
        if (json.peek() == JsonToken.NULL) {
            json.nextNull()
            return SeriesSummary.EMPTY
        }
        if (json.peek() != JsonToken.BEGIN_OBJECT) {
            json.skipValue()
            return SeriesSummary.EMPTY
        }
        var name: String? = null
        var plot: String? = null
        var genre: String? = null
        var releaseDate: String? = null
        var rating: String? = null
        var cast: String? = null
        var director: String? = null
        var coverUrl: String? = null
        var backdropUrl: String? = null
        json.beginObject()
        while (json.hasNext()) {
            when (json.nextName()) {
                "name" -> name = boundedText(readDisplayScalar(json), NAME_LIMIT)
                "plot", "description" -> plot = boundedText(readDisplayScalar(json), PLOT_LIMIT)
                "genre" -> genre = boundedText(readDisplayScalar(json), LONG_TEXT_LIMIT)
                "releaseDate", "release_date", "releasedate", "year" -> {
                    val value = boundedText(readDisplayScalar(json), SHORT_TEXT_LIMIT)
                    if (releaseDate == null) releaseDate = value
                }
                "rating", "rating_5based" -> {
                    val value = boundedText(readDisplayScalar(json), SHORT_TEXT_LIMIT)
                    if (rating == null) rating = value
                }
                "cast" -> cast = boundedText(readDisplayScalar(json), LONG_TEXT_LIMIT)
                "director" -> director = boundedText(readDisplayScalar(json), LONG_TEXT_LIMIT)
                "cover", "cover_big", "movie_image" -> {
                    val value = readArtwork(json, artworkPolicy)
                    if (coverUrl == null) coverUrl = value
                }
                "backdrop_path", "backdrop" -> {
                    val value = readArtwork(json, artworkPolicy)
                    if (backdropUrl == null) backdropUrl = value
                }
                else -> json.skipValue()
            }
        }
        json.endObject()
        return SeriesSummary(
            name = name?.takeIf(String::isNotBlank),
            plot = plot?.takeIf(String::isNotBlank),
            genre = genre?.takeIf(String::isNotBlank),
            releaseDate = releaseDate?.takeIf(String::isNotBlank),
            rating = rating?.takeIf(String::isNotBlank),
            cast = cast?.takeIf(String::isNotBlank),
            director = director?.takeIf(String::isNotBlank),
            coverUrl = coverUrl,
            backdropUrl = backdropUrl,
        )
    }

    private fun parseSeasonsOrSkip(
        json: JsonReader,
        artworkPolicy: CatalogArtworkPolicy,
        advisories: MutableMap<Int, SeasonAdvisory>,
        counters: ParseCounters,
    ) {
        if (json.peek() == JsonToken.NULL) {
            json.nextNull()
            return
        }
        if (json.peek() != JsonToken.BEGIN_ARRAY) {
            json.skipValue()
            return
        }
        var order = 0
        json.beginArray()
        while (json.hasNext()) {
            counters.seasonEntries++
            if (counters.seasonEntries > seasonGroupLimit) throw SeriesEntryLimitException()
            if (json.peek() != JsonToken.BEGIN_OBJECT) {
                json.skipValue()
                counters.skipped++
                order++
                continue
            }
            parseSeason(json, artworkPolicy, order)?.let { advisory ->
                advisories.putIfAbsent(advisory.number, advisory)
            } ?: counters.skipped++
            order++
        }
        json.endArray()
    }

    private fun parseSeason(
        json: JsonReader,
        artworkPolicy: CatalogArtworkPolicy,
        order: Int,
    ): SeasonAdvisory? {
        var number: Int? = null
        var invalidNumber = false
        var name: String? = null
        var overview: String? = null
        var airDate: String? = null
        var coverUrl: String? = null
        json.beginObject()
        while (json.hasNext()) {
            when (json.nextName()) {
                "season_number" -> {
                    val value = readBoundedInteger(json, SEASON_NUMBER_MAX)
                    if (number == null) number = value.value
                    invalidNumber = invalidNumber || value.invalid
                }
                "name" -> name = boundedText(readDisplayScalar(json), NAME_LIMIT)
                "overview" -> overview = boundedText(readDisplayScalar(json), PLOT_LIMIT)
                "air_date", "airdate" -> airDate = boundedText(readDisplayScalar(json), SHORT_TEXT_LIMIT)
                "cover", "cover_big" -> {
                    val value = readArtwork(json, artworkPolicy)
                    if (coverUrl == null) coverUrl = value
                }
                else -> json.skipValue()
            }
        }
        json.endObject()
        val usableNumber = number ?: return null
        if (invalidNumber) return null
        return SeasonAdvisory(
            number = usableNumber,
            name = name?.takeIf(String::isNotBlank),
            overview = overview?.takeIf(String::isNotBlank),
            airDate = airDate?.takeIf(String::isNotBlank),
            coverUrl = coverUrl,
            providerOrder = order,
        )
    }

    private fun parseEpisodeGroups(
        json: JsonReader,
        accountId: String,
        seriesId: String,
        episodes: MutableList<SeriesEpisode>,
        seenEpisodeIds: MutableSet<String>,
        counters: ParseCounters,
    ): Boolean {
        if (json.peek() != JsonToken.BEGIN_OBJECT) {
            json.skipValue()
            return false
        }
        json.beginObject()
        while (json.hasNext()) {
            counters.seasonGroups++
            if (counters.seasonGroups > seasonGroupLimit) throw SeriesEntryLimitException()
            val seasonNumber = parseBoundedIntegerText(json.nextName(), SEASON_NUMBER_MAX)
            parseEpisodeGroup(
                json,
                accountId,
                seriesId,
                seasonNumber,
                episodes,
                seenEpisodeIds,
                counters,
            )
        }
        json.endObject()
        return true
    }

    private fun parseEpisodeGroup(
        json: JsonReader,
        accountId: String,
        seriesId: String,
        seasonNumber: Int?,
        episodes: MutableList<SeriesEpisode>,
        seenEpisodeIds: MutableSet<String>,
        counters: ParseCounters,
    ) {
        if (json.peek() != JsonToken.BEGIN_ARRAY) {
            json.skipValue()
            counters.skipped++
            return
        }
        var order = 0
        json.beginArray()
        while (json.hasNext()) {
            counters.episodeEntries++
            if (counters.episodeEntries > episodeLimit) throw SeriesEntryLimitException()
            if (seasonNumber == null || json.peek() != JsonToken.BEGIN_OBJECT) {
                json.skipValue()
                counters.skipped++
                order++
                continue
            }
            val episode = parseEpisode(json, accountId, seriesId, seasonNumber, order, counters)
            if (episode == null || !seenEpisodeIds.add(episode.providerEpisodeId)) {
                counters.skipped++
            } else {
                episodes += episode
            }
            order++
        }
        json.endArray()
    }

    private fun parseEpisode(
        json: JsonReader,
        accountId: String,
        seriesId: String,
        enclosingSeason: Int,
        order: Int,
        counters: ParseCounters,
    ): SeriesEpisode? {
        var id: String? = null
        var episodeNumber: Int? = null
        var invalidEpisodeNumber = false
        var explicitSeason: Int? = null
        var invalidExplicitSeason = false
        var title: String? = null
        var extension: String? = null
        var plot: String? = null
        var duration: String? = null
        var releaseDate: String? = null
        var rating: String? = null
        json.beginObject()
        while (json.hasNext()) {
            when (json.nextName()) {
                "id", "episode_id" -> id = boundedProviderId(readIdentifier(json))
                "episode_num", "episode_number" -> {
                    val value = readBoundedInteger(json, EPISODE_NUMBER_MAX)
                    if (episodeNumber == null) episodeNumber = value.value
                    invalidEpisodeNumber = invalidEpisodeNumber || value.invalid
                }
                "season", "season_number" -> {
                    val value = readBoundedInteger(json, SEASON_NUMBER_MAX)
                    if (explicitSeason == null) explicitSeason = value.value
                    invalidExplicitSeason = invalidExplicitSeason || value.invalid
                }
                "title", "name" -> title = boundedText(readDisplayScalar(json), NAME_LIMIT)
                "container_extension" -> extension = safeExtension(readDisplayScalar(json))
                "plot", "description" -> plot = boundedText(readDisplayScalar(json), PLOT_LIMIT)
                "duration", "duration_secs" -> duration = boundedText(readDisplayScalar(json), SHORT_TEXT_LIMIT)
                "releaseDate", "release_date", "air_date" -> {
                    releaseDate = boundedText(readDisplayScalar(json), SHORT_TEXT_LIMIT)
                }
                "rating" -> rating = boundedText(readDisplayScalar(json), SHORT_TEXT_LIMIT)
                "info" -> {
                    val metadata = parseEpisodeInfoOrSkip(json)
                    if (plot == null) plot = metadata.plot
                    if (duration == null) duration = metadata.duration
                    if (releaseDate == null) releaseDate = metadata.releaseDate
                    if (rating == null) rating = metadata.rating
                }
                else -> json.skipValue()
            }
        }
        json.endObject()
        val usableId = id ?: return null
        if (invalidEpisodeNumber || invalidExplicitSeason) return null
        if (explicitSeason != null && explicitSeason != enclosingSeason) counters.seasonMismatches++
        return SeriesEpisode(
            accountId = accountId,
            providerSeriesId = seriesId,
            providerEpisodeId = usableId,
            seasonNumber = enclosingSeason,
            episodeNumber = episodeNumber,
            title = title?.takeIf(String::isNotBlank),
            containerExtension = extension,
            providerOrder = order,
            plot = plot?.takeIf(String::isNotBlank),
            duration = duration?.takeIf(String::isNotBlank),
            releaseDate = releaseDate?.takeIf(String::isNotBlank),
            rating = rating?.takeIf(String::isNotBlank),
        )
    }

    private fun parseEpisodeInfoOrSkip(json: JsonReader): EpisodeMetadata {
        if (json.peek() != JsonToken.BEGIN_OBJECT) {
            json.skipValue()
            return EpisodeMetadata.EMPTY
        }
        var plot: String? = null
        var duration: String? = null
        var releaseDate: String? = null
        var rating: String? = null
        json.beginObject()
        while (json.hasNext()) {
            when (json.nextName()) {
                "plot", "description" -> plot = boundedText(readDisplayScalar(json), PLOT_LIMIT)
                "duration", "duration_secs" -> duration = boundedText(readDisplayScalar(json), SHORT_TEXT_LIMIT)
                "releaseDate", "release_date", "air_date" -> {
                    releaseDate = boundedText(readDisplayScalar(json), SHORT_TEXT_LIMIT)
                }
                "rating" -> rating = boundedText(readDisplayScalar(json), SHORT_TEXT_LIMIT)
                else -> json.skipValue()
            }
        }
        json.endObject()
        return EpisodeMetadata(plot, duration, releaseDate, rating)
    }

    private fun readArtwork(json: JsonReader, policy: CatalogArtworkPolicy): String? {
        if (json.peek() == JsonToken.BEGIN_ARRAY) {
            var accepted: String? = null
            json.beginArray()
            while (json.hasNext()) {
                val candidate = if (json.peek() == JsonToken.STRING) json.nextString() else {
                    json.skipValue()
                    null
                }
                if (accepted == null) accepted = policy.accept(candidate)
            }
            json.endArray()
            return accepted
        }
        val candidate = if (json.peek() == JsonToken.STRING) json.nextString() else {
            if (json.peek() == JsonToken.NULL) json.nextNull() else json.skipValue()
            null
        }
        return policy.accept(candidate)
    }

    private fun readIdentifier(json: JsonReader): String? = when (json.peek()) {
        JsonToken.STRING -> json.nextString()
        JsonToken.NUMBER -> normalizeIntegralNumber(json.nextString())
        JsonToken.NULL -> {
            json.nextNull()
            null
        }
        else -> {
            json.skipValue()
            null
        }
    }

    private fun readDisplayScalar(json: JsonReader): String? = when (json.peek()) {
        JsonToken.STRING, JsonToken.NUMBER -> json.nextString()
        JsonToken.NULL -> {
            json.nextNull()
            null
        }
        else -> {
            json.skipValue()
            null
        }
    }

    private fun readBoundedInteger(json: JsonReader, max: Int): IntegerRead {
        return when (json.peek()) {
            JsonToken.NULL -> {
                json.nextNull()
                IntegerRead(null, invalid = false)
            }
            JsonToken.STRING -> {
                val raw = json.nextString()
                if (raw.isEmpty()) IntegerRead(null, invalid = false)
                else {
                    val parsed = parseBoundedIntegerText(raw, max)
                    IntegerRead(parsed, invalid = parsed == null)
                }
            }
            JsonToken.NUMBER -> {
                val normalized = normalizeIntegralNumber(json.nextString())
                val parsed = normalized?.toIntOrNull()?.takeIf { it in 0..max }
                IntegerRead(parsed, invalid = parsed == null)
            }
            else -> {
                json.skipValue()
                IntegerRead(null, invalid = true)
            }
        }
    }

    private fun normalizeIntegralNumber(raw: String): String? = runCatching {
        BigDecimal(raw).toBigIntegerExact().longValueExact().toString()
    }.getOrNull()

    private fun parseBoundedIntegerText(raw: String, max: Int): Int? =
        raw.toIntOrNull()?.takeIf { it in 0..max }

    private fun boundedProviderId(value: String?): String? {
        val candidate = value?.takeIf(String::isNotBlank) ?: return null
        return candidate.takeIf { it.codePointCount(0, it.length) <= PROVIDER_ID_LIMIT }
    }

    private fun boundedText(value: String?, maxCodePoints: Int): String? {
        val candidate = value ?: return null
        val count = candidate.codePointCount(0, candidate.length)
        if (count <= maxCodePoints) return candidate
        return candidate.substring(0, candidate.offsetByCodePoints(0, maxCodePoints))
    }

    private fun safeExtension(value: String?): String? {
        val normalized = value?.lowercase() ?: return null
        return normalized.takeIf {
            it.length in 1..12 && it.all { character -> character.isLowerCaseOrDigitAscii() }
        }
    }

    private fun Char.isLowerCaseOrDigitAscii(): Boolean = this in 'a'..'z' || this in '0'..'9'

    private fun malformed() = SeriesDetailsResult.Failure(SeriesFailure.MalformedResponse)

    private data class ParseCounters(
        var seasonEntries: Int = 0,
        var seasonGroups: Int = 0,
        var episodeEntries: Int = 0,
        var skipped: Int = 0,
        var seasonMismatches: Int = 0,
    )

    private data class IntegerRead(val value: Int?, val invalid: Boolean)

    private data class SeasonAdvisory(
        val number: Int,
        val name: String?,
        val overview: String?,
        val airDate: String?,
        val coverUrl: String?,
        val providerOrder: Int,
    )

    private data class EpisodeMetadata(
        val plot: String?,
        val duration: String?,
        val releaseDate: String?,
        val rating: String?,
    ) {
        companion object {
            val EMPTY = EpisodeMetadata(null, null, null, null)
        }
    }

    private companion object {
        const val SEASON_GROUP_LIMIT = 1_000
        const val EPISODE_LIMIT = 10_000
        const val PROVIDER_ID_LIMIT = 256
        const val SEASON_NUMBER_MAX = 10_000
        const val EPISODE_NUMBER_MAX = 100_000
        const val NAME_LIMIT = 512
        const val PLOT_LIMIT = 4_096
        const val LONG_TEXT_LIMIT = 1_024
        const val SHORT_TEXT_LIMIT = 128
    }
}

private class SeriesEntryLimitException : IOException()
