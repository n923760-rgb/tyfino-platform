package dev.tyfino.foundation.xtream

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XtreamAccountPortfolioCodecTest {
    @Test
    fun `versioned portfolio round trips without changing credentials or active account`() {
        val active = account(1, cleartext = true)
        val inactive = account(2)
        val portfolio = requireNotNull(
            XtreamAccountPortfolio.create(listOf(active, inactive), active.accountId),
        )

        val restored = XtreamAccountPortfolioCodec.decode(XtreamAccountPortfolioCodec.encode(portfolio))

        assertEquals(portfolio, restored)
    }

    @Test
    fun `legacy account migrates without changing identity generation or credentials`() {
        val original = account(3, generation = 41, password = "migration-secret", cleartext = true)
        val legacy = accountJson(original).toString()

        val migrated = XtreamAccountPortfolioCodec.decodeLegacy(legacy)

        assertEquals(listOf(original), migrated?.accounts)
        assertEquals(original.accountId, migrated?.activeAccountId)
    }

    @Test
    fun `unknown schema and unknown active account fail closed`() {
        val account = account(1)
        val unknownSchema = portfolioJson(listOf(account), account.accountId).put("schemaVersion", 99)
        val unknownActive = portfolioJson(listOf(account), accountId(9))

        assertNull(XtreamAccountPortfolioCodec.decode(unknownSchema.toString()))
        assertNull(XtreamAccountPortfolioCodec.decode(unknownActive.toString()))
    }

    @Test
    fun `duplicate identities and duplicate canonical logins fail closed`() {
        val first = account(1)
        val duplicateIdentity = account(2).copy(accountId = first.accountId)
        val duplicateLogin = account(2).copy(endpoint = first.endpoint, username = first.username)

        assertNull(
            XtreamAccountPortfolioCodec.decode(
                portfolioJson(listOf(first, duplicateIdentity), first.accountId).toString(),
            ),
        )
        assertNull(
            XtreamAccountPortfolioCodec.decode(
                portfolioJson(listOf(first, duplicateLogin), first.accountId).toString(),
            ),
        )
    }

    @Test
    fun `over-limit payload fails before account decoding`() {
        val records = JSONArray()
        repeat(XtreamAccountPortfolio.MAX_ACCOUNTS + 1) { records.put(JSONObject()) }
        val payload = JSONObject()
            .put("schemaVersion", XtreamAccountPortfolioCodec.SCHEMA_VERSION)
            .put("activeAccountId", JSONObject.NULL)
            .put("accounts", records)

        assertNull(XtreamAccountPortfolioCodec.decode(payload.toString()))
    }

    @Test
    fun `malformed and invalid transport payloads fail closed`() {
        val invalidHttp = account(1).copy(
            endpoint = ProviderEndpoint("http://provider-1.example", true),
            cleartextConsent = false,
        )

        assertNull(XtreamAccountPortfolioCodec.decode("not-json"))
        assertNull(
            XtreamAccountPortfolioCodec.decode(
                portfolioJson(listOf(invalidHttp), invalidHttp.accountId).toString(),
            ),
        )
        assertNull(XtreamAccountPortfolioCodec.decodeLegacy(JSONObject().toString()))
    }

    private fun portfolioJson(
        accounts: List<SavedXtreamAccount>,
        activeAccountId: String?,
    ): JSONObject {
        val records = JSONArray()
        accounts.forEach { records.put(accountJson(it)) }
        return JSONObject()
            .put("schemaVersion", XtreamAccountPortfolioCodec.SCHEMA_VERSION)
            .put("activeAccountId", activeAccountId ?: JSONObject.NULL)
            .put("accounts", records)
    }

    private fun accountJson(account: SavedXtreamAccount) = JSONObject()
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
        cleartext: Boolean = false,
    ) = SavedXtreamAccount(
        accountId = accountId(number),
        generation = generation,
        endpoint = ProviderEndpoint(
            baseUrl = "${if (cleartext) "http" else "https"}://provider-$number.example",
            isCleartext = cleartext,
        ),
        username = "user-$number",
        password = password,
        cleartextConsent = cleartext,
    )

    private fun accountId(number: Int): String = number.toString().padStart(32, '0')
}
