package dev.tyfino.foundation.xtream

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

internal interface XtreamAccountStore {
    fun load(): SavedXtreamAccount?
    fun save(account: SavedXtreamAccount)
    fun clear()
}

internal class SecureXtreamAccountStore(context: Context) : XtreamAccountStore {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    override fun load(): SavedXtreamAccount? = synchronized(this) {
        val payload = readPayload() ?: return@synchronized null
        runCatching {
            SavedXtreamAccount(
                accountId = payload.getString(ACCOUNT_ID),
                generation = payload.getLong(GENERATION),
                endpoint = ProviderEndpoint(
                    baseUrl = payload.getString(BASE_URL),
                    isCleartext = payload.getBoolean(IS_CLEARTEXT),
                ),
                username = payload.getString(USERNAME),
                password = payload.getString(PASSWORD),
                cleartextConsent = payload.getBoolean(CLEARTEXT_CONSENT),
            ).also { account ->
                require(account.accountId.length in 32..128)
                require(account.generation >= 0)
                require(account.username.isNotEmpty())
                require(account.password.isNotEmpty())
                val parsed = XtreamHostCanonicalizer.parse(account.endpoint.baseUrl)
                require(parsed is HostParseResult.Valid && parsed.endpoint == account.endpoint)
                require(!account.endpoint.isCleartext || account.cleartextConsent)
            }
        }.getOrElse {
            clear()
            null
        }
    }

    override fun save(account: SavedXtreamAccount) = synchronized(this) {
        val payload = JSONObject()
            .put(ACCOUNT_ID, account.accountId)
            .put(GENERATION, account.generation)
            .put(BASE_URL, account.endpoint.baseUrl)
            .put(IS_CLEARTEXT, account.endpoint.isCleartext)
            .put(USERNAME, account.username)
            .put(PASSWORD, account.password)
            .put(CLEARTEXT_CONSENT, account.cleartextConsent)
        writePayload(payload)
    }

    override fun clear() = synchronized(this) {
        preferences.edit { remove(PAYLOAD) }
    }

    private fun readPayload(): JSONObject? {
        val encoded = preferences.getString(PAYLOAD, null) ?: return null
        return runCatching {
            val parts = encoded.split('.', limit = 2)
            require(parts.size == 2)
            val flags = android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE
            val iv = android.util.Base64.decode(parts[0], flags)
            val encrypted = android.util.Base64.decode(parts[1], flags)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            JSONObject(String(cipher.doFinal(encrypted), Charsets.UTF_8))
        }.getOrElse {
            clear()
            null
        }
    }

    private fun writePayload(payload: JSONObject) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val flags = android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE
        val iv = android.util.Base64.encodeToString(cipher.iv, flags)
        val encrypted = android.util.Base64.encodeToString(
            cipher.doFinal(payload.toString().toByteArray(Charsets.UTF_8)),
            flags,
        )
        preferences.edit { putString(PAYLOAD, "$iv.$encrypted") }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val PREFERENCES = "tyfino_xtream_account_protected"
        const val PAYLOAD = "payload"
        const val KEY_ALIAS = "tyfino_xtream_account_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val ACCOUNT_ID = "accountId"
        const val GENERATION = "generation"
        const val BASE_URL = "baseUrl"
        const val IS_CLEARTEXT = "isCleartext"
        const val USERNAME = "username"
        const val PASSWORD = "password"
        const val CLEARTEXT_CONSENT = "cleartextConsent"
    }
}
