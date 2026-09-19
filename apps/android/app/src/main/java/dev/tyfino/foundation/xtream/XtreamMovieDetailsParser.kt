package dev.tyfino.foundation.xtream

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.IOException
import java.io.Reader

internal class XtreamMovieDetailsParser {
    fun parse(
        reader: Reader,
        accountId: String,
        movieId: String,
        artworkPolicy: CatalogArtworkPolicy,
    ): MovieDetailsResult {
        if (bounded(accountId, ID_LIMIT) == null || bounded(movieId, ID_LIMIT) == null) {
            return failure(MovieDetailsFailure.UnsupportedResponse)
        }
        return try {
            JsonReader(reader).use { json ->
                if (json.peek() != JsonToken.BEGIN_OBJECT) return failure(MovieDetailsFailure.MalformedResponse)
                var info: MutableMap<String, String?>? = null
                var providerMovieId: String? = null
                json.beginObject()
                while (json.hasNext()) {
                    when (json.nextName()) {
                        "info" -> {
                            if (info != null || json.peek() != JsonToken.BEGIN_OBJECT) {
                                json.skipValue()
                            } else {
                                info = readInfo(json)
                            }
                        }
                        "movie_data" -> {
                            if (json.peek() != JsonToken.BEGIN_OBJECT) json.skipValue() else {
                                json.beginObject()
                                while (json.hasNext()) {
                                    if (json.nextName() == "stream_id") providerMovieId = scalar(json, ID_LIMIT)
                                    else json.skipValue()
                                }
                                json.endObject()
                            }
                        }
                        else -> json.skipValue()
                    }
                }
                json.endObject()
                if (json.peek() != JsonToken.END_DOCUMENT) return failure(MovieDetailsFailure.MalformedResponse)
                if (providerMovieId != null && providerMovieId != movieId) {
                    return failure(MovieDetailsFailure.UnsupportedResponse)
                }
                val values = info.orEmpty()
                MovieDetailsResult.Success(
                    MovieDetails(
                        accountId = accountId,
                        providerMovieId = movieId,
                        name = values["name"],
                        plot = values["plot"],
                        genre = values["genre"],
                        releaseDate = values["releasedate"] ?: values["releaseDate"] ?: values["year"],
                        rating = values["rating"],
                        duration = values["duration"],
                        cast = values["cast"],
                        director = values["director"],
                        posterUrl = artworkPolicy.accept(values["movie_image"]),
                        backdropUrl = artworkPolicy.accept(values["backdrop_path"]),
                    ),
                )
            }
        } catch (_: IOException) {
            failure(MovieDetailsFailure.MalformedResponse)
        } catch (_: IllegalStateException) {
            failure(MovieDetailsFailure.MalformedResponse)
        }
    }

    private fun readInfo(json: JsonReader): MutableMap<String, String?> {
        val accepted = ACCEPTED_FIELDS
        val result = mutableMapOf<String, String?>()
        json.beginObject()
        while (json.hasNext()) {
            val name = json.nextName()
            if (name in accepted && name !in result) {
                result[name] = if (name == "backdrop_path") firstScalar(json, accepted.getValue(name))
                else scalar(json, accepted.getValue(name))
            }
            else json.skipValue()
        }
        json.endObject()
        return result
    }

    private fun scalar(json: JsonReader, limit: Int): String? = when (json.peek()) {
        JsonToken.STRING, JsonToken.NUMBER -> bounded(json.nextString(), limit)
        JsonToken.NULL -> { json.nextNull(); null }
        else -> { json.skipValue(); null }
    }

    private fun firstScalar(json: JsonReader, limit: Int): String? {
        if (json.peek() != JsonToken.BEGIN_ARRAY) return scalar(json, limit)
        var result: String? = null
        json.beginArray()
        while (json.hasNext()) {
            if (result == null) result = scalar(json, limit) else json.skipValue()
        }
        json.endArray()
        return result
    }

    private fun bounded(value: String?, limit: Int): String? {
        val candidate = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val count = candidate.codePointCount(0, candidate.length)
        return if (count <= limit) candidate else candidate.substring(0, candidate.offsetByCodePoints(0, limit))
    }

    private fun failure(reason: MovieDetailsFailure) = MovieDetailsResult.Failure(reason)

    private companion object {
        const val ID_LIMIT = 256
        const val NAME_LIMIT = 512
        const val TEXT_LIMIT = 8_192
        const val SHORT_LIMIT = 512
        const val URL_LIMIT = 2_048
        val ACCEPTED_FIELDS = mapOf(
            "name" to NAME_LIMIT, "plot" to TEXT_LIMIT, "genre" to SHORT_LIMIT,
            "releasedate" to SHORT_LIMIT, "releaseDate" to SHORT_LIMIT, "year" to SHORT_LIMIT,
            "rating" to SHORT_LIMIT, "duration" to SHORT_LIMIT, "cast" to TEXT_LIMIT,
            "director" to TEXT_LIMIT, "movie_image" to URL_LIMIT, "backdrop_path" to URL_LIMIT,
        )
    }
}
