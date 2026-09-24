package dev.tyfino.foundation.xtream

import android.content.Context
import dev.tyfino.foundation.BuildConfig

internal enum class ProviderUserAgentPreset(val storageValue: String) {
    Tyfino("tyfino"),
    Vlc("vlc");

    fun header(): String = when (this) {
        Tyfino -> "TYFINO/${BuildConfig.VERSION_NAME} (Android)"
        Vlc -> "VLC/3.0.0"
    }

    companion object {
        fun fromStorage(value: String?): ProviderUserAgentPreset =
            entries.firstOrNull { it.storageValue == value } ?: Tyfino
    }
}

/** A local, non-secret transport preference. Only known, bounded header values can be sent. */
internal class ProviderUserAgent(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun selected(): ProviderUserAgentPreset =
        ProviderUserAgentPreset.fromStorage(preferences.getString(KEY_PRESET, null))

    fun select(preset: ProviderUserAgentPreset) {
        preferences.edit().putString(KEY_PRESET, preset.storageValue).apply()
    }

    fun header(): String = selected().header()

    private companion object {
        const val PREFERENCES = "provider_transport"
        const val KEY_PRESET = "user_agent_preset"
    }
}
