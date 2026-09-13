package dev.tyfino.foundation.xtream

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureXtreamAccountStoreTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun portfolioIsEncryptedAndRestoresEveryAccountAndActiveSelection() {
        val store = SecureXtreamAccountStore(context)
        val active = account(1)
        val inactive = account(2)
        val portfolio = requireNotNull(
            XtreamAccountPortfolio.create(listOf(active, inactive), active.accountId),
        )
        store.clearPortfolio()
        try {
            store.savePortfolio(portfolio)

            assertEquals(portfolio, SecureXtreamAccountStore(context).loadPortfolio())
            assertEquals(active, SecureXtreamAccountStore(context).load())
        } finally {
            store.clearPortfolio()
        }
    }

    @Test
    fun legacyPayloadMigratesOnlyAfterVerifiedPortfolioWrite() {
        val payloads = SecureXtreamPayloads(context)
        val store = SecureXtreamAccountStore(context)
        val legacy = account(3, generation = 17, password = "legacy-password")
        store.clearPortfolio()
        try {
            payloads.write(
                XtreamAccountPortfolioPersistence.LEGACY_PAYLOAD,
                legacyJson(legacy).toString(),
            )

            val migrated = store.loadPortfolio()

            assertEquals(listOf(legacy), migrated.accounts)
            assertEquals(legacy.accountId, migrated.activeAccountId)
            assertNull(payloads.read(XtreamAccountPortfolioPersistence.LEGACY_PAYLOAD))
            assertEquals(
                migrated,
                XtreamAccountPortfolioCodec.decode(
                    requireNotNull(payloads.read(XtreamAccountPortfolioPersistence.PORTFOLIO_PAYLOAD)),
                ),
            )
        } finally {
            store.clearPortfolio()
        }
    }

    @Test
    fun invalidAuthenticatedPortfolioFailsClosedWithoutLegacyFallback() {
        val payloads = SecureXtreamPayloads(context)
        val store = SecureXtreamAccountStore(context)
        store.clearPortfolio()
        try {
            payloads.write(XtreamAccountPortfolioPersistence.PORTFOLIO_PAYLOAD, "invalid-json")
            payloads.write(
                XtreamAccountPortfolioPersistence.LEGACY_PAYLOAD,
                legacyJson(account(4)).toString(),
            )

            assertTrue(store.loadPortfolio().accounts.isEmpty())
            assertNull(payloads.read(XtreamAccountPortfolioPersistence.PORTFOLIO_PAYLOAD))
            assertNull(payloads.read(XtreamAccountPortfolioPersistence.LEGACY_PAYLOAD))
        } finally {
            store.clearPortfolio()
        }
    }

    private fun legacyJson(account: SavedXtreamAccount) = JSONObject()
        .put("accountId", account.accountId)
        .put("generation", account.generation)
        .put("baseUrl", account.endpoint.baseUrl)
        .put("isCleartext", account.endpoint.isCleartext)
        .put("username", account.username)
        .put("password", account.password)
        .put("cleartextConsent", account.cleartextConsent)

    private fun account(
        number: Int,
        generation: Long = 1,
        password: String = "password-$number",
    ) = SavedXtreamAccount(
        accountId = number.toString().padStart(32, '0'),
        generation = generation,
        endpoint = ProviderEndpoint("https://provider-$number.example", false),
        username = "user-$number",
        password = password,
        cleartextConsent = false,
    )
}
