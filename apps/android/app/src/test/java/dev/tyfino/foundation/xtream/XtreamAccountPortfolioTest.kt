package dev.tyfino.foundation.xtream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class XtreamAccountPortfolioTest {
    @Test
    fun `single account is active`() {
        val account = account(1)

        val portfolio = XtreamAccountPortfolio.single(account)

        assertEquals(listOf(account), portfolio?.accounts)
        assertEquals(account, portfolio?.activeAccount)
    }

    @Test
    fun `new authenticated account is added and activated`() {
        val first = account(1)
        val second = account(2)
        val initial = requireNotNull(XtreamAccountPortfolio.single(first))

        val result = initial.upsertAuthenticated(second) as XtreamPortfolioUpsert.Added

        assertEquals(listOf(second, first), result.portfolio.accounts)
        assertEquals(second, result.portfolio.activeAccount)
    }

    @Test
    fun `ninth distinct account is rejected without eviction`() {
        val accounts = (1..XtreamAccountPortfolio.MAX_ACCOUNTS).map(::account)
        val initial = requireNotNull(XtreamAccountPortfolio.create(accounts, accounts.first().accountId))

        val result = initial.upsertAuthenticated(account(9))

        assertSame(XtreamPortfolioUpsert.LimitReached, result)
        assertEquals(accounts, initial.accounts)
        assertEquals(accounts.first(), initial.activeAccount)
    }

    @Test
    fun `duplicate updates credentials while preserving identity and incrementing generation`() {
        val original = account(1, generation = 7, password = "old-password")
        val inactive = account(2)
        val initial = requireNotNull(XtreamAccountPortfolio.create(listOf(inactive, original), inactive.accountId))
        val candidate = original.copy(
            accountId = accountId(99),
            generation = 1,
            password = "new-password",
        )

        val result = initial.upsertAuthenticated(candidate) as XtreamPortfolioUpsert.Updated

        assertEquals(original.accountId, result.portfolio.activeAccount?.accountId)
        assertEquals(8L, result.portfolio.activeAccount?.generation)
        assertEquals("new-password", result.portfolio.activeAccount?.password)
        assertEquals(listOf(original.accountId, inactive.accountId), result.portfolio.accounts.map { it.accountId })
    }

    @Test
    fun `activation is explicit and moves the selected account first`() {
        val first = account(1)
        val second = account(2)
        val initial = requireNotNull(XtreamAccountPortfolio.create(listOf(first, second), first.accountId))

        val switched = requireNotNull(initial.activate(second.accountId))

        assertEquals(second, switched.activeAccount)
        assertEquals(listOf(second, first), switched.accounts)
        assertNull(initial.activate(accountId(99)))
    }

    @Test
    fun `inactive removal preserves active account`() {
        val active = account(1)
        val inactive = account(2)
        val initial = requireNotNull(XtreamAccountPortfolio.create(listOf(active, inactive), active.accountId))

        val removed = requireNotNull(initial.remove(inactive.accountId))

        assertEquals(listOf(active), removed.accounts)
        assertEquals(active, removed.activeAccount)
    }

    @Test
    fun `active removal leaves remaining accounts without automatic selection`() {
        val active = account(1)
        val remaining = account(2)
        val initial = requireNotNull(XtreamAccountPortfolio.create(listOf(active, remaining), active.accountId))

        val removed = requireNotNull(initial.remove(active.accountId))

        assertEquals(listOf(remaining), removed.accounts)
        assertNull(removed.activeAccountId)
        assertNull(removed.activeAccount)
    }

    @Test
    fun `malformed persisted portfolios fail closed`() {
        val first = account(1)
        val duplicateIdentity = account(2).copy(accountId = first.accountId)
        val duplicateLogin = account(2).copy(endpoint = first.endpoint, username = first.username)
        val overLimit = (1..9).map(::account)

        assertNull(XtreamAccountPortfolio.create(listOf(first), accountId(99)))
        assertNull(XtreamAccountPortfolio.create(listOf(first, duplicateIdentity), first.accountId))
        assertNull(XtreamAccountPortfolio.create(listOf(first, duplicateLogin), first.accountId))
        assertNull(XtreamAccountPortfolio.create(overLimit, overLimit.first().accountId))
        assertTrue(XtreamAccountPortfolio.Empty.accounts.isEmpty())
    }

    private fun account(
        number: Int,
        generation: Long = 1,
        password: String = "password-$number",
    ) = SavedXtreamAccount(
        accountId = accountId(number),
        generation = generation,
        endpoint = ProviderEndpoint("https://provider-$number.example", false),
        username = "user-$number",
        password = password,
        cleartextConsent = false,
    )

    private fun accountId(number: Int): String = number.toString().padStart(32, '0')
}
