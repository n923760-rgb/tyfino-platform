package dev.tyfino.foundation.xtream

import org.json.JSONArray
import org.json.JSONObject

internal object XtreamAccountPortfolioCodec {
    const val SCHEMA_VERSION = 2

    fun encode(portfolio: XtreamAccountPortfolio): String {
        val accounts = JSONArray()
        portfolio.accounts.forEach { account -> accounts.put(account.toJson()) }
        return JSONObject()
            .put(SCHEMA, SCHEMA_VERSION)
            .put(ACTIVE_ACCOUNT_ID, portfolio.activeAccountId ?: JSONObject.NULL)
            .put(ACCOUNTS, accounts)
            .toString()
    }

    fun decode(encoded: String): XtreamAccountPortfolio? = runCatching {
        val payload = JSONObject(encoded)
        require(payload.getInt(SCHEMA) == SCHEMA_VERSION)
        val records = payload.getJSONArray(ACCOUNTS)
        require(records.length() <= XtreamAccountPortfolio.MAX_ACCOUNTS)
        val accounts = ArrayList<SavedXtreamAccount>(records.length())
        for (index in 0 until records.length()) {
            accounts += records.getJSONObject(index).toAccount()
        }
        val activeAccountId = if (payload.isNull(ACTIVE_ACCOUNT_ID)) {
            null
        } else {
            payload.getString(ACTIVE_ACCOUNT_ID)
        }
        requireNotNull(XtreamAccountPortfolio.create(accounts, activeAccountId))
    }.getOrNull()

    fun decodeLegacy(encoded: String): XtreamAccountPortfolio? = runCatching {
        val account = JSONObject(encoded).toAccount()
        requireNotNull(XtreamAccountPortfolio.single(account))
    }.getOrNull()

    private fun SavedXtreamAccount.toJson(): JSONObject = JSONObject()
        .put(ACCOUNT_ID, accountId)
        .put(GENERATION, generation)
        .put(BASE_URL, endpoint.baseUrl)
        .put(IS_CLEARTEXT, endpoint.isCleartext)
        .put(USERNAME, username)
        .put(PASSWORD, password)
        .put(CLEARTEXT_CONSENT, cleartextConsent)

    private fun JSONObject.toAccount() = SavedXtreamAccount(
        accountId = getString(ACCOUNT_ID),
        generation = getLong(GENERATION),
        endpoint = ProviderEndpoint(
            baseUrl = getString(BASE_URL),
            isCleartext = getBoolean(IS_CLEARTEXT),
        ),
        username = getString(USERNAME),
        password = getString(PASSWORD),
        cleartextConsent = getBoolean(CLEARTEXT_CONSENT),
    )

    private const val SCHEMA = "schemaVersion"
    private const val ACTIVE_ACCOUNT_ID = "activeAccountId"
    private const val ACCOUNTS = "accounts"
    private const val ACCOUNT_ID = "accountId"
    private const val GENERATION = "generation"
    private const val BASE_URL = "baseUrl"
    private const val IS_CLEARTEXT = "isCleartext"
    private const val USERNAME = "username"
    private const val PASSWORD = "password"
    private const val CLEARTEXT_CONSENT = "cleartextConsent"
}
