package dev.tyfino.foundation.xtream

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.IOException
import java.io.Reader
import java.net.URI

internal class XtreamCatalogParser(
    private val categoryLimit: Int = CATEGORY_LIMIT,
    private val itemLimit: Int = ITEM_LIMIT,
) {
    fun parseCategories(reader: Reader): CatalogResult<CatalogCategory> = parseArray(
        reader = reader,
        limit = categoryLimit,
        parseEntry = ::parseCategory,
    )

    fun parseItems(
        reader: Reader,
        section: CatalogSection,
        categoryId: String,
        artworkPolicy: CatalogArtworkPolicy,
    ): CatalogResult<CatalogItem> {
        if (boundedProviderId(categoryId) == null) {
            return CatalogResult.Failure(CatalogFailure.UnsupportedResponse)
        }
        return parseArray(
            reader = reader,
            limit = itemLimit,
            parseEntry = { json, order ->
                parseItem(json, order, section, categoryId, artworkPolicy)
            },
        )
    }

    private fun <T> parseArray(
        reader: Reader,
        limit: Int,
        parseEntry: (JsonReader, Int) -> T?,
    ): CatalogResult<T> {
        return try {
            JsonReader(reader).use { json ->
                if (json.peek() != JsonToken.BEGIN_ARRAY) {
                    return CatalogResult.Failure(CatalogFailure.MalformedResponse)
                }
                json.beginArray()
                val records = ArrayList<T>()
                var entryCount = 0
                var skipped = 0
                while (json.hasNext()) {
                    entryCount++
                    if (entryCount > limit) {
                        return CatalogResult.Failure(CatalogFailure.ResponseTooLarge)
                    }
                    if (json.peek() != JsonToken.BEGIN_OBJECT) {
                        json.skipValue()
                        skipped++
                        continue
                    }
                    parseEntry(json, entryCount - 1)?.let(records::add) ?: skipped++
                }
                json.endArray()
                if (json.peek() != JsonToken.END_DOCUMENT) {
                    return CatalogResult.Failure(CatalogFailure.MalformedResponse)
                }
                CatalogResult.Success(records, skipped)
            }
        } catch (failure: CatalogResponseTooLargeException) {
            CatalogResult.Failure(CatalogFailure.ResponseTooLarge)
        } catch (_: IOException) {
            CatalogResult.Failure(CatalogFailure.MalformedResponse)
        } catch (_: IllegalStateException) {
            CatalogResult.Failure(CatalogFailure.MalformedResponse)
        }
    }

    private fun parseCategory(json: JsonReader, order: Int): CatalogCategory? {
        var id: String? = null
        var name: String? = null
        json.beginObject()
        while (json.hasNext()) {
            when (json.nextName()) {
                "category_id" -> id = boundedProviderId(readScalar(json))
                "category_name" -> name = boundedText(readScalar(json), NAME_LIMIT)
                else -> json.skipValue()
            }
        }
        json.endObject()
        val usableId = id ?: return null
        val usableName = name?.takeIf(String::isNotBlank) ?: return null
        return CatalogCategory(usableId, usableName, order)
    }

    private fun parseItem(
        json: JsonReader,
        order: Int,
        section: CatalogSection,
        categoryId: String,
        artworkPolicy: CatalogArtworkPolicy,
    ): CatalogItem? {
        var id: String? = null
        var name: String? = null
        var artwork: String? = null
        var rating: String? = null
        var releaseYear: String? = null
        var containerExtension: String? = null
        var addedAtEpochSeconds: Long? = null
        json.beginObject()
        while (json.hasNext()) {
            when (json.nextName()) {
                section.itemIdField -> id = boundedProviderId(readScalar(json))
                "name" -> name = boundedText(readScalar(json), NAME_LIMIT)
                section.artworkField -> artwork = readScalar(json)
                "rating" -> rating = boundedText(readScalar(json), SHORT_TEXT_LIMIT)
                "releaseDate", "release_date", "year" -> {
                    val candidate = boundedText(readScalar(json), SHORT_TEXT_LIMIT)
                    if (releaseYear == null) releaseYear = candidate
                }
                "container_extension" -> {
                    containerExtension = boundedText(readScalar(json), SHORT_TEXT_LIMIT)
                }
                "added" -> {
                    addedAtEpochSeconds = readScalar(json)?.toLongOrNull()?.takeIf {
                        it in 946684800L..4102444800L
                    }
                }
                else -> json.skipValue()
            }
        }
        json.endObject()
        val usableId = id ?: return null
        val usableName = name?.takeIf(String::isNotBlank) ?: return null
        return CatalogItem(
            providerId = usableId,
            categoryId = categoryId,
            name = usableName,
            providerOrder = order,
            artworkUrl = artworkPolicy.accept(artwork),
            rating = rating?.takeIf(String::isNotBlank),
            releaseYear = releaseYear?.takeIf(String::isNotBlank),
            containerExtension = containerExtension?.takeIf(String::isNotBlank),
            addedAtEpochSeconds = addedAtEpochSeconds,
        )
    }

    private fun readScalar(json: JsonReader): String? = when (json.peek()) {
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

    private companion object {
        const val CATEGORY_LIMIT = 10_000
        const val ITEM_LIMIT = 50_000
        const val PROVIDER_ID_LIMIT = 256
        const val NAME_LIMIT = 512
        const val SHORT_TEXT_LIMIT = 128
    }
}

internal class CatalogArtworkPolicy(private val account: SavedXtreamAccount) {
    fun accept(candidate: String?): String? {
        val value = candidate?.takeIf(String::isNotBlank) ?: return null
        if (value.length > MAX_ARTWORK_CHARACTERS) return null
        if (value.contains(account.username) || value.contains(account.password)) return null
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "https" && scheme != "http") return null
        if (scheme == "http" && !account.cleartextConsent) return null
        if (uri.rawUserInfo != null || uri.rawFragment != null || uri.host.isNullOrBlank()) return null
        val forbiddenQueryNames = uri.rawQuery.orEmpty()
            .split('&')
            .map { it.substringBefore('=').lowercase() }
        if (forbiddenQueryNames.any { it in FORBIDDEN_QUERY_NAMES }) return null
        return value
    }

    private companion object {
        const val MAX_ARTWORK_CHARACTERS = 2_048
        val FORBIDDEN_QUERY_NAMES = setOf("username", "password", "user_info", "userinfo")
    }
}
