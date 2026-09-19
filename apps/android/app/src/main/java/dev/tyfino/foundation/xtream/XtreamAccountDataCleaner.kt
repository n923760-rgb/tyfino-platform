package dev.tyfino.foundation.xtream

internal fun interface XtreamAccountPartitionCleaner {
    suspend fun clearAccount(accountId: String): Boolean
}

/** Deletes one exact account partition from every implemented local IPTV data store. */
internal class XtreamAccountDataCleaner(
    private val partitions: List<XtreamAccountPartitionCleaner>,
) {
    init {
        require(partitions.size == REQUIRED_PARTITIONS)
    }

    suspend fun clearAccount(accountId: String): Boolean {
        if (accountId.isBlank() || accountId.codePointCount(0, accountId.length) > 128) return false
        return partitions.all { partition -> partition.clearAccount(accountId) }
    }

    private companion object {
        const val REQUIRED_PARTITIONS = 9
    }
}
