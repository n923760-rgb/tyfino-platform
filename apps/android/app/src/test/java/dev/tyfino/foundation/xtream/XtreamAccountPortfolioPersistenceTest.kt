package dev.tyfino.foundation.xtream

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class XtreamAccountPortfolioPersistenceTest {
    @Test
    fun `failed migration write keeps valid legacy representation`() {
        val account = account(1)
        val payloads = FakePayloads().apply {
            values[XtreamAccountPortfolioPersistence.LEGACY_PAYLOAD] = legacyJson(account)
            failNextWrite = true
        }

        val loaded = XtreamAccountPortfolioPersistence(payloads).load()

        assertEquals(account, loaded.activeAccount)
        assertTrue(payloads.values.containsKey(XtreamAccountPortfolioPersistence.LEGACY_PAYLOAD))
        assertFalse(payloads.values.containsKey(XtreamAccountPortfolioPersistence.PORTFOLIO_PAYLOAD))
    }

    @Test
    fun `failed replacement restores previous portfolio`() {
        val payloads = FakePayloads()
        val persistence = XtreamAccountPortfolioPersistence(payloads)
        val original = requireNotNull(XtreamAccountPortfolio.single(account(1)))
        val replacement = requireNotNull(XtreamAccountPortfolio.single(account(2)))
        persistence.save(original)
        payloads.failNextWrite = true

        assertThrows(IllegalStateException::class.java) {
            persistence.save(replacement)
        }

        assertEquals(original, persistence.load())
    }

    private class FakePayloads : XtreamEncryptedPayloads {
        val values = mutableMapOf<String, String>()
        var failNextWrite = false

        override fun read(name: String): String? = values[name]

        override fun write(name: String, plaintext: String) {
            if (failNextWrite) {
                failNextWrite = false
                throw IllegalStateException("simulated persistence failure")
            }
            values[name] = plaintext
        }

        override fun remove(name: String) {
            values.remove(name)
        }
    }

    private fun legacyJson(account: SavedXtreamAccount): String = JSONObject()
        .put("accountId", account.accountId)
        .put("generation", account.generation)
        .put("baseUrl", account.endpoint.baseUrl)
        .put("isCleartext", account.endpoint.isCleartext)
        .put("username", account.username)
        .put("password", account.password)
        .put("cleartextConsent", account.cleartextConsent)
        .toString()

    private fun account(number: Int) = SavedXtreamAccount(
        accountId = number.toString().padStart(32, '0'),
        generation = number.toLong(),
        endpoint = ProviderEndpoint("https://provider-$number.example", false),
        username = "user-$number",
        password = "password-$number",
        cleartextConsent = false,
    )
}
