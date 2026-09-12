package dev.tyfino.foundation.xtream

/** Safe, account-owned metadata; never contains authenticated URLs or provider payloads. */
internal data class LiveEpgProgram(
    val accountId: String,
    val channelId: String,
    val title: String?,
    val description: String?,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
)

internal enum class LiveEpgFailure {
    NetworkUnavailable,
    Timeout,
    ProviderUnavailable,
    AuthenticationRejected,
    MalformedResponse,
    UnsupportedResponse,
    ResponseTooLarge,
}

internal sealed interface LiveEpgResult {
    data class Success(val programs: List<LiveEpgProgram>, val skippedEntries: Int) : LiveEpgResult
    data class Failure(val reason: LiveEpgFailure) : LiveEpgResult
}
