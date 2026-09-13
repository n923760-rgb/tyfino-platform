package dev.tyfino.foundation.xtream

internal sealed interface XtreamPortfolioUpsert {
    val portfolio: XtreamAccountPortfolio?

    data class Added(override val portfolio: XtreamAccountPortfolio) : XtreamPortfolioUpsert
    data class Updated(override val portfolio: XtreamAccountPortfolio) : XtreamPortfolioUpsert
    data object LimitReached : XtreamPortfolioUpsert {
        override val portfolio: XtreamAccountPortfolio? = null
    }
    data object Invalid : XtreamPortfolioUpsert {
        override val portfolio: XtreamAccountPortfolio? = null
    }
}

/**
 * A bounded, immutable account portfolio. List order is recency order; the active account is first.
 * Credentials remain inside the encrypted portfolio and must not be copied into presentation state.
 */
internal data class XtreamAccountPortfolio private constructor(
    val accounts: List<SavedXtreamAccount>,
    val activeAccountId: String?,
) {
    val activeAccount: SavedXtreamAccount?
        get() = activeAccountId?.let { id -> accounts.firstOrNull { it.accountId == id } }

    fun activate(accountId: String): XtreamAccountPortfolio? {
        val selected = accounts.firstOrNull { it.accountId == accountId } ?: return null
        return create(listOf(selected) + accounts.filterNot { it.accountId == accountId }, accountId)
    }

    fun remove(accountId: String): XtreamAccountPortfolio? {
        if (accounts.none { it.accountId == accountId }) return null
        return create(
            accounts = accounts.filterNot { it.accountId == accountId },
            activeAccountId = activeAccountId?.takeUnless { it == accountId },
        )
    }

    fun upsertAuthenticated(candidate: SavedXtreamAccount): XtreamPortfolioUpsert {
        if (!candidate.isPortfolioAccountValid()) return XtreamPortfolioUpsert.Invalid
        val duplicate = accounts.firstOrNull {
            it.endpoint.baseUrl == candidate.endpoint.baseUrl && it.username == candidate.username
        }
        if (duplicate != null) {
            if (duplicate.generation == Long.MAX_VALUE) return XtreamPortfolioUpsert.Invalid
            val updated = candidate.copy(
                accountId = duplicate.accountId,
                generation = duplicate.generation + 1L,
            )
            val portfolio = create(
                accounts = listOf(updated) + accounts.filterNot { it.accountId == duplicate.accountId },
                activeAccountId = duplicate.accountId,
            ) ?: return XtreamPortfolioUpsert.Invalid
            return XtreamPortfolioUpsert.Updated(portfolio)
        }
        if (accounts.size == MAX_ACCOUNTS) return XtreamPortfolioUpsert.LimitReached
        if (accounts.any { it.accountId == candidate.accountId }) return XtreamPortfolioUpsert.Invalid
        val portfolio = create(listOf(candidate) + accounts, candidate.accountId)
            ?: return XtreamPortfolioUpsert.Invalid
        return XtreamPortfolioUpsert.Added(portfolio)
    }

    internal companion object {
        const val MAX_ACCOUNTS = 8

        val Empty = XtreamAccountPortfolio(emptyList(), null)

        fun single(account: SavedXtreamAccount): XtreamAccountPortfolio? =
            create(listOf(account), account.accountId)

        fun create(
            accounts: List<SavedXtreamAccount>,
            activeAccountId: String?,
        ): XtreamAccountPortfolio? {
            if (accounts.size > MAX_ACCOUNTS) return null
            if (accounts.any { !it.isPortfolioAccountValid() }) return null
            if (accounts.map { it.accountId }.distinct().size != accounts.size) return null
            if (accounts.map { it.endpoint.baseUrl to it.username }.distinct().size != accounts.size) return null
            if (activeAccountId != null && accounts.none { it.accountId == activeAccountId }) return null
            val ordered = if (activeAccountId == null) {
                accounts
            } else {
                accounts.sortedByDescending { it.accountId == activeAccountId }
            }
            return XtreamAccountPortfolio(ordered.toList(), activeAccountId)
        }
    }
}

private fun SavedXtreamAccount.isPortfolioAccountValid(): Boolean {
    if (accountId.length !in 32..128 || generation < 0) return false
    if (username.isEmpty() || password.isEmpty()) return false
    val parsed = XtreamHostCanonicalizer.parse(endpoint.baseUrl)
    if (parsed !is HostParseResult.Valid || parsed.endpoint != endpoint) return false
    return !endpoint.isCleartext || cleartextConsent
}
