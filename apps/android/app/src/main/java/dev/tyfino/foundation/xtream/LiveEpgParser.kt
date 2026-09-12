package dev.tyfino.foundation.xtream

import android.util.Base64
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.IOException
import java.io.Reader
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/** Parses one channel's short EPG. Ambiguous local-time strings are deliberately ignored. */
internal class LiveEpgParser(
    private val entryLimit: Int = 100,
    private val decodeBase64: (String) -> ByteArray = { Base64.decode(it, Base64.DEFAULT) },
) {
    fun parse(reader: Reader, accountId: String, channelId: String): LiveEpgResult {
        if (!accountId.bounded(128) || !channelId.bounded(256)) {
            return LiveEpgResult.Failure(LiveEpgFailure.UnsupportedResponse)
        }
        return try {
            JsonReader(reader).use { json -> parseDocument(json, accountId, channelId) }
        } catch (_: CatalogResponseTooLargeException) {
            LiveEpgResult.Failure(LiveEpgFailure.ResponseTooLarge)
        } catch (_: IOException) {
            LiveEpgResult.Failure(LiveEpgFailure.MalformedResponse)
        } catch (_: IllegalStateException) {
            LiveEpgResult.Failure(LiveEpgFailure.MalformedResponse)
        }
    }

    private fun parseDocument(json: JsonReader, accountId: String, channelId: String): LiveEpgResult {
        if (json.peek() != JsonToken.BEGIN_OBJECT) return malformed()
        val programs = ArrayList<LiveEpgProgram>()
        var found = false
        var skipped = 0
        json.beginObject()
        while (json.hasNext()) {
            when (json.nextName()) {
                "epg_listings" -> {
                    if (found || json.peek() != JsonToken.BEGIN_ARRAY) return malformed()
                    found = true
                    json.beginArray()
                    var count = 0
                    while (json.hasNext()) {
                        count++
                        if (count > entryLimit) return LiveEpgResult.Failure(LiveEpgFailure.ResponseTooLarge)
                        if (json.peek() != JsonToken.BEGIN_OBJECT) {
                            json.skipValue()
                            skipped++
                        } else {
                            val entry = parseEntry(json, accountId, channelId)
                            if (entry == null) skipped++ else programs += entry
                        }
                    }
                    json.endArray()
                }
                else -> json.skipValue()
            }
        }
        json.endObject()
        if (!found || json.peek() != JsonToken.END_DOCUMENT) return malformed()
        val ordered = programs.distinctBy { Triple(it.startEpochMillis, it.endEpochMillis, it.title) }
            .sortedWith(compareBy<LiveEpgProgram> { it.startEpochMillis }.thenBy { it.endEpochMillis })
        return LiveEpgResult.Success(ordered, skipped + programs.size - ordered.size)
    }

    private fun parseEntry(json: JsonReader, accountId: String, channelId: String): LiveEpgProgram? {
        var title: String? = null
        var description: String? = null
        var start: Long? = null
        var end: Long? = null
        json.beginObject()
        while (json.hasNext()) {
            when (json.nextName()) {
                "title" -> title = readText(json)
                "description" -> description = readText(json)
                "start_timestamp" -> start = readEpochSeconds(json)
                "stop_timestamp" -> end = readEpochSeconds(json)
                else -> json.skipValue()
            }
        }
        json.endObject()
        val from = start ?: return null
        val until = end ?: return null
        if (until <= from) return null
        return LiveEpgProgram(accountId, channelId, title, description, from * 1_000L, until * 1_000L)
    }

    private fun readEpochSeconds(json: JsonReader): Long? {
        val raw = when (json.peek()) {
            JsonToken.STRING, JsonToken.NUMBER -> json.nextString()
            else -> { json.skipValue(); return null }
        }
        if (raw.length !in 10..11 || !raw.all(Char::isDigit)) return null
        return raw.toLongOrNull()?.takeIf { it in MIN_EPOCH_SECONDS..MAX_EPOCH_SECONDS }
    }

    private fun readText(json: JsonReader): String? {
        val raw = when (json.peek()) {
            JsonToken.STRING -> json.nextString()
            else -> { json.skipValue(); return null }
        }
        if (raw.length > MAX_ENCODED_CHARS) return null
        val decoded = if (raw.length % 4 == 0 && raw.isNotEmpty() &&
            raw.all { it.isLetterOrDigit() && it.code < 128 || it == '+' || it == '/' || it == '=' } &&
            raw.dropLastWhile { it == '=' }.none { it == '=' }
        ) {
            try {
                val bytes = decodeBase64(raw)
                if (bytes.size > MAX_DECODED_BYTES) null else {
                    StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes)).toString()
                }
            } catch (_: IllegalArgumentException) { null }
            catch (_: CharacterCodingException) { null }
        } else null
        return (decoded?.takeIf(::printable) ?: raw.takeIf(::printable))
            ?.trim()?.takeIf { it.isNotEmpty() && it.codePointCount(0, it.length) <= MAX_TEXT_CODE_POINTS }
    }

    private fun printable(value: String): Boolean = value.none(Character::isISOControl)
    private fun String.bounded(limit: Int) = isNotBlank() && codePointCount(0, length) <= limit
    private fun malformed() = LiveEpgResult.Failure(LiveEpgFailure.MalformedResponse)

    private companion object {
        const val MIN_EPOCH_SECONDS = 946_684_800L
        const val MAX_EPOCH_SECONDS = 4_102_444_800L
        const val MAX_ENCODED_CHARS = 4_096
        const val MAX_DECODED_BYTES = 2_048
        const val MAX_TEXT_CODE_POINTS = 512
    }
}
