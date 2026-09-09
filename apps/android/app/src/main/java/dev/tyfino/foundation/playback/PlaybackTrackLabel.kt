package dev.tyfino.foundation.playback

import java.util.Locale

internal object PlaybackTrackLabel {
    private const val MAX_CODE_POINTS = 80
    private const val MAX_LANGUAGE_TAG_CODE_POINTS = 35
    private val unsafeCharacters = Regex("[\\p{Cc}\\p{Cf}]+")
    private val repeatedWhitespace = Regex("\\s+")

    fun resolve(
        language: String?,
        mediaLabel: String?,
        fallback: String,
        displayLocale: Locale,
    ): String {
        val languageName = language
            ?.let(::normalize)
            ?.takeIf { it.codePointCount() in 1..MAX_LANGUAGE_TAG_CODE_POINTS }
            ?.takeIf { it != "und" && LANGUAGE_TAG.matches(it) }
            ?.let(Locale::forLanguageTag)
            ?.takeIf { it.language.isNotBlank() }
            ?.getDisplayName(displayLocale)
            ?.let(::normalize)
            ?.takeIf(String::isNotBlank)

        return bound(
            languageName
                ?: normalize(mediaLabel.orEmpty()).takeIf(String::isNotBlank)
                ?: normalize(fallback).takeIf(String::isNotBlank)
                ?: "?",
        )
    }

    private fun normalize(value: String): String =
        value.replace(unsafeCharacters, " ")
            .trim()
            .replace(repeatedWhitespace, " ")

    private fun bound(value: String): String {
        val count = value.codePointCount()
        if (count <= MAX_CODE_POINTS) return value
        val end = value.offsetByCodePoints(0, MAX_CODE_POINTS - 1)
        return value.substring(0, end) + "…"
    }

    private fun String.codePointCount(): Int = codePointCount(0, length)

    private val LANGUAGE_TAG = Regex("[A-Za-z0-9]{1,8}(-[A-Za-z0-9]{1,8})*")
}
